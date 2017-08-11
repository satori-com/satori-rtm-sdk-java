package com.satori.rtm;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.satori.rtm.auth.AuthProvider;
import com.satori.rtm.auth.RoleSecretAuthProvider;
import com.satori.rtm.connection.ConnectionListener;
import com.satori.rtm.connection.Serializer;
import com.satori.rtm.transport.AbstractTransportFactory;
import com.satori.rtm.transport.Transport;
import com.satori.rtm.transport.TransportFactory;
import com.satori.rtm.transport.WebSocketTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An {@link RtmClient} builder class.
 *
 * <p>The following example demonstrates how to use {@code RtmClientBuilder} to get an RTM client:
 *
 * <pre>
 * RtmClient client = new RtmClientBuilder(YOUR_ENDPOINT, YOUR_APPKEY)
 *   // Sets a proxy server for the connection to RTM
 *   .setProxy(URI.create("http://127.0.0.1:3128"))
 *   // Sets a listener for RTM lifecycle events
 *   .setListener(new RtmClientAdapter() {
 *     // When the client successfully connects to RTM
 *     &#64;Override
 *     public void onEnterConnected(RtmClient client) {
 *       System.out.println("Connected to Satori RTM!");
 *     }
 *   })
 *   // Builds the client instance
 *   .build();
 * </pre>
 * <p>
 * NOTES:
 * <ul>
 *   <li> The order in which you call the "set" methods doesn't matter</li>
 *   <li>
 *     Get the values for {@code YOUR_ENDPOINT} and {@code YOUR_APPKEY} from your Satori project page, which you
 *     access in the Dev Portal.
 *   </li>
 * </ul>
 */
