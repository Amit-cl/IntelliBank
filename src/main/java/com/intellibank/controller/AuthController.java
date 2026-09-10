package com.intellibank.controller;
import com.intellibank.dto.LoginRequest;
import com.intellibank.dto.LoginResponse;
import com.intellibank.dto.RegisterRequest;
import com.intellibank.entity.User;
import com.intellibank.entity.AuditAction;
import com.intellibank.security.TokenService;
import com.intellibank.service.AuditService;
import com.intellibank.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final AuditService auditService;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          TokenService tokenService,
                          AuditService auditService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.auditService = auditService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User registeredUser = userService.registerCustomer(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("username", registeredUser.getUsername());
        response.put("enabled", registeredUser.isEnabled());
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        String token = tokenService.generateToken(authentication);

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        auditService.log(authentication.getName(), AuditAction.LOGIN, "User logged in successfully");

        return ResponseEntity.ok(new LoginResponse(token, authentication.getName(), roles));
    }
}
