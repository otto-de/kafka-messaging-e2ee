package de.otto.messaging.kafka.e2ee;

import static org.assertj.core.api.Assertions.assertThat;

import de.otto.messaging.kafka.e2ee.helper.TestClock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class CacheTest {

  @Test
  void shouldRetrieveValueFromCacheOnTheSecondCall() {
    // given: a cache
    TestClock clock = new TestClock("2023-08-15T15:00Z");
    Cache<String, String> cache = new Cache<>(Duration.ofHours(1), clock);
    // given: a CacheProvider
    CacheProvider cacheProvider = new CacheProvider();
    // when: cache is called for the first time
    String result1 = cache.getOrRetrieve("keyValue", cacheProvider);
    assertThat(result1).describedAs("1st call result").isEqualTo("call-1");
    // when: cache is called for the second time (cached value is used)
    clock.setCurrentTime("2023-08-15T15:50Z");
    String result2 = cache.getOrRetrieve("keyValue", cacheProvider);
    assertThat(result2).describedAs("2nd call result").isEqualTo("call-1");
    // when: cache is called for the third time (cached value is expired)
    clock.setCurrentTime("2023-08-15T16:05Z");
    String result3 = cache.getOrRetrieve("keyValue", cacheProvider);
    assertThat(result3).describedAs("3rd call result").isEqualTo("call-2");
  }

  private static class CacheProvider implements Function<String, String> {

    private final AtomicInteger cnt = new AtomicInteger(0);

    @Override
    public String apply(String s) {
      return "call-" + cnt.incrementAndGet();
    }
  }
}