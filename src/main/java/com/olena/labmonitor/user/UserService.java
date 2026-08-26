package com.olena.labmonitor.user;

import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.membership.Membership;
import com.olena.labmonitor.membership.MembershipRepository;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.user.dto.CreateUserRequest;
import com.olena.labmonitor.user.dto.UpdateUserRequest;
import com.olena.labmonitor.user.dto.UserResponse;
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

    public UserService(UserRepository userRepository, MembershipRepository membershipRepository,
                       UserMapper userMapper, PasswordEncoder passwordEncoder, UserValidator userValidator){
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.userValidator = userValidator;
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
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
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
