package com.backend.libserver.auth;

import com.backend.libserver.auth.password.EmailVerificationService;
import com.backend.libserver.security.JwtService;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Login accepts a username or an email address; these pin down which lookup each one takes. */
class AuthServiceLoginTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);

    private final AuthService authService =
            new AuthService(userRepository, passwordEncoder, jwtService, emailVerificationService);

    @Test
    void logsInWithUsername() {
        givenUser();
        when(userRepository.findByUsername("vamshi")).thenReturn(Optional.of(user()));

        AuthResponse response = authService.login(new LoginRequest("vamshi", "secret123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("vamshi");
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void logsInWithEmail() {
        givenUser();
        when(userRepository.findByEmailIgnoreCase("Vamshi@Example.com")).thenReturn(Optional.of(user()));

        AuthResponse response = authService.login(new LoginRequest("Vamshi@Example.com", "secret123"));

        assertThat(response.username()).isEqualTo("vamshi");
        // An "@" means email, so the username lookup must not be attempted at all.
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void usernameLookupIsCaseInsensitiveAndTrimmed() {
        givenUser();
        when(userRepository.findByUsername("vamshi")).thenReturn(Optional.of(user()));

        assertThat(authService.login(new LoginRequest("  VamShi  ", "secret123")).username())
                .isEqualTo("vamshi");
    }

    @Test
    void unknownIdentifierIsRejected() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "secret123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void wrongPasswordIsRejected() {
        when(userRepository.findByUsername("vamshi")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("vamshi", "wrong")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid credentials");
    }

    private void givenUser() {
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(any(UUID.class), anyString())).thenReturn("jwt-token");
    }

    private User user() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("vamshi");
        user.setEmail("vamshi@example.com");
        user.setPasswordHash("hashed");
        return user;
    }
}
