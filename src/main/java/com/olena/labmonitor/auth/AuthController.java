package com.olena.labmonitor.auth;

import com.olena.labmonitor.auth.dto.ChangePassRequest;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

//    @PostMapping("/register")
//    public ResponseEntity<String> register(@Valid @RequestBody RegisterDto dto){
//
//        if (userRepository.findByEmail(dto.getEmail()).isPresent()){
//            return ResponseEntity.status(HttpStatus.CONFLICT)
//                    .body("Email is already registered");
//        }
//
//        User user = new User(
//                dto.getEmail(),
//                passwordEncoder.encode(dto.getPassword()),
//                dto.getFirstName(),
//                dto.getLastName(),
//                null
//        );
//        userRepository.save(user);
//        return ResponseEntity.status(HttpStatus.CREATED).body("User created");
//    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User user,
                                               @Valid @RequestBody ChangePassRequest request){
        authService.changePassword(user.getId(), request);
        return ResponseEntity.ok().build();
    }

}
