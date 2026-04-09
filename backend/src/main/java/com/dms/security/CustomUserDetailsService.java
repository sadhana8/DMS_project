package com.dms.security;

import com.dms.entity.User;
import com.dms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Spring Security {@link UserDetailsService} implementation that resolves users
 * by their e-mail address.
 *
 * <p>
 * Spring Security's default implementation looks users up by a "username"
 * field. DocVault uses <em>e-mail</em> as the unique identifier instead, so
 * this class overrides that behaviour.
 *
 * <h2>Role mapping</h2>
 * Each {@link com.dms.entity.Role} attached to the {@link User} entity is
 * mapped to a {@link SimpleGrantedAuthority} using the role's exact string name
 * (e.g. {@code "ROLE_ADMIN"}). Spring Security then uses these authorities for
 * all {@code hasRole()} and {@code @PreAuthorize} checks.
 *
 * <h2>Deactivated accounts</h2>
 * If the user's {@code isActive} flag is {@code false} a
 * {@link UsernameNotFoundException} is thrown, which Spring Security translates
 * to a {@code 401 Unauthorized} response.
 *
 * @author DocVault Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.dms.security.JwtAuthenticationFilter
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * Repository used to look up the {@link User} by e-mail.
     */
    private final UserRepository userRepository;

    /**
     * Loads the {@link UserDetails} for the given e-mail address.
     *
     * <p>
     * The {@code EAGER} fetch on {@link User#getRoles()} means roles are loaded
     * in the same SQL query, so the read-only transaction opened here covers
     * the entire operation.
     *
     * @param email the e-mail address used as the login credential
     * @return a fully-populated {@link UserDetails} instance including granted
     * authorities derived from the user's roles
     * @throws UsernameNotFoundException if no active user exists with the given
     * e-mail
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("User account is deactivated: " + email);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                        .collect(Collectors.toList()))
                .build();
    }
}
