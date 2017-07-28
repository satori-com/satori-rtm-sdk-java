package com.satori.rtm;

/**
 * A callback for any asynchronous call that can result in success or failure.
 */
public interface Callback<V> {
  /**
   * Called when an asynchronous call completes successfully.
   *
   * @param result the value returned
   */
  void onResponse(V result);

  /**
   * Called when an asynchronous call fails.
   * <p>
   * If the asynchronous call throws a {@link Exception}, then the thrown object is passed to this
   * method.
   *
   * @param t thrown exception
   */
  void onFailure(Throwable t);
}
