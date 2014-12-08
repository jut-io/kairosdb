package org.kairosdb.util;

/**
 *  
 */
public interface Retriable<T> {
  T execute();
}
