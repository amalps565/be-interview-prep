package com.interviewprep.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIntegrationTest {

  private final String userPassword = UUID.randomUUID().toString();

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private AppUserRepository users;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private AdminProperties admin;

  @Autowired private JwtEncoder jwtEncoder;

  @Autowired private JwtDecoder jwtDecoder;

  @BeforeEach
  void clean() {
    users.deleteAll();
    users.save(new AppUser(admin.email(), passwordEncoder.encode(admin.password()), Role.ADMIN));
  }

  @Test
  void userCannotAccessTheAdminEndpoint() throws Exception {
    String token = registerAndLogin("user@example.test");

    mockMvc
        .perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.path").value("/api/admin/users"));
  }

  @Test
  void adminCanListAllUsers() throws Exception {
    registerAndLogin("user@example.test");
    String adminToken = login(admin.email(), admin.password());

    mockMvc
        .perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].email", hasItem("user@example.test")))
        .andExpect(jsonPath("$[*].email", hasItem(admin.email())))
        .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
  }

  @Test
  void loggedInUserSeesTheirOwnProfile() throws Exception {
    String token = registerAndLogin("Me@Example.test");

    mockMvc
        .perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("me@example.test"))
        .andExpect(jsonPath("$.role").value("USER"));
  }

  @Test
  void requestWithoutLoginGets401AsJson() throws Exception {
    for (String path : List.of("/api/users/me", "/api/admin/users", "/api/tasks")) {
      mockMvc
          .perform(get(path))
          .andExpect(status().isUnauthorized())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(jsonPath("$.message").value("Authentication is required"));
    }
  }

  @Test
  void tamperedTokenGets401AsJson() throws Exception {
    String token = registerAndLogin("user@example.test");

    mockMvc
        .perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token + "x")))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void expiredTokenGets401() throws Exception {
    Instant issued = Instant.now().minus(Duration.ofMinutes(16));
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(JwtConfig.ISSUER)
            .subject(admin.email())
            .issuedAt(issued)
            .expiresAt(issued.plus(Duration.ofMinutes(15)))
            .claim(TokenService.ROLES_CLAIM, List.of("ADMIN"))
            .build();
    String expired =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

    mockMvc
        .perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginTokenExpiresAfterFifteenMinutes() throws Exception {
    String token = registerAndLogin("user@example.test");

    Jwt jwt = jwtDecoder.decode(token);

    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
        .isEqualTo(Duration.ofMinutes(15));
    assertThat(jwt.getClaimAsStringList(TokenService.ROLES_CLAIM)).containsExactly("USER");
  }

  @Test
  void passwordsAreStoredHashed() throws Exception {
    registerAndLogin("user@example.test");

    String stored = users.findByEmail("user@example.test").orElseThrow().getPasswordHash();

    assertThat(stored).startsWith("{bcrypt}").doesNotContain(userPassword);
  }

  @Test
  void wrongPasswordAndUnknownEmailGetTheSame401() throws Exception {
    registerAndLogin("user@example.test");

    for (String email : List.of("user@example.test", "nobody@example.test")) {
      loginRequest(email, "not-" + userPassword)
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
  }

  @Test
  void duplicateEmailGets409AndInvalidInputGets400() throws Exception {
    registerAndLogin("user@example.test");

    register(Map.of("email", "USER@example.test", "password", userPassword))
        .andExpect(status().isConflict());
    register(Map.of("email", "not-an-email", "password", "short"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.length()").value(2));
  }

  @Test
  void registrationCannotGrantAdmin() throws Exception {
    register(Map.of("email", "sneaky@example.test", "password", userPassword, "role", "ADMIN"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("USER"));
  }

  private String registerAndLogin(String email) throws Exception {
    register(Map.of("email", email, "password", userPassword)).andExpect(status().isCreated());
    return login(email, userPassword);
  }

  private ResultActions register(Map<String, String> body) throws Exception {
    return mockMvc.perform(
        post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)));
  }

  private String login(String email, String password) throws Exception {
    String body =
        loginRequest(email, password)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("accessToken").asText();
  }

  private ResultActions loginRequest(String email, String password) throws Exception {
    return mockMvc.perform(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                objectMapper.writeValueAsString(Map.of("email", email, "password", password))));
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
