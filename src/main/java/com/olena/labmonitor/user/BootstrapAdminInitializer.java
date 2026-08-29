package com.olena.labmonitor.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);
    private static final String FALLBACK_FIRST_NAME = "Bootstrap";
    private static final String FALLBACK_LAST_NAME = "Admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;

    public BootstrapAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${BOOTSTRAP_ADMIN_EMAIL:}") String email,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password,
            @Value("${BOOTSTRAP_ADMIN_FIRST_NAME:}") String firstName,
            @Value("${BOOTSTRAP_ADMIN_LAST_NAME:}") String lastName
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        if (isBlank(email)) {
            log.warn("Initial SUPER_ADMIN was not created because BOOTSTRAP_ADMIN_EMAIL is missing");
            return;
        }
        if (isBlank(password)) {
            log.warn("Initial SUPER_ADMIN was not created because BOOTSTRAP_ADMIN_PASSWORD is missing");
            return;
        }

        User user = new User(
                email.trim(),
                passwordEncoder.encode(password),
                valueOrFallback(firstName, FALLBACK_FIRST_NAME),
                valueOrFallback(lastName, FALLBACK_LAST_NAME),
                null
        );
        user.setStatus("ACTIVE");
        user.setGlobalRole("SUPER_ADMIN");

        try {
            userRepository.saveAndFlush(user);
            log.info("Initial SUPER_ADMIN account created");
        } catch (DataIntegrityViolationException exception) {
            if (userRepository.count() > 0) {
                log.info("Initial SUPER_ADMIN bootstrap skipped because a user was created concurrently");
                return;
            }
            throw exception;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String valueOrFallback(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }
}
