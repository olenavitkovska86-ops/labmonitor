package com.olena.labmonitor.auth;

import com.olena.labmonitor.auth.dto.ChangePassRequest;
import com.olena.labmonitor.auth.dto.LoginDto;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDto login){
        String token = authService.login(login);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                               @Valid @RequestBody ChangePassRequest request){
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        authService.changePassword(user.getId(),request);
        return ResponseEntity.ok().build();
    }

}
