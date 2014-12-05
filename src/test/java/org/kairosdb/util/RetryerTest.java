package org.kairosdb.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public final class RetryerTest {

  @Test
  public void test_no_retry() throws Exception {
    final AtomicInteger called = new AtomicInteger(0);

    final Retryer retryer = new Retryer.Builder().maxRetries(5).build();
    final String value = retryer.retry(new Retriable<String>() {
      @Override
      public String execute() {
        called.incrementAndGet();
        return "foo";
      }
    });

    assertEquals(called.get(), 1);
    assertEquals(value, "foo");
  }

  @Test
  public void test_retry() throws Exception {
    final AtomicInteger called = new AtomicInteger(0);

    final Retryer retryer = new Retryer.Builder().maxRetries(5).build();
    final String value = retryer.retry(new Retriable<String>() {
      @Override
      public String execute() {
        final int attempt = called.incrementAndGet();
        if (attempt == 2) {
          return "foo";
        }
        throw new RuntimeException("failed");
      }
    });
    assertEquals(called.get(), 2);
    assertEquals(value, "foo");
  }

  @Test
  public void test_timeout() {
    try {
      final Retryer retryer = new Retryer.Builder().maxRetries(1).build();
      retryer.retry(new Retriable<String>() {
        @Override
        public String execute() {
          throw new RuntimeException("failed");
        }
      });
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof RuntimeException);
      assertEquals(e.getMessage(), "failed");
    }
  }
}
