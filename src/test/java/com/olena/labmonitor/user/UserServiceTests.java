package com.olena.labmonitor.user;

import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.membership.MembershipRepository;
import com.olena.labmonitor.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock UserRepository userRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserValidator userValidator;

    @InjectMocks UserService userService;

    @Test
    void updatesOnlyTheUserResolvedFromAuthenticatedEmail() {
        User user = new User("employee@example.com", "hash", "Old", "Name", null);
        UpdateUserRequest request = new UpdateUserRequest("New", "Person", "+48 123 456 789");
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenAnswer(invocation ->
                com.olena.labmonitor.user.dto.UserResponse.from(invocation.getArgument(0)));

        var response = userService.updateMe("employee@example.com", request);

        assertThat(response.firstName()).isEqualTo("New");
        assertThat(response.lastName()).isEqualTo("Person");
        assertThat(response.phone()).isEqualTo("+48 123 456 789");
        assertThat(response.permissions()).contains("profile.read", "profile.update", "password.change");
        verify(userRepository).findByEmail("employee@example.com");
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void rejectsProfileUpdateWhenAuthenticatedUserNoLongerExists() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateMe(
                "missing@example.com", new UpdateUserRequest("New", "Person", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void persistsAlertNotificationPreferenceForAuthenticatedUser() {
        User user = new User("employee@example.com", "hash", "Employee", "User", null);
        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenAnswer(invocation ->
                com.olena.labmonitor.user.dto.UserResponse.from(invocation.getArgument(0)));

        var response = userService.updateNotificationPreference("employee@example.com", false);

        assertThat(response.alertNotificationsEnabled()).isFalse();
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void preventsAdministratorFromDisablingOwnAccount() {
        User user = new User("admin@example.com", "hash", "Admin", "User", null);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateStatus(7L, "DISABLED", "admin@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own account");
    }

    @Test
    void rejectsUnknownAccountStatus() {
        assertThatThrownBy(() -> userService.updateStatus(7L, "INVITED", "admin@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACTIVE or DISABLED");
    }
}
