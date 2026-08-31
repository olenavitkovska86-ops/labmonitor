package com.olena.labmonitor.device.credential;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface DeviceCredentialRepository extends JpaRepository<DeviceCredential, Long> {
    @EntityGraph(attributePaths = "device")
    List<DeviceCredential> findByDeviceIdAndStatus(Long deviceId, DeviceCredentialStatus status);

    @EntityGraph(attributePaths = "device")
    List<DeviceCredential> findByDeviceIdOrderByIssuedAtDesc(Long deviceId);
}
