package com.edstem.interviewprep.url;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "short_links",
    indexes = @Index(name = "idx_short_links_original_url", columnList = "originalUrl"))
public class ShortLink {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 8)
  private String code;

  @Column(nullable = false, length = 2048)
  private String originalUrl;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  private Instant expiresAt;

  @Column(nullable = false)
  private long visitCount;

  protected ShortLink() {}

  public ShortLink(String code, String originalUrl, Instant expiresAt) {
    this.code = code;
    this.originalUrl = originalUrl;
    this.expiresAt = expiresAt;
  }

  public boolean isExpiredAt(Instant now) {
    return expiresAt != null && !now.isBefore(expiresAt);
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public long getVisitCount() {
    return visitCount;
  }
}
