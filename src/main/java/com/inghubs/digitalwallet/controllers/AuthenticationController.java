package com.inghubs.digitalwallet.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.*;
import org.springframework.web.bind.annotation.*;

import com.inghubs.digitalwallet.dtos.requests.AuthenticationRequest;
import com.inghubs.digitalwallet.utilities.security.CustomUserDetails;
import com.inghubs.digitalwallet.utilities.security.JwtUtility;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtility jwtUtils;

    @Operation(summary = "Authenticate user and generate JWT token", description = "Authenticates a user with username and password and returns a JWT token.")
    @PostMapping("/sign-in")
    public String authenticateUser(@Valid @RequestBody AuthenticationRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));
                        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return jwtUtils.generateToken(userDetails.getUsername());
    }
}
