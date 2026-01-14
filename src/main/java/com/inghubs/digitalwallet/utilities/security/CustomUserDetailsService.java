package com.inghubs.digitalwallet.utilities.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import com.inghubs.digitalwallet.repositories.UserRepository;
import com.inghubs.digitalwallet.entities.User;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User Not Found with username: " + username);
        }
        return CustomUserDetails.builder()
                .id(user.getId())
                .customerId(user.getCustomer().getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .role(user.getRole())
                .authorities(Collections.emptyList())
                .build();
    }
}
