package org.kairosdb.util;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public final class Retryer {

  private static final Logger log = LoggerFactory.getLogger(Retryer.class);

  public static class Builder {
    private Integer maxRetries;
    private Integer delay;

    /**
     * The maximum number of times to retry
     */
    public Builder maxRetries(final int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    /**
     * The initial time to wait between attempts, in seconds
     */
    public Builder delay(final int delay) {
      this.delay = delay;
      return this;
    }

    public Retryer build() {
      if (delay == null) {
        delay = 1;
      }

      if (maxRetries == null) {
        maxRetries = 10;
      }
      return new Retryer(this);
    }
  }

  private final int delay;
  private final int maxRetries;

  private Retryer(final Builder builder) {
    this.delay = builder.delay;
    this.maxRetries = builder.maxRetries;
  }

  /**
   * Attempt to run the given {@link Retriable} n times. If maxRetries has been reached, the
   * original exception will be thrown if an instance of {@link Exception}, otherwise it will be
   * wrapped in an {@link Exception}
   * 
   * @param retriable is the {@link Retriable} to execute
   * @param <T>
   * @return the {@link Retriable} result
   * @throws Exception if the max number of retries has been reached
   */
  public <T> T retry(final Retriable<T> retriable) throws Exception {
    T result;
    int retries = 0;
    long sleepSec = delay;
    while (true) {
      try {
        result = retriable.execute();
        break;
      } catch (Throwable e) {
        if (retries == maxRetries) {
          log.warn("max retries ({}) reached", maxRetries);
          Throwables.propagateIfPossible(e, Exception.class);
          throw new Exception(e);
        }
        retries++;
        log.warn("operation failed with message: '{}', retrying in {} seconds - retry #{}",
            e.getMessage(), sleepSec, retries);
        sleepUninterruptibly(sleepSec, TimeUnit.SECONDS);
        sleepSec++;
      }
    }
    return result;
  }

}
