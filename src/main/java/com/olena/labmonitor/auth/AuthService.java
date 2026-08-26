package com.olena.labmonitor.auth;

import com.olena.labmonitor.auth.dto.ChangePassRequest;
import com.olena.labmonitor.auth.dto.LoginDto;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.security.JwtService;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService, UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    public void changePassword(Long userId, ChangePassRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())){
            throw new IllegalArgumentException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public String login(LoginDto login){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login.getEmail(), login.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(login.getEmail());
        User user = userRepository.findByEmail(login.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setLastLoginAt(LocalDateTime.now());
        return jwtService.generateToken(userDetails);

    }
}
