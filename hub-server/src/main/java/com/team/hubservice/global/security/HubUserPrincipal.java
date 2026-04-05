package com.team.hubservice.global.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * JWT subject(사용자 ID)와 역할을 Spring Security {@link UserDetails}로 노출합니다.
 * {@link org.springframework.security.core.userdetails.User#getUsername()}은 사용자 ID(UUID 문자열)를 반환합니다.
 */
public class HubUserPrincipal implements UserDetails {

    private final UUID userId;
    private final String role;
    private final List<GrantedAuthority> authorities;

    public HubUserPrincipal(UUID userId, String role) {
        this.userId = userId;
        this.role = role;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return userId.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
