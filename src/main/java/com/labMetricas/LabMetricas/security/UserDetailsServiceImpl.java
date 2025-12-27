package com.labMetricas.LabMetricas.security;

import com.labMetricas.LabMetricas.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String trimmed = email == null ? null : email.trim();
        if (trimmed == null || trimmed.isBlank()) {
            throw new UsernameNotFoundException("Usuario no encontrado con el email: " + email);
        }

        return userRepository.findByEmailWithRoleNormalized(trimmed)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con el email: " + trimmed));
    }
}