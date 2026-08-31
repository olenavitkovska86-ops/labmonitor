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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MembershipCsrfFlowTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void membershipPutRequiresCsrfAndAcceptsValidSuperAdminRequest() throws Exception {
        User admin = new User("membership-admin@example.com", passwordEncoder.encode("password123"),
                "Membership", "Admin", null);
        admin.setGlobalRole("SUPER_ADMIN");
        userRepository.save(admin);

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"membership-admin@example.com","password":"password123"}
                                """))
                .andExpect(status().isNoContent()).andReturn();
        Cookie session = login.getResponse().getCookie("LABMONITOR_SESSION");

        mockMvc.perform(put("/api/memberships/4").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"LIMITED_EMPLOYEE","scope":{"type":"ORGANIZATION","labIds":[],"roomIds":[]}}
                                """))
                .andExpect(status().isForbidden());

        MvcResult csrf = mockMvc.perform(get("/api/csrf").cookie(session))
                .andExpect(status().isOk()).andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(csrf.getResponse().getContentAsString(), "$.token");
        String header = com.jayway.jsonpath.JsonPath.read(csrf.getResponse().getContentAsString(), "$.headerName");
        Cookie csrfCookie = csrf.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();

        mockMvc.perform(put("/api/memberships/4").cookie(session, csrfCookie)
                        .header(header, token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"LIMITED_EMPLOYEE","scope":{"type":"ORGANIZATION","labIds":[],"roomIds":[]}}
                                """))
                .andExpect(status().isNotFound());
    }
}
