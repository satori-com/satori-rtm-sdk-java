package com.satori.rtm.transport;

import java.net.URI;

public abstract class AbstractTransportFactory implements TransportFactory {
  int mConnectionTimeout = 60000;
  URI mProxyUri = null;

  /**
   * Sets the http(s) proxy server.
   *
   * @param proxyUri uri of proxy server.
   */
  public void setProxy(URI proxyUri) {
    this.mProxyUri = proxyUri;
  }

  /**
   * Sets the connection timeout in millis.
   *
   * @param mConnectionTimeout timeout
   */
  public void setConnectionTimeoutMillis(int mConnectionTimeout) {
    this.mConnectionTimeout = mConnectionTimeout;
  }
}

