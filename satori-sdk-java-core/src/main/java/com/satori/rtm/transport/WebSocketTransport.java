package com.satori.rtm.transport;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the Java SDK WebSocket transport based on the nv-websocket-client library.
 *
 * @see <a href="https://github.com/TakahikoKawasaki/nv-websocket-client">nv-websocket-client</a>
 */
public class WebSocketTransport implements Transport {
  private final static Logger LOG = LoggerFactory.getLogger(WebSocketTransport.class);
  private final static Integer SENDER_QUEUE_CAPACITY = 1024;

  private final static Integer DEFAULT_PING_INTERVAL = 60000;
  private final static Integer MIN_PING_INTERVAL = 1000;
  private final static Integer SO_TIMEOUT_MS = 60 * 5 * 1000;

  private final WebSocket mWebSocket;
  protected TransportListener mTransportListener;

  private long mPongTimestamp = 0;
  private long mPingTimestamp = 0;
  private long mRecvTimestamp = 0;

  public WebSocketTransport(WebSocket webSocket, boolean enableCongestionControl) {
    mWebSocket = webSocket;
    if (enableCongestionControl) {
      mWebSocket.setFrameQueueSize(SENDER_QUEUE_CAPACITY);
    }
    setPingInterval(DEFAULT_PING_INTERVAL);
  }

  public void setPingInterval(long interval) {
    long normalizedInterval = Math.max(interval, MIN_PING_INTERVAL);
    mWebSocket.setPingInterval(normalizedInterval);
  }

  @Override
  public void connect(TransportListener listener) throws TransportException {
    try {
      mTransportListener = listener;
      mWebSocket.addListener(convertWebSocketListener(listener));
      mWebSocket.getSocket().setSoTimeout(SO_TIMEOUT_MS);
      mWebSocket.connect();
    } catch (SocketException e) {
      throw new TransportException(e);
    } catch (WebSocketException e) {
      throw new TransportException(e);
    }
  }

  @Override
  public void send(String data) throws InterruptedException, TransportException {
    mWebSocket.sendText(data);
  }

  @Override
  public void close() {
    mWebSocket.disconnect();
  }

  public WebSocket getNVWebSocket() {
    return mWebSocket;
  }

  private WebSocketAdapter convertWebSocketListener(final TransportListener listener) {
    return new WebSocketAdapter() {
      @Override
      public void onTextMessage(WebSocket websocket, String text) throws Exception {
        mRecvTimestamp = System.currentTimeMillis();
        if (null != listener) {
          listener.onMessage(text);
        }
      }

      @Override
      public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        mPongTimestamp = System.currentTimeMillis();
      }

      @Override
      public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        if (frame.isPingFrame()) {
          long prevPingTimestamp = mPingTimestamp;
          if (mPongTimestamp < prevPingTimestamp) {
            long pingInterval = websocket.getPingInterval();
            if (mRecvTimestamp < prevPingTimestamp) {
              LOG.error("No messages and no WS PING responses received for time {} ms.",
                  pingInterval);
              forceCloseUnderlyingSocket();
            } else {
              LOG.warn("WS Pong message not received. You may be processing data too slow to" +
                  " receive WS Pong within {} ms.", pingInterval);
            }
          }
          mPingTimestamp = System.currentTimeMillis();
        }
      }

      @Override
      public void onConnected(WebSocket websocket, Map<String, List<String>> headers)
          throws Exception {
        if (null != listener) {
          listener.onConnected();
        }
      }

      @Override
      public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame,
                                 WebSocketFrame clientCloseFrame, boolean closedByServer)
          throws Exception {
        if (null != listener) {
          listener.onDisconnected();
        }
      }

      @Override
      public void onConnectError(WebSocket websocket, WebSocketException exception)
          throws Exception {
        if (null != listener) {
          listener.onConnectingError(exception);
        }
      }

      @Override
      public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame)
          throws Exception {
        if (null != listener) {
          listener.onTransportError(cause);
        }
      }

      @Override
      public void onMessageError(WebSocket websocket, WebSocketException cause,
                                 List<WebSocketFrame> frames) throws Exception {
        if (null != listener) {
          listener.onTransportError(cause);
        }
      }

      @Override
      public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
        if (null != listener) {
          listener.onTransportError(cause);
        }
      }

      @Override
      public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data)
          throws Exception {
        if (null != listener) {
          listener.onTransportError(cause);
        }
      }

      @Override
      public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame)
          throws Exception {
        if (null != listener) {
          listener.onTransportError(cause);
        }
      }

      @Override
      public void onUnexpectedError(WebSocket websocket, WebSocketException cause)
          throws Exception {
        if (null != listener) {
          listener.onTransportError(cause);
        }
      }
    };
  }

  private void forceCloseUnderlyingSocket() {
    mWebSocket.disconnect();
    // WebSocket library tries to unblock socket I/O by calling Thread.interrupt
    // it has platform specific behaviour. Call the socket's close method to ensure
    // that all I/O calls are interrupted
    try {
      mWebSocket.getSocket().close();
    } catch (IOException e) {
      // ignore it
    }
  }
}

