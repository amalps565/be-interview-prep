package com.interviewprep.auth;

import com.interviewprep.auth.dto.LoginRequest;
import com.interviewprep.auth.dto.RegisterRequest;
import com.interviewprep.auth.dto.TokenResponse;
import com.interviewprep.auth.dto.UserResponse;
import com.interviewprep.common.error.ApiException;
import com.interviewprep.common.error.InvalidFieldException;
import com.interviewprep.common.error.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final String INVALID_CREDENTIALS = "Invalid email or password";
  private static final int BCRYPT_MAX_BYTES = 72;

  private final AppUserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final String timingDecoyHash;

  public AuthService(
      AppUserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.timingDecoyHash = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  public UserResponse register(RegisterRequest request) {
    if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
      throw new InvalidFieldException("password", "must be at most 72 bytes when UTF-8 encoded");
    }
    String email = normalise(request.email());
    if (users.existsByEmail(email)) {
      throw emailTaken();
    }
    try {
      AppUser user = new AppUser(email, passwordEncoder.encode(request.password()), Role.USER);
      return UserResponse.from(users.saveAndFlush(user));
    } catch (DataIntegrityViolationException raceOnEmail) {
      throw emailTaken();
    }
  }

  @Transactional(readOnly = true)
  public TokenResponse login(LoginRequest request) {
    Optional<AppUser> user = users.findByEmail(normalise(request.email()));
    String hash = user.map(AppUser::getPasswordHash).orElse(timingDecoyHash);
    boolean matches = passwordEncoder.matches(request.password(), hash);
    if (user.isEmpty() || !matches) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
    }
    return tokenService.issue(user.get());
  }

  @Transactional(readOnly = true)
  public UserResponse profile(String email) {
    return users
        .findByEmail(email)
        .map(UserResponse::from)
        .orElseThrow(() -> new ResourceNotFoundException("User", email));
  }

  @Transactional(readOnly = true)
  public List<UserResponse> listUsers() {
    return users.findAllByOrderByIdAsc().stream().map(UserResponse::from).toList();
  }

  static String normalise(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }

  private ApiException emailTaken() {
    return new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
  }
}
