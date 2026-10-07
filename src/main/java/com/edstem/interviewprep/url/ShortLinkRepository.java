package com.edstem.interviewprep.url;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

  Optional<ShortLink> findByCode(String code);

  Optional<ShortLink> findByDedupKey(String dedupKey);

  @Modifying
  @Query("update ShortLink s set s.visitCount = s.visitCount + 1 where s.id = :id")
  int incrementVisitCount(@Param("id") Long id);
}
