package com.olena.labmonitor.device;

import com.jayway.jsonpath.JsonPath;
import com.olena.labmonitor.device.credential.*;
import com.olena.labmonitor.lab.*;
import com.olena.labmonitor.organization.*;
import com.olena.labmonitor.room.*;
import com.olena.labmonitor.sensor.*;
import com.olena.labmonitor.sensor.reading.SensorReadingRepository;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DeviceIngestionFlowTests {
    @Autowired MockMvc mockMvc;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired LabRepository labRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired SensorRepository sensorRepository;
    @Autowired DeviceRepository deviceRepository;
    @Autowired DeviceCredentialRepository credentialRepository;
    @Autowired DeviceCredentialService credentialService;
    @Autowired SensorReadingRepository readingRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager entityManager;

    @Test
    void validCredentialResolvesMultipleChannelsAndUpdatesUsageTimestamps() throws Exception {
        Fixture fixture = fixture("multi", true);
        String token = credentialService.provision(fixture.device.getId()).token();

        mockMvc.perform(deviceReading(token, "temperature", "23.7", "msg-temperature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.sensorId").value(fixture.temperature.getId()));
        mockMvc.perform(deviceReading(token, "humidity", "48.2", "msg-humidity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.sensorId").value(fixture.humidity.getId()));

        entityManager.flush();
        entityManager.clear();
        Device reloadedDevice = deviceRepository.findById(fixture.device.getId()).orElseThrow();
        DeviceCredential credential = credentialRepository.findByDeviceIdAndStatus(
                fixture.device.getId(), DeviceCredentialStatus.ACTIVE).getFirst();
        assertThat(reloadedDevice.getLastSeenAt()).isNotNull();
        assertThat(credential.getLastUsedAt()).isNotNull();
        assertThat(readingRepository.count()).isEqualTo(2);
    }

    @Test
    void duplicateMessageReturnsSuccessWithoutCreatingAnotherReading() throws Exception {
        Fixture fixture = fixture("duplicate", false);
        String token = credentialService.provision(fixture.device.getId()).token();

        mockMvc.perform(deviceReading(token, "temperature", "22.1", "same-message"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("accepted"));
        mockMvc.perform(deviceReading(token, "temperature", "99.9", "same-message"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("already_processed"));

        assertThat(readingRepository.count()).isEqualTo(1);
        assertThat(readingRepository.findBySourceDeviceIdAndMessageId(
                fixture.device.getId(), "same-message").orElseThrow().getValue())
                .isEqualByComparingTo("22.1");
    }

    @Test
    void offsetTimestampIsStoredInTheApplicationsLocalTimeline() throws Exception {
        Fixture fixture = fixture("timezone", false);
        String token = credentialService.provision(fixture.device.getId()).token();

        mockMvc.perform(deviceReading(token, "temperature", "22.1", "timezone-message"))
                .andExpect(status().isOk());

        LocalDateTime measuredAt = readingRepository.findBySourceDeviceIdAndMessageId(
                fixture.device.getId(), "timezone-message").orElseThrow().getMeasuredAt();
        assertThat(measuredAt).isBetween(
                LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(2),
                LocalDateTime.now(ZoneId.systemDefault()));
    }

    @Test
    void sameMessageIdIsIndependentForDifferentDevices() throws Exception {
        Fixture first = fixture("message-first", false);
        Fixture second = fixture("message-second", false);
        String firstToken = credentialService.provision(first.device.getId()).token();
        String secondToken = credentialService.provision(second.device.getId()).token();

        mockMvc.perform(deviceReading(firstToken, "temperature", "21.1", "shared-message"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("accepted"));
        mockMvc.perform(deviceReading(secondToken, "temperature", "22.2", "shared-message"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("accepted"));

        assertThat(readingRepository.count()).isEqualTo(2);
    }

    @Test
    void invalidAndRevokedCredentialsAreRejected() throws Exception {
        Fixture fixture = fixture("credentials", false);
        var provisioned = credentialService.provision(fixture.device.getId());

        mockMvc.perform(deviceReading("lmdev_999999_invalid", "temperature", "21", "invalid"))
                .andExpect(status().isUnauthorized());
        credentialService.revoke(fixture.device.getId(), provisioned.credentialId());
        mockMvc.perform(deviceReading(provisioned.token(), "temperature", "21", "revoked"))
                .andExpect(status().isUnauthorized());
        assertThat(readingRepository.count()).isZero();
    }

    @Test
    void disabledDeviceIsRejectedWithoutUpdatingUsageTimestamps() throws Exception {
        Fixture fixture = fixture("disabled", false);
        var provisioned = credentialService.provision(fixture.device.getId());
        fixture.device.disable();
        deviceRepository.saveAndFlush(fixture.device);

        mockMvc.perform(deviceReading(provisioned.token(), "temperature", "21", "disabled-message"))
                .andExpect(status().isUnauthorized());

        entityManager.clear();
        assertThat(deviceRepository.findById(fixture.device.getId()).orElseThrow().getLastSeenAt()).isNull();
        assertThat(credentialRepository.findById(provisioned.credentialId()).orElseThrow().getLastUsedAt()).isNull();
        assertThat(readingRepository.count()).isZero();
    }

    @Test
    void outOfRangeDatabaseValueIsRejectedAsBadRequest() throws Exception {
        Fixture fixture = fixture("value-validation", false);
        String token = credentialService.provision(fixture.device.getId()).token();

        mockMvc.perform(deviceReading(token, "temperature", "1234567890.123", "oversized-value"))
                .andExpect(status().isBadRequest());

        assertThat(readingRepository.count()).isZero();
    }

    @Test
    void rotationRevokesOldTokenAndRawTokensAreNeverPersisted() throws Exception {
        Fixture fixture = fixture("rotation", false);
        var original = credentialService.provision(fixture.device.getId());
        var rotated = credentialService.rotate(fixture.device.getId());

        assertThat(original.token()).isNotEqualTo(rotated.token());
        assertThat(credentialRepository.findById(original.credentialId()).orElseThrow().getStatus())
                .isEqualTo(DeviceCredentialStatus.REVOKED);
        assertThat(credentialRepository.findAll())
                .allSatisfy(credential -> assertThat(credential.getCredentialHash())
                        .doesNotContain(original.token()).doesNotContain(rotated.token()));
        mockMvc.perform(deviceReading(original.token(), "temperature", "21", "old-token"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(deviceReading(rotated.token(), "temperature", "21", "new-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("accepted"));
    }

    @Test
    void unknownAndAnotherDevicesChannelsCannotBeUsed() throws Exception {
        Fixture first = fixture("first-device", false);
        Fixture second = fixture("second-device", false);
        String firstToken = credentialService.provision(first.device.getId()).token();

        mockMvc.perform(deviceReading(firstToken, "unknown", "21", "unknown-channel"))
                .andExpect(status().isNotFound());
        second.temperature.assignDeviceChannel(second.device, "foreign-only");
        sensorRepository.saveAndFlush(second.temperature);
        mockMvc.perform(deviceReading(firstToken, "foreign-only", "21", "foreign-channel"))
                .andExpect(status().isNotFound());
        assertThat(readingRepository.count()).isZero();
    }

    @Test
    void deviceTokenCannotAuthorizeUserApis() throws Exception {
        Fixture fixture = fixture("isolation", false);
        String token = credentialService.provision(fixture.device.getId()).token();
        mockMvc.perform(get("/api/organizations").header("Authorization", "Device " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void existingUserAuthenticatedReadingEndpointStillWorks() throws Exception {
        Fixture fixture = fixture("legacy", false);
        User administrator = new User("device-legacy-admin@example.com", passwordEncoder.encode("password123"),
                "Device", "Admin", null);
        administrator.setGlobalRole("SUPER_ADMIN");
        userRepository.saveAndFlush(administrator);

        Cookie session = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"device-legacy-admin@example.com","password":"password123"}
                                """))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("LABMONITOR_SESSION");
        assertThat(session).isNotNull();

        var csrfResponse = mockMvc.perform(get("/api/csrf").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        String csrfHeader = JsonPath.read(csrfResponse.getContentAsString(), "$.headerName");
        String csrfToken = JsonPath.read(csrfResponse.getContentAsString(), "$.token");
        mockMvc.perform(post("/api/sensor-readings")
                        .cookie(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sensorId":%d,"value":20.5}
                                """.formatted(fixture.temperature.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sensorId").value(fixture.temperature.getId()));
        assertThat(readingRepository.count()).isEqualTo(1);
    }

    private Fixture fixture(String suffix, boolean humidity) {
        Organization organization = organizationRepository.save(new Organization("Organization " + suffix, null));
        Lab lab = labRepository.save(new Lab(organization, "Lab " + suffix, null, null));
        Room room = roomRepository.save(new Room(lab, "Room " + suffix,
                RoomType.EXPERIMENT_ROOM, 1, BigDecimal.TEN));
        Device device = deviceRepository.save(new Device(room, "Device " + suffix,
                DeviceType.BROWSER_SIMULATOR));
        Sensor temperature = new Sensor(room, "Temperature " + suffix, SensorType.TEMPERATURE, "C");
        temperature.assignDeviceChannel(device, "temperature");
        sensorRepository.save(temperature);
        Sensor humiditySensor = null;
        if (humidity) {
            humiditySensor = new Sensor(room, "Humidity " + suffix, SensorType.HUMIDITY, "%");
            humiditySensor.assignDeviceChannel(device, "humidity");
            sensorRepository.save(humiditySensor);
        }
        entityManager.flush();
        return new Fixture(device, temperature, humiditySensor);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder deviceReading(
            String token, String channel, String value, String messageId) {
        String measuredAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1).toString();
        return post("/api/device/readings")
                .header("Authorization", "Device " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"channel":"%s","value":%s,"measuredAt":"%s","messageId":"%s"}
                        """.formatted(channel, value, measuredAt, messageId));
    }

    private record Fixture(Device device, Sensor temperature, Sensor humidity) {}
}
