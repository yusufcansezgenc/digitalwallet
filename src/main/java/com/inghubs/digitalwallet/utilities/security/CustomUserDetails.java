package com.inghubs.digitalwallet.utilities.security;

import java.util.Collection;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.inghubs.digitalwallet.utilities.enums.Role;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CustomUserDetails implements UserDetails {

    private UUID id;
    private UUID customerId;
    private String username;
    private String password;
    private Role role;
    private Collection<? extends GrantedAuthority> authorities;
}
