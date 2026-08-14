package com.olena.labmonitor.user;

import com.olena.labmonitor.user.dto.CreateUserRequest;
import com.olena.labmonitor.user.dto.UpdateUserRequest;
import com.olena.labmonitor.user.dto.UserResponse;
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

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable("id") Long profile){return userService.findById(profile);}

    @GetMapping("/me")
    public UserResponse getMyProfile(Authentication authentication){
        String email = authentication.getName();
        return userService.findMe(email);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request){
        return userService.update(id, request);
    }

    // Super Admin ONLY
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request){
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
