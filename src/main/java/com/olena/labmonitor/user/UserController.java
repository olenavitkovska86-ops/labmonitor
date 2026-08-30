package com.olena.labmonitor.user;

import com.olena.labmonitor.user.dto.CreateUserRequest;
import com.olena.labmonitor.user.dto.UpdateUserRequest;
import com.olena.labmonitor.user.dto.UserResponse;
import com.olena.labmonitor.user.dto.ManagedUserResponse;
import com.olena.labmonitor.user.dto.UpdateUserStatusRequest;
import com.olena.labmonitor.user.dto.UpdateNotificationPreferenceRequest;
import com.olena.labmonitor.user.dto.DemoteRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService){this.userService = userService;}

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping
    public java.util.List<UserResponse> findAll(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) String search) {
        return userService.findAll(organizationId, search);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/managed")
    public java.util.List<ManagedUserResponse> findAllManaged(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) String search) {
        return userService.findAllManaged(organizationId, search);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable("id") Long profile){return userService.findById(profile);}

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public UserResponse getMyProfile(Authentication authentication){
        String email = authentication.getName();
        return userService.findMe(email);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public UserResponse updateMyProfile(Authentication authentication,
                                        @Valid @RequestBody UpdateUserRequest request){
        return userService.updateMe(authentication.getName(), request);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/preferences/notifications")
    public UserResponse updateNotificationPreference(
            Authentication authentication,
            @Valid @RequestBody UpdateNotificationPreferenceRequest request
    ) {
        return userService.updateNotificationPreference(
                authentication.getName(), request.alertNotificationsEnabled());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/status")
    public UserResponse updateStatus(@PathVariable Long id, Authentication authentication,
                                     @Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.updateStatus(id, request.status(), authentication.getName());
    }

    // Super Admin ONLY
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request){
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/promote")
    public UserResponse promote(@PathVariable Long id) {
        return userService.promoteToSuperAdmin(id);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/demote")
    public UserResponse demote(@PathVariable Long id, @Valid @RequestBody DemoteRequest request) {
        return userService.demoteFromSuperAdmin(id, request);
    }


}
