package com.interviewprep.url;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "short_links",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_short_links_code", columnNames = "code"),
      @UniqueConstraint(name = "uk_short_links_dedup_key", columnNames = "dedup_key")
    })
public class ShortLink {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 8)
  private String code;

  @Column(nullable = false, length = 2048)
  private String originalUrl;

  @Column(name = "dedup_key", length = 64)
  private String dedupKey;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  private Instant expiresAt;

  @Column(nullable = false)
  private long visitCount;

  protected ShortLink() {}

  public ShortLink(String code, String originalUrl, Instant expiresAt) {
    this(code, originalUrl, expiresAt, null);
  }

  public ShortLink(String code, String originalUrl, Instant expiresAt, String dedupKey) {
    this.code = code;
    this.originalUrl = originalUrl;
    this.expiresAt = expiresAt;
    this.dedupKey = dedupKey;
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
