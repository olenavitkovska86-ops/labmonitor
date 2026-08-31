package com.olena.labmonitor.support;

import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.organization.OrganizationRepository;
import com.olena.labmonitor.lab.Lab;
import com.olena.labmonitor.lab.LabRepository;
import com.olena.labmonitor.room.Room;
import com.olena.labmonitor.room.RoomRepository;
import com.olena.labmonitor.room.RoomType;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.sensor.SensorRepository;
import com.olena.labmonitor.sensor.SensorType;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import com.olena.labmonitor.membership.Membership;
import com.olena.labmonitor.membership.MembershipRepository;
import com.olena.labmonitor.membership.MembershipScopeType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Set;

/** Real JPA fixtures shared by security integration tests. */
public final class IntegrationFixtures {
    private IntegrationFixtures() {}
    public static User superAdmin(UserRepository r, PasswordEncoder e, String suffix) {
        User u = new User("super-" + suffix + "@example.com", e.encode("password123"), "Super", "Admin", null);
        u.setGlobalRole("SUPER_ADMIN"); return r.saveAndFlush(u);
    }
    public static User user(UserRepository r, PasswordEncoder e, String suffix) {
        return r.saveAndFlush(new User("user-" + suffix + "@example.com", e.encode("password123"), "Test", "User", null));
    }
    public static Organization organization(OrganizationRepository r, String suffix) { return r.saveAndFlush(new Organization("Organization " + suffix, null)); }
    public static Lab lab(LabRepository r, Organization o, String suffix) { return r.saveAndFlush(new Lab(o, "Lab " + suffix, null, null)); }
    public static Room room(RoomRepository r, Lab l, String suffix) { return r.saveAndFlush(new Room(l, "Room " + suffix, RoomType.EXPERIMENT_ROOM, 1, BigDecimal.TEN)); }
    public static Sensor sensor(SensorRepository r, Room room, String suffix) { return r.saveAndFlush(new Sensor(room, "Temperature " + suffix, SensorType.TEMPERATURE, "C")); }
    public static Membership membership(MembershipRepository r, Organization o, User u, String role) { Membership m = new Membership(o, u, role); u.getMemberships().add(m); return r.saveAndFlush(m); }
    public static Membership scoped(MembershipRepository r, Organization o, User u, String role, Lab l, Room room) {
        Membership m = new Membership(o, u, role); m.updateAccess(role, MembershipScopeType.SPECIFIC, Set.of(l), Set.of(room)); u.getMemberships().add(m); return r.saveAndFlush(m);
    }
}
