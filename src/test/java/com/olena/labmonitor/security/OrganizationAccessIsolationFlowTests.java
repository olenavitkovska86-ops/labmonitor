package com.olena.labmonitor.security;

import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.lab.LabRepository;
import com.olena.labmonitor.membership.Membership;
import com.olena.labmonitor.membership.MembershipRepository;
import com.olena.labmonitor.membership.MembershipScopeType;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.organization.OrganizationRepository;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomRepository;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.session.MonitoringSession;
import com.olena.labmonitor.session.MonitoringSessionRepository;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrganizationAccessIsolationFlowTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired LabRepository labRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired MembershipRepository membershipRepository;
    @Autowired MonitoringSessionRepository sessionRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager entityManager;

    @Test
    void membershipsInTwoOrganizationsReturnOnlyResourcesAllowedByEachSpecificScope() throws Exception {
        User user = saveUser("multi-org-flow@example.com", "NONE");
        Resources first = resources("First organization", "First lab", "Allowed first room", "Hidden first room", user);
        Resources second = resources("Second organization", "Second lab", "Allowed second room", "Hidden second room", user);
        saveSpecificMembership(first, user, Set.of(), Set.of(first.allowedRoom));
        saveSpecificMembership(second, user, Set.of(second.lab), Set.of());
        flushAndClear();
        Cookie session = login(user.getEmail());

        mockMvc.perform(get("/api/organizations").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/labs").param("organizationId", first.organization.getId().toString()).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(first.lab.getId()));
        mockMvc.perform(get("/api/rooms").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.id == %s)]", first.hiddenRoom.getId()).doesNotExist())
                .andExpect(jsonPath("$[?(@.id == %s)]", first.allowedRoom.getId()).exists())
                .andExpect(jsonPath("$[?(@.id == %s)]", second.allowedRoom.getId()).exists())
                .andExpect(jsonPath("$[?(@.id == %s)]", second.hiddenRoom.getId()).exists());
        mockMvc.perform(get("/api/monitoring-sessions").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.roomId == %s)]", first.hiddenRoom.getId()).doesNotExist());
    }

    @Test
    void manuallySupplyingInaccessibleOrganizationOrSessionIsDenied() throws Exception {
        User user = saveUser("manual-id-flow@example.com", "NONE");
        Resources allowed = resources("Allowed organization", "Allowed lab", "Allowed room", "Hidden sibling", user);
        Resources foreign = resources("Foreign organization", "Foreign lab", "Foreign room", "Other foreign room", user);
        saveSpecificMembership(allowed, user, Set.of(), Set.of(allowed.allowedRoom));
        flushAndClear();
        Cookie session = login(user.getEmail());

        mockMvc.perform(get("/api/analytics/organizations/{id}/history", foreign.organization.getId()).cookie(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/rooms/{id}", foreign.allowedRoom.getId()).cookie(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/monitoring-sessions/{id}", foreign.allowedSession.getId()).cookie(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/monitoring-sessions/{id}/timeline", foreign.allowedSession.getId()).cookie(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/monitoring-sessions/{id}/export", foreign.allowedSession.getId()).cookie(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminStillReceivesResourcesFromEveryOrganization() throws Exception {
        User administrator = saveUser("super-admin-flow@example.com", "SUPER_ADMIN");
        resources("Admin first organization", "Admin first lab", "Admin room one", "Admin room two", administrator);
        resources("Admin second organization", "Admin second lab", "Admin room three", "Admin room four", administrator);
        flushAndClear();
        Cookie session = login(administrator.getEmail());

        mockMvc.perform(get("/api/organizations").cookie(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/rooms").cookie(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
        mockMvc.perform(get("/api/monitoring-sessions").cookie(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
    }

    private Resources resources(String organizationName, String labName, String allowedRoomName,
                                String hiddenRoomName, User creator) {
        Organization organization = organizationRepository.save(new Organization(organizationName, null));
        Lab lab = labRepository.save(new Lab(organization, labName, null, null));
        Room allowedRoom = roomRepository.save(new Room(lab, allowedRoomName, RoomType.EXPERIMENT_ROOM, 1, BigDecimal.TEN));
        Room hiddenRoom = roomRepository.save(new Room(lab, hiddenRoomName, RoomType.EXPERIMENT_ROOM, 1, BigDecimal.TEN));
        MonitoringSession allowedSession = sessionRepository.save(new MonitoringSession(allowedRoom, allowedRoomName + " session", null, creator));
        sessionRepository.save(new MonitoringSession(hiddenRoom, hiddenRoomName + " session", null, creator));
        return new Resources(organization, lab, allowedRoom, hiddenRoom, allowedSession);
    }

    private void saveSpecificMembership(Resources resources, User user, Set<Lab> labs, Set<Room> rooms) {
        Membership membership = new Membership(resources.organization, user, "LIMITED_EMPLOYEE");
        membership.updateAccess("LIMITED_EMPLOYEE", MembershipScopeType.SPECIFIC, labs, rooms);
        membershipRepository.save(membership);
    }

    private User saveUser(String email, String role) {
        User user = new User(email, passwordEncoder.encode("password123"), "Test", "User", null);
        user.setGlobalRole(role);
        return userRepository.save(user);
    }

    private Cookie login(String email) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("LABMONITOR_SESSION");
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private record Resources(Organization organization, Lab lab, Room allowedRoom,
                             Room hiddenRoom, MonitoringSession allowedSession) {}
}