public class RtmClientBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(RtmClientBuilder.class);
  private static final int DEFAULT_PENDING_QUEUE_LENGTH = (1 << 10);
  private static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
  private static final int DEFAULT_MIN_RECONNECT_INTERVAL = 1000;
  private static final int DEFAULT_MAX_RECONNECT_INTERVAL = 120000;
  private static final String RTM_VER = "v2";

  private final String mEndpoint;
  private final String mAppKey;
  long mMinReconnectInterval = DEFAULT_MIN_RECONNECT_INTERVAL;
  long mMaxReconnectInterval = DEFAULT_MAX_RECONNECT_INTERVAL;
  int mPendingActionQueueLength = DEFAULT_PENDING_QUEUE_LENGTH;
  ScheduledExecutorService mScheduledExecutorService;
  RtmClientListener mUserListener = new RtmClientAdapter() {};
  boolean mIsAutoReconnect = true;
  AbstractTransportFactory mTransportFactory;
  AuthProvider mAuthProvider;
  Serializer mJsonSerializer;
  boolean mShouldDispatchTransport = true;
  ExecutorService mDispatcher;
  private int mConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
  private URI mProxyUri;

  /**
   * Constructs a new client builder with the endpoint and the appkey.
   * <p>
   * Use {@code RtmClientBuilder} to build an {@link RtmClient} with various configuration
   * settings. {@code RtmClientBuilder} uses the builder pattern. First, call configuration methods to set desired
   * options, and then call {@link #build()}.
   * <p>
   *   Get the values for {@code endpoint} and {@code appKey} from your Satori project page, which you
   *   access in the Dev Portal.
   *
   * @param endpoint endpoint from your project page in Dev Portal
   * @param appKey appkey from your project page in Dev Portal
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

  private static Set<URL> findPossibleBinderPathSet(String path) {
    Set<URL> staticPathSet = new LinkedHashSet<URL>();
    try {
      ClassLoader classLoader = RtmClientBuilder.class.getClassLoader();
      Enumeration<URL> paths;
      if (classLoader == null) {
        paths = ClassLoader.getSystemResources(path);
      } else {
        paths = classLoader.getResources(path);
      }
      while (paths.hasMoreElements()) {
        staticPathSet.add(paths.nextElement());
      }
    } catch (IOException ioe) {
      LOG.error("Error getting resources from path", ioe);
    }
    return staticPathSet;
  }

  private static Class<?> dynamicClassLoad(String className) {
    String path = className.replace('.', '/') + ".class";
    try {
      Set<URL> pathSet = findPossibleBinderPathSet(path);
      if (pathSet.size() > 1) {
        LOG.error("Class path contains multiple class files for " + className);
        for (URL url : pathSet) {
          LOG.error("Found binding in [" + url + "]");
        }
      }
      ClassLoader classLoader = RtmClientBuilder.class.getClassLoader();
      return classLoader.loadClass(className);
    } catch (Exception e) {
      LOG.error("Failed to load class: " + className, e);
      throw new IllegalStateException("Unexpected initialization failure", e);
    }
  }

  static Serializer createSerializer() {
    final String jsonBinderClassName = "com.satori.rtm.connection.StaticJsonBinder";
    final String methodName = "createSerializer";

    Class<?> clazz = dynamicClassLoad(jsonBinderClassName);
    try {
      Method method = clazz.getMethod(methodName);
      return (Serializer) method.invoke(null);
    } catch (Exception e) {
      LOG.error("Failed to invoke method: " + methodName, e);
      throw new IllegalStateException("Unexpected initialization failure", e);
    }
  }

  /**
   * Builds an {@link RtmClient} with the configured properties.
   * <p>
   * Call this method <strong>after</strong> you've set properties.
   *
   * @return RTM client
   */
  public RtmClient build() {
    if (null == mTransportFactory) {
      mTransportFactory = new WebSocketTransportFactory();
    }
    mTransportFactory.setConnectionTimeoutMillis(mConnectionTimeout);
    mTransportFactory.setProxy(mProxyUri);

    if (null == mJsonSerializer) {
      mJsonSerializer = createSerializer();
    }

    return new RtmClientImpl(
        createUri(mEndpoint, mAppKey),
        this
    );
  }

  /**
   * Sets a socket connection timeout.
   * <p>
   * If the client fails to connect to the RTM with the specified timeout, the RTM SDK passes an
   * exception to the {@link ConnectionListener#onConnectingError} callback.
   * <p>
   * The default interval is {@value #DEFAULT_CONNECTION_TIMEOUT}, in
   * milliseconds. Zero timeout is interpreted as an infinite timeout.
   *
   * @param connectionTimeout interval, in milliseconds, to wait for a connection
   * @return the current builder object
   * @see RtmClientBuilder#setListener(RtmClientListener)
   */
  public RtmClientBuilder setConnectionTimeout(int connectionTimeout) {
    this.mConnectionTimeout = connectionTimeout;
    return this;
  }

  /**
   * Sets a proxy server for the RTM client.
   * <p>
   * If the URI contains a {@code scheme} part and its value is
   * {@code "https"} (case-insensitive) then TLS is enabled.
   * <p>
   * If the URI contains a {@code userinfo} part then it's used for authentication.
   *
   * @param proxyUri proxy server identifier
   * @return the current builder object
   */
  public RtmClientBuilder setProxy(URI proxyUri) {
    this.mProxyUri = proxyUri;
    return this;
  }

  /**
   * Sets the length of offline queue.
   * <p>
   * The offline queue is used to temporarily store the user's actions when a connection to RTM isn't
   * established. These actions are performed when client reconnects to RTM.
   * <p>
   * A length of zero disables the offline queue. The default value is
   * {@value RtmClientBuilder#DEFAULT_PENDING_QUEUE_LENGTH}.
   *
   * @param pendingActionQueueLength length of the offline queue
   * @return the current builder object
   */
  public RtmClientBuilder setPendingActionQueueLength(int pendingActionQueueLength) {
    this.mPendingActionQueueLength = pendingActionQueueLength;
    return this;
  }

  /**
   * Sets the maximum time to wait between reconnection attempts.
   * <p>
   * The Satori RTM SDK uses the following formula to calculate the interval between reconnection
   * attempts:
   * <p>
   * {@code min(maxReconnectInterval, jitter + minReconnectInterval * (2 ^ (attemptNumber))}
   * <p>
   * This value steadily increases between attempts until it reaches the maximum time.
   * <p>
   * The waiting period applies whenever the client reconnects, regardless of why it disconnected.
   *
   * @param maxReconnectInterval maximum waiting time, in milliseconds. The default is {@value #DEFAULT_MAX_RECONNECT_INTERVAL}
   * @return the current builder object
   */
  public RtmClientBuilder setMaxReconnectInterval(long maxReconnectInterval) {
    mMaxReconnectInterval = maxReconnectInterval;
    return this;
  }

  /**
   * Sets the minimum time to wait between reconnection attempts.
   * <p>
   * The Satori RTM SDK uses the following formula to calculate the interval between reconnection
   * attempts:
   * <p>
   * {@code min(maxReconnectInterval, jitter + minReconnectInterval * (2 ^ (attemptNumber))}
   * <p>
   * This value steadily increases between attempts until it reaches the maximum time.
   * <p>
   * The waiting period applies whenever the client reconnects, regardless of why it disconnected.
   *
   * @param minReconnectInterval minimum waiting period, in milliseconds. The default is {@value #DEFAULT_MIN_RECONNECT_INTERVAL}
   * @return the current builder object
   */
  public RtmClientBuilder setMinReconnectInterval(long minReconnectInterval) {
    mMinReconnectInterval = minReconnectInterval;
    return this;
  }

  /**
   * Sets an client listener that has callback methods to handle client state changes, connection
   * events and error conditions.
   *
   * @param listener event listener
   * @return the current builder object
   */
  public RtmClientBuilder setListener(RtmClientListener listener) {
    this.mUserListener = checkNotNull(listener);
    return this;
  }

  /**
   * Sets the WebSocket transport used by the client.
   * <p>
   * By default, the RTM SDK uses the {@code nv-websocket-client} WebSocket client implementation.
   * {@code setTransportFactory} lets you use a different WebSocket implementation.
   *
   * @param transportFactory WebSocket transport factory.
   * @return {@link RtmClientBuilder} instance.
   * @deprecated use {@link RtmClientBuilder#setTransportFactory(AbstractTransportFactory)}
   */
  @Deprecated
  public RtmClientBuilder setTransportFactory(final TransportFactory transportFactory) {
    return setTransportFactory(new AbstractTransportFactory() {
      @Override
      public Transport create(URI uri) throws IOException {
        return transportFactory.create(uri);
      }
    });
  }

  /**
   * Sets the WebSocket transport factory used by the client.
   * <p>
   * By default, the RTM SDK uses the {@code nv-websocket-client} WebSocket client implementation.
   * {@code setTransportFactory} lets you use a different WebSocket implementation.
   *
   * @param transportFactory WebSocket transport factory.
   * @return the current builder object
   */
  public RtmClientBuilder setTransportFactory(AbstractTransportFactory transportFactory) {
    this.mTransportFactory = transportFactory;
    return this;
  }

  /**
   * Sets an authenticator for the client.
   * <p>
   * The RTM SDK automatically authenticates the RTM client immediately after a connection to RTM is
   * established.
   * <p>
   * The RTM SDK includes {@link RoleSecretAuthProvider}, which offers role-based authentication. Use
   * role-based authentication to request specific permissions for the RTM client. To manage roles and
   * channel permissions, go to your project in the Dev Portal.
   *
   * @param authProvider authentication provider
   * @return the current builder object
   */
  public RtmClientBuilder setAuthProvider(AuthProvider authProvider) {
    this.mAuthProvider = authProvider;
    return this;
  }

  /**
   * Sets the automatic reconnection flag for the client.
   * <p>
   * By default, the client automatically tries to reconnect to RTM if the connection fails.
   *
   * @param isAutoReconnect set to {@code true} to enable automatic reconnect, or {@code false} to disable it
   * @return the current builder object
   */
  public RtmClientBuilder setAutoReconnect(boolean isAutoReconnect) {
    this.mIsAutoReconnect = isAutoReconnect;
    return this;
  }

  /**
   * Sets a scheduler to run delayed or periodic tasks. If not specified, the RTM SDK uses a
   * single-threaded executor service.
   * <p>
   * This method is intended for advanced users who want control over client task execution.
   *
   * @param service task scheduler
   * @return the current builder object
   */
  public RtmClientBuilder setScheduler(ScheduledExecutorService service) {
    this.mScheduledExecutorService = service;
    return this;
  }

  /**
   * Sets an event dispatcher that executes user actions and transport events. If not specified,
   * the RTM SDK provides a dispatcher that executes events sequentially in first-in first-out
   * order.
   * <p>
   * This method is intended for advanced users who want control over client task execution.
   *
   * @param dispatcher event dispatcher
   * @param shouldDispatchTransport if {@code true}, the dispatcher executes transport events,
   *                                otherwise a dispatcher isn't used for transport events
   * @return the current builder object
   */
  public RtmClientBuilder setDispatcher(ExecutorService dispatcher,
                                        boolean shouldDispatchTransport) {
    this.mDispatcher = dispatcher;
    this.mShouldDispatchTransport = shouldDispatchTransport;
    return this;
  }

  /**
   * Sets the JSON serializer for the client.
   * <p>
   * The RTM SDK supports two JSON libraries: {@code google-gson} and {@code Jackson2}.
   * See the Java RTM SDK GitHub repository documentation for the
   * <a href="https://github.com/satori-com/satori-rtm-sdk-java#json-library">JSON Library</a>
   * for more details.
   *
   * @param serializer JSON serializer
   * @return the current builder object
   */
  public RtmClientBuilder setJsonSerializer(Serializer serializer) {
    this.mJsonSerializer = serializer;
    return this;
  }

  URI createUri(String endpoint, String appKey) {
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
