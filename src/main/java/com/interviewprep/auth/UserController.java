package com.interviewprep.auth;

import com.interviewprep.auth.dto.UserResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

  private final AuthService authService;

  public UserController(AuthService authService) {
    this.authService = authService;
  }

  @GetMapping("/api/users/me")
  public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
    return authService.profile(jwt.getSubject());
  }

  @GetMapping("/api/admin/users")
  public List<UserResponse> listUsers() {
    return authService.listUsers();
  }
}
