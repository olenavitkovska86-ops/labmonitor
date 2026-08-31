package com.olena.labmonitor.security;

import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthRefreshFlowTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void rotatesRefreshTokenAndRejectsItsReuse() throws Exception {
        userRepository.save(new User("refresh@example.com", passwordEncoder.encode("password123"),
                "Refresh", "User", null));

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"refresh@example.com","password":"password123"}
                                """))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie access = login.getResponse().getCookie("LABMONITOR_SESSION");
        Cookie originalRefresh = login.getResponse().getCookie("LABMONITOR_REFRESH");
        assertThat(access).isNotNull();
        assertThat(originalRefresh).isNotNull();
        assertThat(originalRefresh.isHttpOnly()).isTrue();
        assertThat(originalRefresh.getPath()).isEqualTo("/auth");

        MvcResult refreshed = mockMvc.perform(post("/auth/refresh").cookie(originalRefresh))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie newAccess = refreshed.getResponse().getCookie("LABMONITOR_SESSION");
        Cookie newRefresh = refreshed.getResponse().getCookie("LABMONITOR_REFRESH");
        assertThat(newAccess).isNotNull();
        assertThat(newRefresh).isNotNull();
        assertThat(newRefresh.getValue()).isNotEqualTo(originalRefresh.getValue());

        mockMvc.perform(get("/api/users/me").cookie(newAccess))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/refresh").cookie(originalRefresh))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/refresh").cookie(newRefresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieIsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }
}
