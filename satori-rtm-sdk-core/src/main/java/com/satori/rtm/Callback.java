package com.satori.rtm;

/**
 * A callback for accepting the responses of the RTM Service
 */
public interface Callback<V> {
  /**
   * Invoked with the positive response is received.
   *
   * @param result The RTM Service response.
   */
  void onResponse(V result);

  /**
   * Invoked when a negative response is received or request is canceled.
   *
   * @param t The reason of failure.
   */
  void onFailure(Throwable t);
}
