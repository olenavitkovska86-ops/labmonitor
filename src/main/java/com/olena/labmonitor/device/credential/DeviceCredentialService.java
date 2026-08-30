package com.olena.labmonitor.device.credential;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.device.Device;
import com.olena.labmonitor.device.DeviceService;
import com.olena.labmonitor.device.DeviceStatus;
import com.olena.labmonitor.device.credential.dto.DeviceCredentialResponse;
import com.olena.labmonitor.device.credential.dto.ProvisionedDeviceCredentialResponse;
import com.olena.labmonitor.device.security.DevicePrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.List;

@Service
@Transactional
public class DeviceCredentialService {
    private static final String TOKEN_PREFIX = "lmdev_";
    private final DeviceCredentialRepository credentialRepository;
    private final DeviceService deviceService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public DeviceCredentialService(DeviceCredentialRepository credentialRepository,
                                   DeviceService deviceService, PasswordEncoder passwordEncoder) {
        this.credentialRepository = credentialRepository;
        this.deviceService = deviceService;
        this.passwordEncoder = passwordEncoder;
    }

    public ProvisionedDeviceCredentialResponse provision(Long deviceId) {
        Device device = deviceService.getExistingDeviceForUpdate(deviceId);
        if (!credentialRepository.findByDeviceIdAndStatus(deviceId, DeviceCredentialStatus.ACTIVE).isEmpty()) {
            throw new InvalidOperationException("Device already has an active credential; rotate it instead");
        }
        return issue(device);
    }

    public ProvisionedDeviceCredentialResponse rotate(Long deviceId) {
        Device device = deviceService.getExistingDeviceForUpdate(deviceId);
        LocalDateTime now = now();
        credentialRepository.findByDeviceIdAndStatus(deviceId, DeviceCredentialStatus.ACTIVE)
                .forEach(credential -> credential.revoke(now));
        return issue(device);
    }

    public DeviceCredentialResponse revoke(Long deviceId, Long credentialId) {
        DeviceCredential credential = getCredential(credentialId);
        if (!credential.getDevice().getId().equals(deviceId)) {
            throw new ResourceNotFoundException("Credential with id " + credentialId
                    + " was not found for device with id " + deviceId);
        }
        credential.revoke(now());
        return DeviceCredentialResponse.from(credentialRepository.saveAndFlush(credential));
    }

    @Transactional(readOnly = true)
    public List<DeviceCredentialResponse> findAll(Long deviceId) {
        deviceService.getExistingDevice(deviceId);
        return credentialRepository.findByDeviceIdOrderByIssuedAtDesc(deviceId).stream()
                .map(DeviceCredentialResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Optional<DevicePrincipal> authenticate(String rawToken) {
        ParsedToken parsed = parse(rawToken);
        if (parsed == null) return Optional.empty();
        return credentialRepository.findById(parsed.credentialId())
                .filter(DeviceCredential::isActive)
                .filter(credential -> credential.getDevice().getStatus() == DeviceStatus.ACTIVE)
                .filter(credential -> passwordEncoder.matches(parsed.secret(), credential.getCredentialHash()))
                .map(credential -> new DevicePrincipal(credential.getDevice().getId(), credential.getId()));
    }

    private ProvisionedDeviceCredentialResponse issue(Device device) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime issuedAt = now();
        DeviceCredential credential = credentialRepository.saveAndFlush(
                new DeviceCredential(device, passwordEncoder.encode(secret), issuedAt));
        String token = TOKEN_PREFIX + credential.getId() + "_" + secret;
        return new ProvisionedDeviceCredentialResponse(credential.getId(), device.getId(), token,
                credential.getStatus(), credential.getIssuedAt());
    }

    private DeviceCredential getCredential(Long id) {
        return credentialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device credential with id " + id + " was not found"));
    }

    private ParsedToken parse(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) return null;
        int separator = token.indexOf('_', TOKEN_PREFIX.length());
        if (separator < 0 || separator == token.length() - 1) return null;
        try {
            Long id = Long.valueOf(token.substring(TOKEN_PREFIX.length(), separator));
            return new ParsedToken(id, token.substring(separator + 1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private record ParsedToken(Long credentialId, String secret) {}
}
