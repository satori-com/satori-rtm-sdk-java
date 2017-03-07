package com.satori.rtm.transport;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;
import java.io.IOException;
import java.net.URI;

/**
 * The factory for a WebSocket transport.
 */
public class WebSocketTransportFactory implements TransportFactory {
  private final WebSocketFactory mWebSocketFactory;
  private final boolean mIsControlCongestion;

  public WebSocketTransportFactory(Integer connectionTimeout) {
    this(connectionTimeout, true);
  }

  public WebSocketTransportFactory(Integer connectionTimeout, boolean isControlCongestion) {
    WebSocketFactory factory = new WebSocketFactory();
    factory.setConnectionTimeout(connectionTimeout);
    mWebSocketFactory = factory;
    mIsControlCongestion = isControlCongestion;
  }

  @Override
  public Transport create(URI uri) throws IOException {
    WebSocket webSocket = mWebSocketFactory.createSocket(uri);
    return new WebSocketTransport(webSocket, mIsControlCongestion);
  }
}
