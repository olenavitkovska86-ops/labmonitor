package com.olena.labmonitor.security;

import com.olena.labmonitor.membership.MembershipRepository;
import com.olena.labmonitor.organization.OrganizationRepository;
import com.olena.labmonitor.lab.LabRepository;
import com.olena.labmonitor.room.RoomRepository;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import com.olena.labmonitor.support.IntegrationFixtures;
import com.olena.labmonitor.auth.RefreshTokenRepository;
import com.olena.labmonitor.sensor.SensorRepository;
import com.olena.labmonitor.sensor.reading.SensorReadingRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class Phase1EndpointIntegrationTests {
 @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired OrganizationRepository orgs;
 @Autowired MembershipRepository memberships; @Autowired PasswordEncoder encoder; @Autowired RefreshTokenRepository refreshTokens;
 @Autowired LabRepository labs; @Autowired RoomRepository rooms;
 @Autowired SensorRepository sensors; @Autowired SensorReadingRepository readings;
 @Autowired EntityManager entityManager;

 private Cookie login(User u) throws Exception { entityManager.clear(); return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+u.getEmail()+"\",\"password\":\"password123\"}" )).andExpect(status().isNoContent()).andReturn().getResponse().getCookie("LABMONITOR_SESSION"); }
 private RequestPostProcessor validCsrf() { return csrf(); }

 @Test void superAdminCanCreateOneMembership() throws Exception {
  User admin=IntegrationFixtures.superAdmin(users,encoder,"membership"); User target=IntegrationFixtures.user(users,encoder,"target"); var org=IntegrationFixtures.organization(orgs,"membership"); long before=memberships.count();
  mvc.perform(post("/api/memberships").cookie(login(admin)).with(validCsrf()).contentType(MediaType.APPLICATION_JSON).content("{\"userId\":"+target.getId()+",\"organizationId\":"+org.getId()+",\"role\":\"LIMITED_EMPLOYEE\",\"scope\":{\"type\":\"ORGANIZATION\",\"labIds\":[],\"roomIds\":[]}}" )).andExpect(status().isCreated());
  assertThat(memberships.count()).isEqualTo(before+1); var m=memberships.findByUserIdAndOrganizationId(target.getId(),org.getId()).orElseThrow(); assertThat(m.getRole()).isEqualTo("LIMITED_EMPLOYEE"); assertThat(m.getScopeType().name()).isEqualTo("ORGANIZATION");
 }

 @Test void profileUpdatePersists() throws Exception {
  User u=IntegrationFixtures.user(users,encoder,"profile"); mvc.perform(put("/api/users/me").cookie(login(u)).with(validCsrf()).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"Updated\",\"lastName\":\"Person\",\"phone\":\"+48123456789\"}" )).andExpect(status().isOk());
  User reloaded=users.findById(u.getId()).orElseThrow(); assertThat(reloaded.getFirstName()).isEqualTo("Updated"); assertThat(reloaded.getLastName()).isEqualTo("Person"); assertThat(reloaded.getPhone()).isEqualTo("+48123456789");
 }

 @Test void changePasswordPersistsAndRevokesRefreshTokens() throws Exception {
  User u=IntegrationFixtures.user(users,encoder,"password"); mvc.perform(post("/auth/change-password").cookie(login(u)).with(validCsrf()).contentType(MediaType.APPLICATION_JSON).content("{\"oldPassword\":\"password123\",\"newPassword\":\"newPassword123\"}" )).andExpect(status().isOk());
  User reloaded=users.findById(u.getId()).orElseThrow(); assertThat(encoder.matches("newPassword123",reloaded.getPasswordHash())).isTrue(); assertThat(encoder.matches("password123",reloaded.getPasswordHash())).isFalse(); assertThat(refreshTokens.findAll()).allMatch(t -> t.getRevokedAt()!=null || !t.getUser().getId().equals(u.getId()));
 }

 @Test void labAdminCanMutateOnlyOverlappingScope() throws Exception {
  var org=IntegrationFixtures.organization(orgs,"team"); var lab=IntegrationFixtures.lab(labs,org,"team"); var room=IntegrationFixtures.room(rooms,lab,"team");
  var otherLab=IntegrationFixtures.lab(labs,org,"other"); var otherRoom=IntegrationFixtures.room(rooms,otherLab,"other");
  User admin=IntegrationFixtures.user(users,encoder,"labadmin"); memberships.saveAndFlush(IntegrationFixtures.scoped(memberships,org,admin,"LAB_ADMIN",lab,room));
  User target=IntegrationFixtures.user(users,encoder,"teamtarget"); memberships.saveAndFlush(IntegrationFixtures.scoped(memberships,org,target,"LIMITED_EMPLOYEE",lab,room));
  Cookie session=login(admin);
  String inScope="{\"type\":\"SPECIFIC\",\"labIds\":["+lab.getId()+"],\"roomIds\":[]}";
  mvc.perform(put("/api/team-access/organizations/"+org.getId()+"/users/"+target.getId()+"/scope").cookie(session).with(validCsrf()).contentType(MediaType.APPLICATION_JSON).content(inScope)).andExpect(status().isOk());
  String outScope="{\"type\":\"SPECIFIC\",\"labIds\":["+otherLab.getId()+"],\"roomIds\":[]}";
  mvc.perform(put("/api/team-access/organizations/"+org.getId()+"/users/"+target.getId()+"/scope").cookie(session).with(validCsrf()).contentType(MediaType.APPLICATION_JSON).content(outScope)).andExpect(status().isForbidden());
 }

 @Test void successfulPostSensorReadingPersistsExactlyOnce() throws Exception {
  User admin=IntegrationFixtures.superAdmin(users,encoder,"duplicate"); var org=IntegrationFixtures.organization(orgs,"duplicate"); var lab=IntegrationFixtures.lab(labs,org,"duplicate"); var room=IntegrationFixtures.room(rooms,lab,"duplicate"); var sensor=IntegrationFixtures.sensor(sensors,room,"duplicate");
  long before=readings.count();
  mvc.perform(post("/api/sensor-readings").cookie(login(admin)).with(validCsrf()).contentType(MediaType.APPLICATION_JSON).content("{\"sensorId\":"+sensor.getId()+",\"value\":21.5,\"measuredAt\":\"2026-08-30T12:00:00\"}" )).andExpect(status().isCreated());
  assertThat(readings.count()-before).isEqualTo(1);
 }
}
