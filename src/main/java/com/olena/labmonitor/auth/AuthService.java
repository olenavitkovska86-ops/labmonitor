package com.olena.labmonitor.auth;

import com.olena.labmonitor.auth.dto.ChangePassRequest;
import com.olena.labmonitor.auth.dto.LoginDto;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.security.JwtService;
import com.olena.labmonitor.security.JwtProperties;
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
import java.time.Clock;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       UserDetailsService userDetailsService, RefreshTokenRepository refreshTokenRepository,
                       JwtProperties jwtProperties, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    public void changePassword(Long userId, ChangePassRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())){
            throw new IllegalArgumentException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(userId, now());
    }

    public AuthTokens login(LoginDto login, String userAgent, String ipAddress){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login.getEmail(), login.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(login.getEmail());
        User user = userRepository.findByEmail(login.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setLastLoginAt(now());
        return issueTokens(user, userDetails, UUID.randomUUID().toString(), userAgent, ipAddress);

    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthTokens refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) throw new InvalidRefreshTokenException();

        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        LocalDateTime now = now();
        if (current.getRevokedAt() != null) {
            refreshTokenRepository.revokeAllByFamilyId(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (!current.getExpiresAt().isAfter(now)) {
            current.revoke(now);
            throw new InvalidRefreshTokenException();
        }

        User user = current.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        if (!userDetails.isEnabled()) {
            current.revoke(now);
            throw new InvalidRefreshTokenException();
        }

        current.revoke(now);
        return issueTokens(user, userDetails, current.getFamilyId(), userAgent, ipAddress);
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        refreshTokenRepository.findByTokenHashForUpdate(hash(rawRefreshToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.revoke(now()));
    }

    private AuthTokens issueTokens(User user, UserDetails userDetails, String familyId,
                                   String userAgent, String ipAddress) {
        String rawRefreshToken = newRefreshToken();
        LocalDateTime createdAt = now();
        LocalDateTime expiresAt = createdAt.plus(Duration.ofMillis(jwtProperties.refreshExpiration()));
        refreshTokenRepository.save(new RefreshToken(user, hash(rawRefreshToken), familyId, expiresAt,
                truncate(userAgent, 500), truncate(ipAddress, 45), createdAt));
        return new AuthTokens(jwtService.generateToken(userDetails), rawRefreshToken);
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String truncate(String value, int maximumLength) {
        if (value == null) return null;
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength);

    }
}
