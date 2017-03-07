package com.satori.rtm.transport;

import java.io.IOException;
import java.net.URI;

/**
 * A factory to create the WebSocket transport.
 */
public interface TransportFactory {
  /**
   * Creates the transport for given {@code URI}.
   * <p>
   * Note that the case-insensitive scheme part of the URI must be one of {@code ws}, {@code wss}, {@code http} or
   * {@code https}
   *
   * @param uri The URI of the WebSocket endpoint on the server side.
   * @return A transport for given {@code URI}.
   * @throws IOException Failed to create a socket. Or, HTTP proxy handshake or SSL
   *                     handshake failed.
   */
  Transport create(URI uri) throws IOException;
}
