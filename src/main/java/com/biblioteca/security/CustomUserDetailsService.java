package com.biblioteca.security;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.biblioteca.model.User;
import com.biblioteca.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con username/email: " + username));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> {
                    String name = role.getName();
                    if (name == null || name.isBlank()) return java.util.stream.Stream.empty();
                    String withPrefix = name.startsWith("ROLE_") ? name : "ROLE_" + name;
                    String withoutPrefix = name.startsWith("ROLE_") ? name.substring(5) : name;
                    return java.util.stream.Stream.of(
                            new SimpleGrantedAuthority(withPrefix),
                            new SimpleGrantedAuthority(withoutPrefix)
                    );
                })
                .distinct()
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                true,
                authorities
        );
    }
}
