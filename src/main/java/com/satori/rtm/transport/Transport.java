package com.satori.rtm.transport;

import com.neovisionaries.ws.client.WebSocket;

/**
 * General interface for a WebSocket transport.
 */
public interface Transport {
  /**
   * Sends message to the RTM Service.
   *
   * @param data Encoded message.
   * @throws InterruptedException Process interrupted when sending data to the RTM Service.
   * @throws TransportException   Indicates an error occurred when sending data.
   * @see WebSocketTransport#WebSocketTransport(WebSocket, boolean)
   */
  void send(String data) throws InterruptedException, TransportException;

  /**
   * Connects to the server. This method performs the handshake with the RTM Service and sets transport listener.
   *
   * @param listener Transport listener.
   * @throws TransportException Indicates an error occurred when connecting to the RTM Service.
   */
  void connect(TransportListener listener) throws TransportException;

  /**
   * Closes transport connection.
   */
  void close();
}
