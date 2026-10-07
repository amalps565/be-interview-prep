package com.interviewprep.url;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VisitCountConcurrencyTest {

  private static final int VISITS = 200;
  private static final int THREADS = 32;

  @Autowired private ShortLinkService service;

  @Autowired private ShortLinkRepository repository;

  @Test
  void simultaneousVisitsAreAllCounted() throws Exception {
    ShortLink link = repository.save(new ShortLink("busy001", "https://example.com", null));
    CountDownLatch start = new CountDownLatch(1);
    List<Future<String>> visits = new ArrayList<>();

    try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
      for (int i = 0; i < VISITS; i++) {
        visits.add(
            pool.submit(
                () -> {
                  start.await();
                  return service.resolve(link.getCode());
                }));
      }
      start.countDown();
      for (Future<String> visit : visits) {
        visit.get();
      }
    }

    assertThat(service.stats(link.getCode()).visitCount()).isEqualTo(VISITS);
  }
}
