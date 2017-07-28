package com.satori.rtm.transport;

/**
 * Listener interface to define application functionality based on the WebSocket transport events.
 */
public interface TransportListener {
  /**
   * Called after a transport connects to RTM.
   */
  void onConnected();

  /**
   * Called after a transport disconnects from RTM.
   */
  void onDisconnected();

  /**
   * Called when a transport receive a text WebSocket frame.
   *
   * @param message A text frame message
   */
  void onMessage(String message);

  /**
   * Called when a transport error occurs when the connection is being established.
   *
   * @param ex An exception that represents the error. Type of exception depends on transport implementation.
   */
  void onTransportError(Exception ex);

  /**
   * Called when a connecting error is occurred.
   *
   * @param ex An exception that represents the error. Type of exception depends on transport implementation.
   */
  void onConnectingError(Exception ex);
}
