package com.olena.labmonitor.security;

import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdministrationSecurityFlowTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void superAdminCapabilityAuthorityAndCsrfFlowStayConsistent() throws Exception {
        User administrator = user("admin@example.com", "SUPER_ADMIN");
        User employee = user("employee@example.com", "NONE");
        userRepository.save(administrator);
        userRepository.save(employee);

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.com","password":"password123"}
                                """))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie session = login.getResponse().getCookie("LABMONITOR_SESSION");
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/users/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.globalRole").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.permissions[?(@ == 'users.manage')]").exists());

        mockMvc.perform(get("/api/users").cookie(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/organizations").cookie(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/labs").cookie(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/rooms").cookie(session)).andExpect(status().isOk());

        MvcResult csrfResult = mockMvc.perform(get("/api/csrf").cookie(session))
                .andExpect(status().isOk())
                .andReturn();
        String csrfJson = csrfResult.getResponse().getContentAsString();
        String csrfHeader = JsonPath.read(csrfJson, "$.headerName");
        String csrfToken = JsonPath.read(csrfJson, "$.token");
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();

        mockMvc.perform(put("/api/users/{id}/status", employee.getId())
                        .cookie(session, csrfCookie)
                        .header(csrfHeader, csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    private User user(String email, String globalRole) {
        User user = new User(email, passwordEncoder.encode("password123"), "Test", "User", null);
        user.setGlobalRole(globalRole);
        return user;
    }
}
