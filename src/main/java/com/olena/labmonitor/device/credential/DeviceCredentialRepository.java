package com.olena.labmonitor.device.credential;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceCredentialRepository extends JpaRepository<DeviceCredential, Long> {
    List<DeviceCredential> findByDeviceIdAndStatus(Long deviceId, DeviceCredentialStatus status);
}
