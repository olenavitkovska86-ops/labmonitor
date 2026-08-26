package com.olena.labmonitor.user;

import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.membership.Membership;
import com.olena.labmonitor.membership.MembershipRepository;
import com.olena.labmonitor.membership.MembershipService;
import com.olena.labmonitor.membership.dto.SaveMembershipRequest;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.user.dto.CreateUserRequest;
import com.olena.labmonitor.user.dto.UpdateUserRequest;
import com.olena.labmonitor.user.dto.UserResponse;
import com.olena.labmonitor.user.dto.DemoteRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;
    private final MembershipService membershipService;

    public UserService(UserRepository userRepository, MembershipRepository membershipRepository,
                       UserMapper userMapper, PasswordEncoder passwordEncoder, UserValidator userValidator,
                       MembershipService membershipService){
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.userValidator = userValidator;
        this.membershipService = membershipService;
    }


    @Transactional(readOnly = true)
    public UserResponse findMe(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id){
        User user = getUser(id);

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll(Long organizationId, String search) {
        boolean hasSearch = search != null && !search.isBlank();
        List<User> users;
        if (organizationId != null && hasSearch) {
            users = userRepository.searchByOrganizationId(organizationId, search.trim());
        } else if (organizationId != null) {
            users = userRepository.findByOrganizationId(organizationId);
        } else if (hasSearch) {
            users = userRepository.search(search.trim());
        } else {
            users = userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        }
        return users.stream().map(userMapper::toResponse).toList();
    }

    public UserResponse promoteToSuperAdmin(Long userId) {
        User user = getUser(userId);
        user.setGlobalRole("SUPER_ADMIN");
        membershipRepository.deleteAll(membershipRepository.findByUserId(userId));
        user.getMemberships().clear();
        return userMapper.toResponse(user);
    }

    public UserResponse demoteFromSuperAdmin(Long userId, DemoteRequest request) {
        User user = getUser(userId);
        if (!"SUPER_ADMIN".equals(user.getGlobalRole())) {
            throw new IllegalArgumentException("User is not a SUPER_ADMIN");
        }
        user.setGlobalRole("NONE");
        membershipService.create(new SaveMembershipRequest(
                userId, request.organizationId(), request.role(), request.scope()));
        return userMapper.toResponse(user);
    }

    public UserResponse updateStatus(Long id, String status, String actingUserEmail) {
        if (!Set.of("ACTIVE", "DISABLED").contains(status)) {
            throw new IllegalArgumentException("Status must be ACTIVE or DISABLED");
        }
        User user = getUser(id);
        if (user.getEmail().equalsIgnoreCase(actingUserEmail) && "DISABLED".equals(status)) {
            throw new IllegalArgumentException("You cannot disable your own account");
        }
        user.setStatus(status);
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    public UserResponse updateMe(String email, UpdateUserRequest request){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        user.update(request.firstName(), request.lastName(), request.phone());
        User savedUser = userRepository.saveAndFlush(user);

        return userMapper.toResponse(savedUser);
    }

    public UserResponse updateNotificationPreference(String email, boolean enabled) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        user.setAlertNotificationsEnabled(enabled);
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }


    // Super Admin
    public UserResponse createUser(CreateUserRequest request) {
        userValidator.validateEmail(request.email());
        userValidator.validateRole(request.role());

        String role = request.role();

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                null
        );

        if (role.equals("SUPER_ADMIN")) {
            user.setGlobalRole("SUPER_ADMIN");
            User savedUser = userRepository.save(user);
            return userMapper.toResponse(savedUser);
        }

        // Required orgaization ID
        if (role.equals("LAB_ADMIN") || role.equals("LIMITED_EMPLOYEE")){
            Organization organization = userValidator.validateAndGetOrganization(request.organization());

            User savedUser = userRepository.save(user);

            Membership membership = new Membership(organization, savedUser, role);
            membershipRepository.save(membership);
            savedUser.getMemberships().add(membership);

            return userMapper.toResponse(savedUser);
        }

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);


    }



    //
    private User getUser(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
