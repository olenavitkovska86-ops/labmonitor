package com.olena.labmonitor.device;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.device.credential.DeviceCredentialRepository;
import com.olena.labmonitor.device.credential.DeviceCredentialService;
import com.olena.labmonitor.device.ingestion.DeviceIngestionService;
import com.olena.labmonitor.device.ingestion.dto.DeviceReadingRequest;
import com.olena.labmonitor.device.ingestion.dto.DeviceReadingResponse;
import com.olena.labmonitor.device.security.DevicePrincipal;
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
import com.olena.labmonitor.sensor.reading.SensorReadingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DeviceConcurrencyTests {

    @Autowired OrganizationRepository organizationRepository;
    @Autowired LabRepository labRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired SensorRepository sensorRepository;
    @Autowired DeviceRepository deviceRepository;
    @Autowired DeviceCredentialRepository credentialRepository;
    @Autowired SensorReadingRepository readingRepository;
    @Autowired DeviceCredentialService credentialService;
    @Autowired DeviceIngestionService ingestionService;
    @Autowired TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanUp() {
        transactionTemplate.executeWithoutResult(status -> {
            readingRepository.deleteAll();
            credentialRepository.deleteAll();
            sensorRepository.deleteAll();
            deviceRepository.deleteAll();
            roomRepository.deleteAll();
            labRepository.deleteAll();
            organizationRepository.deleteAll();
        });
    }

    @Test
    void concurrentProvisionLeavesExactlyOneActiveCredential() throws Exception {
        Long deviceId = createFixture("provision").deviceId();
        var attempts = runTogether(
                () -> credentialService.provision(deviceId),
                () -> credentialService.provision(deviceId)
        );

        assertThat(attempts).filteredOn(Attempt::successful).hasSize(1);
        assertThat(attempts).filteredOn(attempt -> attempt.error() instanceof InvalidOperationException).hasSize(1);
        assertThat(credentialRepository.findByDeviceIdAndStatus(
                deviceId, com.olena.labmonitor.device.credential.DeviceCredentialStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void concurrentDuplicateIngestionCreatesOneReading() throws Exception {
        Fixture fixture = createFixture("ingestion");
        var provisioned = credentialService.provision(fixture.deviceId());
        DevicePrincipal principal = new DevicePrincipal(fixture.deviceId(), provisioned.credentialId());
        DeviceReadingRequest request = new DeviceReadingRequest(
                "temperature", new BigDecimal("21.500"),
                OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1), "concurrent-message");

        List<Attempt<DeviceReadingResponse>> attempts = runTogether(
                () -> ingestionService.ingest(principal, request),
                () -> ingestionService.ingest(principal, request)
        );

        assertThat(attempts).allMatch(Attempt::successful);
        assertThat(attempts).extracting(attempt -> attempt.value().status())
                .containsExactlyInAnyOrder("accepted", "already_processed");
        assertThat(readingRepository.findBySourceDeviceIdAndMessageId(
                fixture.deviceId(), request.messageId())).isPresent();
        assertThat(readingRepository.count()).isEqualTo(1);
    }

    private Fixture createFixture(String suffix) {
        return transactionTemplate.execute(status -> {
            Organization organization = organizationRepository.save(
                    new Organization("Concurrent Organization " + suffix, null));
            Lab lab = labRepository.save(new Lab(organization, "Concurrent Lab " + suffix, null, null));
            Room room = roomRepository.save(new Room(lab, "Concurrent Room " + suffix,
                    RoomType.EXPERIMENT_ROOM, 1, BigDecimal.TEN));
            Device device = deviceRepository.save(new Device(
                    organization, "Concurrent Device " + suffix, DeviceType.GATEWAY));
            Sensor sensor = new Sensor(room, "Concurrent Temperature " + suffix,
                    SensorType.TEMPERATURE, "C");
            sensor.assignDeviceChannel(device, "temperature");
            sensorRepository.save(sensor);
            return new Fixture(device.getId());
        });
    }

    @SafeVarargs
    private static <T> List<Attempt<T>> runTogether(Callable<T>... operations) throws Exception {
        CountDownLatch ready = new CountDownLatch(operations.length);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(operations.length)) {
            List<Future<Attempt<T>>> futures = java.util.Arrays.stream(operations)
                    .map(operation -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        try {
                            return new Attempt<>(operation.call(), null);
                        } catch (Throwable error) {
                            return new Attempt<T>(null, error);
                        }
                    }))
                    .toList();
            ready.await();
            start.countDown();
            return futures.stream().map(DeviceConcurrencyTests::await).toList();
        }
    }

    private static <T> Attempt<T> await(Future<Attempt<T>> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError("Concurrent operation did not complete", exception);
        }
    }

    private record Fixture(Long deviceId) {}

    private record Attempt<T>(T value, Throwable error) {
        boolean successful() {
            return error == null;
        }
    }
}
