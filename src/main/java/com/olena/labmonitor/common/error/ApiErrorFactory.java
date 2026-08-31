package com.olena.labmonitor.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ApiErrorFactory {
    private final Clock clock;

    public ApiErrorFactory(Clock clock) {
        this.clock = clock;
    }

    public ApiError create(HttpStatus status, String message, List<String> details) {
        return new ApiError(LocalDateTime.now(clock), status.value(), status.getReasonPhrase(), message, details);
    }
}
