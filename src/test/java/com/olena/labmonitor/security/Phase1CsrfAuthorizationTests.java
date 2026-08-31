package com.olena.labmonitor.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase1CsrfAuthorizationTests {
    @Autowired MockMvc mockMvc;

    @Test
    void missingCsrfOnPostIsIdentifiableAndDoesNotReachController() throws Exception {
        mockMvc.perform(post("/api/sensor-readings")
                        .with(user("admin").roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"LIMITED_EMPLOYEE\",\"scope\":{\"type\":\"ORGANIZATION\",\"labIds\":[],\"roomIds\":[]}}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_FAILURE"));
    }

    @Test
    void authorizationFailureRemainsAPlainForbidden() throws Exception {
        mockMvc.perform(put("/api/memberships/1")
                        .with(user("employee").roles("LIMITED_EMPLOYEE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"LIMITED_EMPLOYEE\",\"scope\":{\"type\":\"ORGANIZATION\",\"labIds\":[],\"roomIds\":[]}}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void postMembershipWithValidCsrfPassesCsrfGate() throws Exception {
        mockMvc.perform(post("/api/memberships").with(user("admin").roles("SUPER_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"organizationId\":1,\"role\":\"LIMITED_EMPLOYEE\",\"scope\":{\"type\":\"ORGANIZATION\",\"labIds\":[],\"roomIds\":[]}}"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void postSensorReadingWithValidCsrfPassesCsrfGate() throws Exception {
        mockMvc.perform(post("/api/sensor-readings").with(user("admin").roles("SUPER_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensorId\":1,\"value\":21.5,\"measuredAt\":\"2026-08-30T12:00:00\"}"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void profileUpdateWithValidCsrfPassesCsrfGate() throws Exception {
        mockMvc.perform(put("/api/users/me").with(user("admin").roles("SUPER_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Admin\",\"lastName\":\"User\",\"phone\":\"+48123456789\"}"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void changePasswordWithValidCsrfPassesCsrfGate() throws Exception {
        mockMvc.perform(post("/auth/change-password").with(user("admin").roles("SUPER_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"password123\",\"newPassword\":\"newPassword123\"}"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }
}
