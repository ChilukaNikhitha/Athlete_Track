package com.athletetrack.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "user_accounts")
public class UserAccount implements UserDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false, length = 100)
    private String displayName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected UserAccount() {}

    public UserAccount(String email, String passwordHash, String displayName, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public Role getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
}
