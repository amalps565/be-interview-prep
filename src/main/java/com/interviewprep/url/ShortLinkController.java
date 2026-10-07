package com.interviewprep.url;

import com.interviewprep.url.dto.ShortLinkResponse;
import com.interviewprep.url.dto.ShortLinkStats;
import com.interviewprep.url.dto.ShortenRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/urls")
public class ShortLinkController {

  private final ShortLinkService service;

  public ShortLinkController(ShortLinkService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ShortLinkResponse> shorten(@Valid @RequestBody ShortenRequest request) {
    ShortenResult result = service.shorten(request);
    ShortLink link = result.link();
    String shortUrl =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/{code}")
            .buildAndExpand(link.getCode())
            .toUriString();
    ShortLinkResponse body =
        new ShortLinkResponse(
            link.getCode(),
            shortUrl,
            link.getOriginalUrl(),
            link.getCreatedAt(),
            link.getExpiresAt());
    return result.created()
        ? ResponseEntity.status(HttpStatus.CREATED).location(URI.create(shortUrl)).body(body)
        : ResponseEntity.ok(body);
  }

  @GetMapping("/{code}/stats")
  public ShortLinkStats stats(@PathVariable String code) {
    return service.stats(code);
  }
}
