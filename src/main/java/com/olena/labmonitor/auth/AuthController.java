package com.olena.labmonitor.auth;

import com.olena.labmonitor.auth.dto.ChangePassRequest;
import com.olena.labmonitor.auth.dto.LoginDto;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.security.JwtProperties;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final boolean secureCookie;
    private final long jwtExpirationMillis;
    private final long refreshExpirationMillis;

    public AuthController(UserRepository userRepository, AuthService authService, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.secureCookie = jwtProperties.cookieSecure();
        this.jwtExpirationMillis = jwtProperties.expiration();
        this.refreshExpirationMillis = jwtProperties.refreshExpiration();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginDto login, HttpServletRequest request){
        AuthTokens tokens = authService.login(login, request.getHeader(HttpHeaders.USER_AGENT), request.getRemoteAddr());
        return tokenResponse(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@org.springframework.web.bind.annotation.CookieValue(
            name = "LABMONITOR_REFRESH", required = false) String refreshToken,
                                        HttpServletRequest request) {
        AuthTokens tokens = authService.refresh(refreshToken, request.getHeader(HttpHeaders.USER_AGENT),
                request.getRemoteAddr());
        return tokenResponse(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@org.springframework.web.bind.annotation.CookieValue(
            name = "LABMONITOR_REFRESH", required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, accessCookie("", 0).toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString())
                .build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                               @Valid @RequestBody ChangePassRequest request){
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        authService.changePassword(user.getId(),request);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Void> tokenResponse(AuthTokens tokens) {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, accessCookie(tokens.accessToken(), jwtExpirationMillis / 1000).toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken(), refreshExpirationMillis / 1000).toString())
                .build();
    }

    private ResponseCookie accessCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from("LABMONITOR_SESSION", value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from("LABMONITOR_REFRESH", value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/auth")
                .maxAge(maxAgeSeconds)
                .build();
    }

}
