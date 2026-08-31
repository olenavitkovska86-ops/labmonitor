package com.olena.labmonitor.auth;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Refresh session is invalid or expired");
    }
}
