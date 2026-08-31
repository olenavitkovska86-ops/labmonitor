package com.olena.labmonitor.common.error;

import com.olena.labmonitor.device.security.InvalidDeviceCredentialException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(
            new ApiErrorFactory(Clock.fixed(Instant.parse("2026-08-30T20:00:00Z"), ZoneOffset.UTC)));

    @Test
    void reportsUserLoginFailureWithoutReferringToDeviceCredentials() {
        var response = handler.handleBadCredentials(new BadCredentialsException("bad credentials"));

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid email or password");
    }

    @Test
    void keepsDeviceCredentialFailureDistinct() {
        var response = handler.handleInvalidDeviceCredential(new InvalidDeviceCredentialException());

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid device credential");
    }
}
