package com.interviewprep.auth;

import com.interviewprep.auth.dto.TokenResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

  static final String ROLES_CLAIM = "roles";
  static final String USER_ID_CLAIM = "uid";

  private final JwtEncoder encoder;
  private final JwtProperties properties;
  private final Clock clock;

  public TokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
    this.encoder = encoder;
    this.properties = properties;
    this.clock = clock;
  }

  public TokenResponse issue(AppUser user) {
    Instant now = Instant.now(clock);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(JwtConfig.ISSUER)
            .subject(user.getEmail())
            .issuedAt(now)
            .expiresAt(now.plus(properties.ttl()))
            .claim(USER_ID_CLAIM, user.getId())
            .claim(ROLES_CLAIM, List.of(user.getRole().name()))
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new TokenResponse(token, "Bearer", properties.ttl().toSeconds());
  }
}
