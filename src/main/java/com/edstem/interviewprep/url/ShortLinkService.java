package com.edstem.interviewprep.url;

import com.edstem.interviewprep.common.error.ApiException;
import com.edstem.interviewprep.common.error.ResourceNotFoundException;
import com.edstem.interviewprep.url.dto.ShortLinkStats;
import com.edstem.interviewprep.url.dto.ShortenRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortLinkService {

  private static final String RESOURCE = "Short link";
  private static final int MAX_ATTEMPTS = 5;
  private static final Set<String> RESERVED_CODES = Set.of("api", "error");

  private final ShortLinkRepository repository;
  private final ShortCodeGenerator generator;
  private final Clock clock;

  public ShortLinkService(
      ShortLinkRepository repository, ShortCodeGenerator generator, Clock clock) {
    this.repository = repository;
    this.generator = generator;
    this.clock = clock;
  }

  @Transactional
  public ShortenResult shorten(ShortenRequest request) {
    String url = request.url().strip();
    if (request.customCode() != null) {
      return new ShortenResult(
          createWithCustomCode(request.customCode(), url, request.expiresAt()), true);
    }
    Optional<ShortLink> existing =
        request.expiresAt() == null
            ? repository.findFirstByOriginalUrlAndExpiresAtIsNullOrderByIdAsc(url)
            : repository.findFirstByOriginalUrlAndExpiresAtOrderByIdAsc(url, request.expiresAt());
    return existing
        .map(link -> new ShortenResult(link, false))
        .orElseGet(() -> new ShortenResult(create(url, request.expiresAt()), true));
  }

  @Transactional
  public String resolve(String code) {
    ShortLink link = find(code);
    if (link.isExpiredAt(Instant.now(clock))) {
      throw new ApiException(HttpStatus.GONE, RESOURCE + " " + code + " has expired");
    }
    repository.incrementVisitCount(link.getId());
    return link.getOriginalUrl();
  }

  @Transactional(readOnly = true)
  public ShortLinkStats stats(String code) {
    return ShortLinkStats.from(find(code));
  }

  private ShortLink createWithCustomCode(String code, String url, Instant expiresAt) {
    if (RESERVED_CODES.contains(code.toLowerCase(Locale.ROOT)) || repository.existsByCode(code)) {
      throw new ApiException(HttpStatus.CONFLICT, "Short code " + code + " is already taken");
    }
    return repository.save(new ShortLink(code, url, expiresAt));
  }

  private ShortLink create(String url, Instant expiresAt) {
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      String code = generator.next();
      if (!repository.existsByCode(code)) {
        return repository.save(new ShortLink(code, url, expiresAt));
      }
    }
    throw new IllegalStateException("Could not generate a unique short code");
  }

  private ShortLink find(String code) {
    return repository
        .findByCode(code)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, code));
  }
}
