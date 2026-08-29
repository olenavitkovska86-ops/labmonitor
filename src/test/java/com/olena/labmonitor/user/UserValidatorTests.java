package com.olena.labmonitor.user;

import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.organization.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserValidatorTests {

    @Mock UserRepository userRepository;
    @Mock OrganizationRepository organizationRepository;

    @Test
    void acceptsLocalDomain() {
        UserValidator validator = validatorWithAvailableEmail("labadmin@labmonitor.local");

        assertThatCode(() -> validator.validateEmail("labadmin@labmonitor.local"))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsLongTopLevelDomain() {
        UserValidator validator = validatorWithAvailableEmail("admin@example.museum");

        assertThatCode(() -> validator.validateEmail("admin@example.museum"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMalformedEmail() {
        UserValidator validator = validatorWithAvailableEmail("not-an-email");

        assertThatThrownBy(() -> validator.validateEmail("not-an-email"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Email format is invalid.");
    }

    private UserValidator validatorWithAvailableEmail(String email) {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        return new UserValidator(userRepository, organizationRepository);
    }
}
