package com.olena.labmonitor.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminInitializerTests {

    @Mock UserRepository userRepository;

    @Test
    void createsSuperAdminWhenUsersTableIsEmptyAndEnvironmentValuesAreValid() throws Exception {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        when(userRepository.count()).thenReturn(0L);
        BootstrapAdminInitializer initializer = initializer(
                encoder, "admin@example.com", "safe-password", "Cloud", "Admin");

        initializer.run(new DefaultApplicationArguments());

        User saved = savedUser();
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getFirstName()).isEqualTo("Cloud");
        assertThat(saved.getLastName()).isEqualTo("Admin");
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getGlobalRole()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void skipsBootstrapWhenAUserAlreadyExists() throws Exception {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        when(userRepository.count()).thenReturn(1L);

        initializer(encoder, "admin@example.com", "safe-password", "Cloud", "Admin")
                .run(new DefaultApplicationArguments());

        verify(userRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void skipsBootstrapWhenEmailIsMissing() throws Exception {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        when(userRepository.count()).thenReturn(0L);

        initializer(encoder, " ", "safe-password", "Cloud", "Admin")
                .run(new DefaultApplicationArguments());

        verify(userRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void skipsBootstrapWhenPasswordIsMissing() throws Exception {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        when(userRepository.count()).thenReturn(0L);

        initializer(encoder, "admin@example.com", " ", "Cloud", "Admin")
                .run(new DefaultApplicationArguments());

        verify(userRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void storesAPasswordThatMatchesUsingTheConfiguredEncoder() throws Exception {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        when(userRepository.count()).thenReturn(0L);

        initializer(encoder, "admin@example.com", "safe-password", "", "")
                .run(new DefaultApplicationArguments());

        User saved = savedUser();
        assertThat(saved.getPasswordHash()).isNotEqualTo("safe-password");
        assertThat(encoder.matches("safe-password", saved.getPasswordHash())).isTrue();
        assertThat(saved.getFirstName()).isEqualTo("Bootstrap");
        assertThat(saved.getLastName()).isEqualTo("Admin");
    }

    private BootstrapAdminInitializer initializer(
            PasswordEncoder encoder, String email, String password, String firstName, String lastName
    ) {
        return new BootstrapAdminInitializer(
                userRepository, encoder, email, password, firstName, lastName);
    }

    private User savedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }
}
