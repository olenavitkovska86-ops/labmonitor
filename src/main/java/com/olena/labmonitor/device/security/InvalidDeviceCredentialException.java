package com.olena.labmonitor.device.security;

import org.springframework.security.authentication.BadCredentialsException;

public class InvalidDeviceCredentialException extends BadCredentialsException {
    public InvalidDeviceCredentialException() {
        super("Invalid device credential");
    }
}
