package com.athletetrack.auth;

import com.athletetrack.security.JwtService;
import com.athletetrack.user.Role;
import com.athletetrack.user.UserAccount;
import com.athletetrack.user.UserAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountRepository users;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authentication;
    private final JwtService jwt;
    private final String coachRegistrationCode;
    public AuthController(UserAccountRepository users, PasswordEncoder passwords, AuthenticationManager authentication,
                          JwtService jwt, @Value("${athletetrack.coach.registration-code:}") String coachRegistrationCode) {
        this.users = users;
        this.passwords = passwords;
        this.authentication = authentication;
        this.jwt = jwt;
        this.coachRegistrationCode = coachRegistrationCode;
    }
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return register(request.email(), request.password(), request.displayName(), Role.PLAYER);
    }
    @PostMapping("/register-coach")
    public AuthResponse registerCoach(@Valid @RequestBody CoachRegisterRequest request) {
        if (coachRegistrationCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Coach registration is not configured.");
        }
        if (!MessageDigest.isEqual(coachRegistrationCode.getBytes(StandardCharsets.UTF_8),
                request.registrationCode().getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The coach invitation code is invalid.");
        }
        return register(request.email(), request.password(), request.displayName(), Role.COACH);
    }
    private AuthResponse register(String requestedEmail, String requestedPassword, String requestedName, Role role) {
        String email = requestedEmail.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        UserAccount user = users.save(new UserAccount(email, passwords.encode(requestedPassword),
                requestedName.trim(), role));
        return response(user);
    }
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication result = authentication.authenticate(new UsernamePasswordAuthenticationToken(
                request.email().trim().toLowerCase(), request.password()));
        UserAccount user = (UserAccount) result.getPrincipal();
        if (user.getRole() != request.role()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect for the selected account type.");
        }
        return response(user);
    }
    @GetMapping("/me")
    public ResponseEntity<SessionView> session(@AuthenticationPrincipal UserAccount user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(new SessionView(user.getId(), user.getRole()));
    }
    private AuthResponse response(UserAccount user) {
        return new AuthResponse(jwt.create(user), new AccountView(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole()));
    }
    public record RegisterRequest(@NotBlank @Email @Size(max=254) String email,
                                  @NotBlank @Size(min=8,max=72) String password,
                                  @NotBlank @Size(max=100) String displayName) {}
    public record CoachRegisterRequest(@NotBlank @Email @Size(max=254) String email,
                                       @NotBlank @Size(min=8,max=72) String password,
                                       @NotBlank @Size(max=100) String displayName,
                                       @NotBlank @Size(max=256) String registrationCode) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password, @NotNull Role role) {}
    public record AccountView(Long id, String email, String displayName, Role role) {}
    public record SessionView(Long id, Role role) {}
    public record AuthResponse(String token, AccountView account) {}
}
