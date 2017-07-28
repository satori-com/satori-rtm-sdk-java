package com.satori.rtm.transport;

import com.neovisionaries.ws.client.WebSocket;

/**
 * General interface for a WebSocket transport.
 */
public interface Transport {
  /**
   * Sends data to RTM.
   *
   * @param data Encoded message.
   * @throws InterruptedException Process interrupted when sending data to RTM.
   * @throws TransportException   Indicates an error occurred when sending data.
   * @see WebSocketTransport#WebSocketTransport(WebSocket, boolean)
   */
  void send(String data) throws InterruptedException, TransportException;

  /**
   * Connects to the server. This method performs the handshake with RTM and sets transport listener.
   *
   * @param listener Transport listener.
   * @throws TransportException Indicates an error occurred when connecting to RTM.
   */
  void connect(TransportListener listener) throws TransportException;

  /**
   * Closes transport connection.
   */
  void close();
}
