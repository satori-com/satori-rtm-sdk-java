package com.satori.rtm.transport;

import com.neovisionaries.ws.client.ProxySettings;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;
import java.io.IOException;
import java.net.URI;

/**
 * The factory for a WebSocket transport.
 */
public class WebSocketTransportFactory extends AbstractTransportFactory {
  private WebSocketFactory mWebSocketFactory;

  public WebSocketTransportFactory() { }

  @Deprecated
  public WebSocketTransportFactory(Integer connectionTimeout) {
    setConnectionTimeoutMillis(connectionTimeout);
  }

  @Deprecated
  public WebSocketTransportFactory(Integer connectionTimeout, boolean isControlCongestion) {
    setConnectionTimeoutMillis(connectionTimeout);
  }

  @Override
  public WebSocketTransport create(URI uri) throws IOException {
    WebSocketFactory factory;
    if (mWebSocketFactory == null) {
      factory = buildUnderlyingWebSocketFactory();
    } else {
      // Backward compatibility with users who used getNVWebSocketFactory method to configure
      // Neo Vision WebSocket library directly
      factory = mWebSocketFactory;
    }
    WebSocket webSocket = factory.createSocket(uri);
    return new WebSocketTransport(webSocket, true);
  }

  WebSocketFactory buildUnderlyingWebSocketFactory() {
    WebSocketFactory factory = new WebSocketFactory();
    factory.setConnectionTimeout(mConnectionTimeout);
    ProxySettings proxySettings = factory.getProxySettings();
    proxySettings.setServer(mProxyUri);
    return factory;
  }

  /**
   * Returns underlying WebSocket factory.
   *
   * @return Neo-Vision WebSocket factory
   * @deprecated If you need to reconfigure underlying WebSocket then use
   * {@link WebSocketTransportFactory#buildUnderlyingWebSocketFactory()}
   * method at your own risk. SDK doesn't provide any garanties that underlying WebSocket library
   * will not be changed in the future.
   */
  @Deprecated
  public WebSocketFactory getNVWebSocketFactory() {
    if (null == mWebSocketFactory) {
      mWebSocketFactory = buildUnderlyingWebSocketFactory();
    }
    return mWebSocketFactory;
  }

}
