package com.interviewprep.url;

import static org.assertj.core.api.Assertions.assertThat;

import com.interviewprep.common.error.ApiException;
import com.interviewprep.url.dto.ShortenRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

@SpringBootTest
class ShortenConcurrencyTest {

  private static final int REQUESTS = 20;

  @Autowired private ShortLinkService service;

  @Autowired private ShortLinkRepository repository;

  @BeforeEach
  void clean() {
    repository.deleteAll();
  }

  @Test
  void simultaneousShortensOfTheSameUrlShareOneLink() throws Exception {
    ShortenRequest request = new ShortenRequest("https://example.com/same", null, null);

    List<Object> outcomes = runAtOnce(() -> service.shorten(request).link().getCode());

    assertThat(outcomes).doesNotHaveAnyElementsOfTypes(Throwable.class);
    assertThat(outcomes.stream().distinct()).hasSize(1);
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void simultaneousClaimsOfOneCustomCodeGiveOneWinnerAndConflicts() throws Exception {
    ShortenRequest request = new ShortenRequest("https://example.com/mine", null, "mine");

    List<Object> outcomes = runAtOnce(() -> service.shorten(request).link().getCode());

    assertThat(outcomes).filteredOn("mine"::equals).hasSize(1);
    assertThat(outcomes)
        .filteredOn(ApiException.class::isInstance)
        .hasSize(REQUESTS - 1)
        .allMatch(error -> ((ApiException) error).getStatus() == HttpStatus.CONFLICT);
  }

  private List<Object> runAtOnce(Callable<String> call) throws InterruptedException {
    CountDownLatch start = new CountDownLatch(1);
    List<Future<String>> futures = new ArrayList<>();
    List<Object> outcomes = new ArrayList<>();
    try (ExecutorService pool = Executors.newFixedThreadPool(REQUESTS)) {
      for (int i = 0; i < REQUESTS; i++) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return call.call();
                }));
      }
      start.countDown();
      for (Future<String> future : futures) {
        try {
          outcomes.add(future.get());
        } catch (ExecutionException ex) {
          outcomes.add(ex.getCause());
        }
      }
    }
    return outcomes;
  }
}
