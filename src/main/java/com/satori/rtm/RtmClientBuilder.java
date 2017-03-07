package com.satori.rtm;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.satori.rtm.auth.AuthProvider;
import com.satori.rtm.connection.GsonSerializer;
import com.satori.rtm.connection.ConnectionListener;
import com.satori.rtm.connection.Serializer;
import com.satori.rtm.transport.TransportFactory;
import com.satori.rtm.transport.WebSocketTransport;
import com.satori.rtm.transport.WebSocketTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Use the RtmClientBuilder to create an instance of the RtmClient interface and set both the client properties and the
 * listener for client state events.
 * <p><strong>Code Example</strong>
 * <pre>
 * {@code
 * RtmClient client = new RtmClientBuilder(ENDPOINT, APP_KEY)
 *            .setListener(new RtmClientAdapter() {
 *                // overridden methods go here
 *            )}
 *            .build();
 *            ...
 * }
 * </pre>
 * <p>
 * After you create the client, use the client instance to publish messages and subscribe to channels.
 */
public class RtmClientBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(RtmClientBuilder.class);
  private static final int DEFAULT_PENDING_QUEUE_LENGTH = (1 << 10);
  private static final String RTM_VER = "v2";
  private final String mEndpoint;
  private final String mAppKey;
  private int mConnectionTimeout = 60000;
  private long mMinReconnectInterval = 1000;
  private long mMaxReconnectInterval = 120000;
  private int mPendingActionQueueLength = DEFAULT_PENDING_QUEUE_LENGTH;
  private ScheduledExecutorService mScheduledExecutorService;

  private RtmClientListener mUserListener = new RtmClientAdapter() {
  };
  private boolean mIsAutoReconnect = true;

  private TransportFactory mTransportFactory;
  private AuthProvider mAuthProvider;
  private Serializer mJsonSerializer;
  private boolean mShouldDispatchTransport = true;
  private ExecutorService mDispatcher;

  /**
   * Creates a instance of a client builder for specific endpoint and application key.
   * <p>
   * Use the Developer Portal to obtain the appropriate application keys.
   *
   * @param endpoint RTM Service endpoint.
   * @param appKey   Application key.
   */
  public RtmClientBuilder(String endpoint, String appKey) {
    if (Strings.isNullOrEmpty(endpoint)) {
      throw new IllegalArgumentException("endpoint can't be null or empty");
    }

    if (Strings.isNullOrEmpty(appKey)) {
      throw new IllegalArgumentException("appKey can't be null or empty");
    }

    this.mEndpoint = endpoint;
    this.mAppKey = appKey;
  }

  /**
   * Builds an instance of the {@link RtmClient} interface. Use this method after you
   * set the client properties and listener.
   *
   * @return {@link RtmClient}
   */
  public RtmClient build() {
    if (null == mTransportFactory) {
      mTransportFactory = new WebSocketTransportFactory(mConnectionTimeout);
    }

    if (null == mJsonSerializer) {
      mJsonSerializer = new GsonSerializer();
    }

    return new RtmClientImpl(
        createUri(mEndpoint, mAppKey),
        mUserListener,
        mTransportFactory,
        mAuthProvider,
        mDispatcher,
        mScheduledExecutorService,
        mJsonSerializer,
        mShouldDispatchTransport,
        mIsAutoReconnect,
        mMinReconnectInterval,
        mMaxReconnectInterval,
        mPendingActionQueueLength
    );
  }

  /**
   * Sets the connection timeout interval. The timeout interval is the time, in milliseconds, in which the client
   * attempts to make a WebSocket connection to the RTM Service. If the timeout is exceeded before a connection is
   * made, the client throws {@link ConnectionListener#onConnectingError(Exception)}.
   * <p>
   * Setting this property is optional. Default is {@code 60000}.
   *
   * @param connectionTimeout Timeout in milliseconds.
   * @return {@link RtmClientBuilder} instance.
   */
  public RtmClientBuilder setConnectionTimeout(int connectionTimeout) {
    this.mConnectionTimeout = connectionTimeout;
    return this;
  }

  /**
   * Sets the length of pending action queue. When a client is disconnected, the pending action queue temporarily
   * stores messages until the client reconnects.
   *
   * @param pendingActionQueueLength Length of the queue. Default value is {@code 1024}.
   * @return {@link RtmClientBuilder} instance.
   */
  public RtmClientBuilder setPendingActionQueueLength(int pendingActionQueueLength) {
    this.mPendingActionQueueLength = pendingActionQueueLength;
    return this;
  }

  /**
   * Sets the maximum time period, in seconds, to wait between reconnection attempts. The Java SDK uses the following
   * formula:
   * <p>
   * {@code min(minReconnectInterval * (2 ^ (attempt_number), maxReconnectInterval)}
   * <p>
   * to calculate the reconnect interval after the client disconnects from the RTM Service for any reason. The
   * timeout period between each successive connection attempt increases, but never exceeds this value.
   *
   * @param maxReconnectInterval Maximum reconnect time in milliseconds.
   * @return {@link RtmClientBuilder} instance.
   */
  public RtmClientBuilder setMaxReconnectInterval(long maxReconnectInterval) {
    mMaxReconnectInterval = maxReconnectInterval;
    return this;
  }

  /**
   * Sets the minimum time period, in seconds, to wait between reconnection attempts. The Java SDK uses the following
   * formula:
   * <p>
   * {@code min(minReconnectInterval * (2 ^ (attempt_number), maxReconnectInterval)}
   * <p>
   * to calculate the reconnect interval after the client disconnects from the RTM Service for any reason. The
   * timeout period between each successive connection attempt increases, but starts with this value.
   *
   * @param minReconnectInterval Minimum reconnect time in milliseconds.
   * @return {@link RtmClientBuilder} instance.
   */
  public RtmClientBuilder setMinReconnectInterval(long minReconnectInterval) {
    mMinReconnectInterval = minReconnectInterval;
    return this;
  }

  /**
   * Sets an instance of a {@link RtmClientListener} to define application functionality based on the state
   * changes of a client. To create a listener, create an instance of the {@link RtmClientListener} interface and
   * override the RtmClientListener methods to define application functionality based on client state changes.
   *
   * @param listener Instance of RtmClientListener.
   * @return {@link RtmClientBuilder} instance.
   */
  public RtmClientBuilder setListener(RtmClientListener listener) {
    this.mUserListener = checkNotNull(listener);
    return this;
  }

  /**
   * Sets factory for a WebSocket transport.
   * <p>
   * Java does not include a native WebSocket implementation. This method allows you to use a different WebSocket
   * implementation other than the default Java SDK WebSocket transport.
   * <p>
   * By default, the Java SDK uses the {@code nv-websocket-client} WebSocket client implementation.
   *
   * @param transportFactory WebSocket transport factory.
   * @return {@link RtmClientBuilder} instance.
   * @see WebSocketTransport
   * @see WebSocketTransportFactory
   */
  public RtmClientBuilder setTransportFactory(TransportFactory transportFactory) {
    this.mTransportFactory = transportFactory;
    return this;
  }

  /**
   * Sets an implementation of {@link AuthProvider} as the authentication provider for
   * the client.
   * <p>
   * Use this method if you want to authenticate an application user when you establish the connection.
   *
   * @param authProvider Authentication provider.
   * @return {@link RtmClientBuilder} instance.
   */
  public RtmClientBuilder setAuthProvider(AuthProvider authProvider) {
    this.mAuthProvider = authProvider;
    return this;
  }

  /**
   * Sets the automatic reconnect option for the client connection to the RTM Service. Use {@code true} to enable
   * automatic reconnect and {@code false} to disable it.
   * <p>
   * If you enable automatic reconnect, the client attempts to reconnect to the RTM Service whenever the connection
   * to the RTM Service fails.
   *
   * @param isAutoReconnect {@code true} enables automatic reconnect; {@code false} disables it.
   * @return {@link RtmClientBuilder} instance.
   */
  public RtmClientBuilder setAutoReconnect(boolean isAutoReconnect) {
    this.mIsAutoReconnect = isAutoReconnect;
    return this;
  }

  /**
   * Sets the scheduler to run delayed or periodic tasks. If not specified, it will be created automatically.
   *
   * @param service Instance of {@code ScheduledExecutorService}.
   * @return {@link RtmClientBuilder} instance.
   */
  public RtmClientBuilder setScheduler(ScheduledExecutorService service) {
    this.mScheduledExecutorService = service;
    return this;
  }

  /**
   * Sets the external dispatcher. All transport events and user actions are executed in dispatcher. External
   * dispatcher must provide the order of events.
   *
   * @param dispatcher              Dispatcher
   * @param shouldDispatchTransport If false then dispatcher will not be used for transport events (for example if
   *                                they already dispatched).
   * @return {@link RtmClientBuilder} instance.
   */
  public RtmClientBuilder setDispatcher(ExecutorService dispatcher, boolean shouldDispatchTransport) {
    this.mDispatcher = dispatcher;
    this.mShouldDispatchTransport = shouldDispatchTransport;
    return this;
  }

  /**
   * Sets an implementation of {@link Serializer} for the JSON serialization/deserialization of PDUs.
   * <p>
   * By default, SDK uses {@code google-gson} library, but you could use any library for that task.
   *
   * @param serializer Instance of {@link Serializer}
   * @return {@link RtmClientBuilder} instance.
   */
  public RtmClientBuilder setJsonSerializer(Serializer serializer) {
    this.mJsonSerializer = serializer;
    return this;
  }

  protected URI createUri(String endpoint, String appKey) {
    if (Strings.isNullOrEmpty(endpoint)) {
      throw new IllegalArgumentException();
    }
    Pattern verPattern = Pattern.compile("/(v\\d+)$");
    Matcher m = verPattern.matcher(endpoint);

    String fullEndpoint = endpoint;

    if (!m.find()) {
      if (!fullEndpoint.endsWith("/")) {
        fullEndpoint += "/";
      }
      fullEndpoint += RTM_VER;
    } else {
      String ver = m.group(1);
      LOG.warn("Specifying a version as a part of the endpoint is deprecated. " +
          "Please remove the {} from {}", ver, endpoint);
    }

    String uri = String.format("%s?appkey=%s", fullEndpoint, appKey);
    try {
      return new URI(uri);
    } catch (URISyntaxException e) {
      LOG.error("Unable to parse URI {}", uri, e);
      throw new RuntimeException(e);
    }
  }
}
