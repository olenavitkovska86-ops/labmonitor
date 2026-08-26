package com.olena.labmonitor.user;

import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.membership.Membership;
import com.olena.labmonitor.membership.MembershipRepository;
import com.olena.labmonitor.membership.MembershipService;
import com.olena.labmonitor.organization.Organization;
import com.olena.labmonitor.user.dto.CreateUserRequest;
import com.olena.labmonitor.user.dto.UpdateUserRequest;
import com.olena.labmonitor.user.dto.UserResponse;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;
    private final MembershipService membershipService;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, UserValidator userValidator, MembershipService membershipService){
        this.userRepository = userRepository;
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

    // Need authorization check in Controller
    public UserResponse update(Long id, UpdateUserRequest request){
        User user = getUser(id);
        user.update(request.firstName(), request.lastName(), request.phone());
        User savedUser = userRepository.saveAndFlush(user);

        return userMapper.toResponse(savedUser);
    }


    // ========================
    // SUPER_ADMIN ONLY
    // ========================

    @Transactional(readOnly = true)
    public List<UserResponse> getAll(Long organizationId, String search){
        List<User> users = findUsers(organizationId, search);
        return userMapper.toResponses(users);
    }

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

        // Not tied to any organization
        if (role.equals("SUPER_ADMIN")) {
            user.setGlobalRole("SUPER_ADMIN");
            User savedUser = userRepository.save(user);
            return userMapper.toResponse(savedUser);
        }

        User savedUser = userRepository.save(user);
        membershipService.createMembership(savedUser.getId(), request.organization(), role);
        return userMapper.toResponse(savedUser);

//        // In case of unhandled fourth role
//        throw new IllegalStateException("Unhandled role: " + role);
//
    }

    public UserResponse promoteToSuperAdmin(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setGlobalRole("SUPER_ADMIN");
        membershipService.deleteMembership(userId);
        return userMapper.toResponse(user);
    }

    public UserResponse demoteFromSuperAdmin(Long userId, Long organizationId, String role){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));

        user.setGlobalRole("NONE");
        membershipService.createMembership(userId, organizationId, role);
        return userMapper.toResponse(user);
    }




    // Helpers
    private User getUser(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private List<User> findUsers(Long organizationId, String search){
        boolean hasSearch = hasText(search);

        if (organizationId != null && hasSearch){
            return userRepository.searchByOrganizationIdAndName(organizationId, search.trim());
        }
        if (organizationId != null){
            return userRepository.findByOrganizationId(organizationId);
        }
        if (hasSearch){
            return userRepository.searchUserByName(search.trim());
        }
        return userRepository.findAll(Sort.by(Sort.Direction.ASC,"id"));
    }

    private boolean hasText(String value){
        return value != null && !value.trim().isBlank();
    }
}
