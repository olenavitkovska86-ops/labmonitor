package com.olena.labmonitor.device;

import com.olena.labmonitor.device.credential.DeviceCredentialService;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.lab.LabRepository;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.organization.OrganizationRepository;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomRepository;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorRepository;
import com.olena.labmonitor.sensor.SensorType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DeviceAdministrationApiTests {
    @Autowired MockMvc mockMvc;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired LabRepository labRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired SensorRepository sensorRepository;
    @Autowired DeviceRepository deviceRepository;
    @Autowired DeviceCredentialService credentialService;

    @Test
    void superAdminCanListConfigureAndDisableDevice() throws Exception {
        Fixture fixture = fixture();

        mockMvc.perform(get("/api/devices").with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fixture.device.getId()))
                .andExpect(jsonPath("$[0].roomId").value(fixture.room.getId()))
                .andExpect(jsonPath("$[0].organizationId").value(fixture.room.getLab().getOrganization().getId()));

        mockMvc.perform(put("/api/devices/{deviceId}/sensors/{sensorId}",
                        fixture.device.getId(), fixture.sensor.getId())
                        .with(user("admin").roles("SUPER_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channelKey\":\" temperature \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channelKey").value("temperature"))
                .andExpect(jsonPath("$.sensorName").value(fixture.sensor.getName()));

        mockMvc.perform(get("/api/devices/{deviceId}/channels", fixture.device.getId())
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sensorId").value(fixture.sensor.getId()));

        mockMvc.perform(patch("/api/devices/{deviceId}/status", fixture.device.getId())
                        .with(user("admin").roles("SUPER_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void superAdminCanCreateAndAssignSensorFromDeviceCard() throws Exception {
        Fixture fixture = fixture();

        mockMvc.perform(post("/api/devices/{deviceId}/sensor-channels", fixture.device.getId())
                        .with(user("admin").roles("SUPER_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Device humidity","type":"HUMIDITY","unit":"%",
                                 "channelKey":"humidity","minSafeValue":30,"maxSafeValue":70}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceId").value(fixture.device.getId()))
                .andExpect(jsonPath("$.roomId").value(fixture.room.getId()))
                .andExpect(jsonPath("$.channelKey").value("humidity"));

        Sensor created = sensorRepository.findByDeviceIdAndChannelKey(fixture.device.getId(), "humidity").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(created.getRoom().getId()).isEqualTo(fixture.room.getId());
    }

    @Test
    void sensorFromAnotherRoomCannotBeAssigned() throws Exception {
        Fixture fixture = fixture();
        Room anotherRoom = roomRepository.save(new Room(fixture.room.getLab(), "Another room",
                RoomType.EXPERIMENT_ROOM, 2, BigDecimal.TEN));
        Sensor foreignSensor = sensorRepository.save(new Sensor(
                anotherRoom, "Foreign temperature", SensorType.TEMPERATURE, "C"));

        mockMvc.perform(put("/api/devices/{deviceId}/sensors/{sensorId}",
                        fixture.device.getId(), foreignSensor.getId())
                        .with(user("admin").roles("SUPER_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channelKey\":\"temperature\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void credentialListingNeverReturnsSecretOrHash() throws Exception {
        Fixture fixture = fixture();
        credentialService.provision(fixture.device.getId());

        mockMvc.perform(get("/api/devices/{deviceId}/credentials", fixture.device.getId())
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].credentialHash").doesNotExist())
                .andExpect(jsonPath("$[0].token").doesNotExist());
    }

    @Test
    void nonSuperAdminCannotReadDevices() throws Exception {
        mockMvc.perform(get("/api/devices").with(user("employee").roles("LIMITED_EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    private Fixture fixture() {
        Organization organization = organizationRepository.save(new Organization("Device API org", null));
        Lab lab = labRepository.save(new Lab(organization, "Device API lab", null, null));
        Room room = roomRepository.save(new Room(lab, "Device API room", RoomType.EXPERIMENT_ROOM, 1, BigDecimal.TEN));
        Sensor sensor = sensorRepository.save(new Sensor(room, "Device API temperature", SensorType.TEMPERATURE, "C"));
        Device device = deviceRepository.save(new Device(room, "Device API gateway", DeviceType.GATEWAY));
        return new Fixture(device, sensor, room);
    }

    private record Fixture(Device device, Sensor sensor, Room room) {}
}
