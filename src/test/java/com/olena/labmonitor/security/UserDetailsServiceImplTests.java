package com.olena.labmonitor.security;

import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTests {
    private final UserRepository repository = mock(UserRepository.class);
    private final UserDetailsServiceImpl service = new UserDetailsServiceImpl(repository);

    @Test
    void activeUserIsEnabled() {
        User user = new User("active@example.com", "hash", "Active", "User", null);
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername(user.getEmail()).isEnabled()).isTrue();
    }

    @Test
    void disabledUserCannotAuthenticate() {
        User user = new User("disabled@example.com", "hash", "Disabled", "User", null);
        user.setStatus("DISABLED");
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername(user.getEmail()).isEnabled()).isFalse();
    }
}
