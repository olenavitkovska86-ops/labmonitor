package com.olena.labmonitor.device.credential;

import com.olena.labmonitor.device.credential.dto.DeviceCredentialResponse;
import com.olena.labmonitor.device.credential.dto.ProvisionedDeviceCredentialResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices/{deviceId}/credentials")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class DeviceCredentialController {
    private final DeviceCredentialService credentialService;

    public DeviceCredentialController(DeviceCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping
    public List<DeviceCredentialResponse> findAll(@PathVariable Long deviceId) {
        return credentialService.findAll(deviceId);
    }

    @PostMapping("/provision")
    @ResponseStatus(HttpStatus.CREATED)
    public ProvisionedDeviceCredentialResponse provision(@PathVariable Long deviceId) {
        return credentialService.provision(deviceId);
    }

    @PostMapping("/rotate")
    @ResponseStatus(HttpStatus.CREATED)
    public ProvisionedDeviceCredentialResponse rotate(@PathVariable Long deviceId) {
        return credentialService.rotate(deviceId);
    }

    @PostMapping("/{credentialId}/revoke")
    public DeviceCredentialResponse revoke(@PathVariable Long deviceId, @PathVariable Long credentialId) {
        return credentialService.revoke(deviceId, credentialId);
    }
}
