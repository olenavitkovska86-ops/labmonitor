package com.olena.labmonitor.user;

import com.olena.labmonitor.user.dto.CreateUserRequest;
import com.olena.labmonitor.user.dto.DemoteRequest;
import com.olena.labmonitor.user.dto.UpdateUserRequest;
import com.olena.labmonitor.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService){this.userService = userService;}

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable("id") Long profile){return userService.findById(profile);}

    @PreAuthorize("hasRole('LAB_ADMIN') or hasRole('LIMITED_EMPLOYEE')") // might have to change
    @GetMapping("/me")
    public UserResponse getMyProfile(Authentication authentication){
        String email = authentication.getName();
        return userService.findMe(email);
    }

    @PreAuthorize("hasRole('LAB_ADMIN') or hasRole('LIMITED_EMPLOYEE')")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request){
        return userService.update(id, request);
    }

    // ========================
    // SUPER_ADMIN ONLY
    // ========================

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request){
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get all users
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping
    public List<UserResponse> getAll(@RequestParam(required = false) Long organizationId,
                                     @RequestParam(required = false) String search){
        return userService.getAll(organizationId, search);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/promote")
    public ResponseEntity<UserResponse> promoteToSuperAdmin(@PathVariable Long id){
        UserResponse response = userService.promoteToSuperAdmin(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/demote")
    public ResponseEntity<UserResponse> demoteFromSuperAdmin(@PathVariable Long id, @RequestBody DemoteRequest request){
        UserResponse response = userService.demoteFromSuperAdmin(id, request.organizationId(), request.role());
        return ResponseEntity.ok(response);
    }


}
