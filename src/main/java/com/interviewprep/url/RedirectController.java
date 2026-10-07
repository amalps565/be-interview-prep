package com.interviewprep.url;

import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

  private final ShortLinkService service;

  public RedirectController(ShortLinkService service) {
    this.service = service;
  }

  @GetMapping("/{code:[A-Za-z0-9_-]{3,8}}")
  public ResponseEntity<Void> redirect(@PathVariable String code) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(service.resolve(code)))
        .cacheControl(CacheControl.noStore())
        .build();
  }
}
