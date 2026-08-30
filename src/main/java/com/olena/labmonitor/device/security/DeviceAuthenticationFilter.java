package com.olena.labmonitor.device.security;

import com.olena.labmonitor.device.credential.DeviceCredentialService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class DeviceAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_PREFIX = "Device ";
    private final DeviceCredentialService credentialService;

    public DeviceAuthenticationFilter(DeviceCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/device/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(AUTHORIZATION_PREFIX)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        var principal = credentialService.authenticate(header.substring(AUTHORIZATION_PREFIX.length())).orElse(null);
        if (principal == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("DEVICE_INGEST")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
