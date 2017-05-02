package com.satori.rtm.connection;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.satori.rtm.Callback;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduException;
import com.satori.rtm.model.PduRaw;
import com.satori.rtm.transport.Transport;
import com.satori.rtm.transport.TransportException;
import com.satori.rtm.transport.TransportFactory;
import com.satori.rtm.transport.TransportListener;
import com.satori.rtm.utils.DispatcherProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Access the RTM Service on the connection level to connect to the RTM Service, send and receive PDUs, and wait
 * for responses from the RTM Service.
 * <p>
 * Create a {@code Connection} with the {@link Connection#create(URI, TransportFactory, Serializer)} method.
 * <p>
 * <strong>Code Example</strong>
 * <pre>
 * {@code
 * Connection connection = Connection.create(new URI("<ENDPOINT>?appkey=<APP_KEY>"),
 *                                           new WebSocketTransportFactory());
 * connection.connect(new ConnectionListener() { ... });
 * }
 * </pre>
 */
public class Connection {
  private final static Logger LOG = LoggerFactory.getLogger(Connection.class);
  private final ConcurrentMap<String, ResponseWaiter> mResponseWaiters;
  private final AtomicInteger mIdCounter;
  private final Transport mTransport;
  private final Serializer mSerializer;
  private volatile boolean isDisposed = false;
  private ConnectionListener mUserListener;

  private Connection(Transport transport, Serializer serializer) {
    checkNotNull(transport);
    this.mTransport = transport;
    this.mIdCounter = new AtomicInteger(0);
    this.mSerializer = serializer;
    this.mResponseWaiters = new ConcurrentHashMap<String, ResponseWaiter>();
  }

  /**
   * Creates a {@link Connection} object for a specific RTM Service endpoint {@code URI}.
   *
   * @param uri              RTM Service endpoint.
   * @param transportFactory Transport factory.
   * @param serializer       Json serializer/deserializer.
   * @return {@link Connection}.
   * @throws TransportException Could not create transport for the {@code URI}.
   */
  public static Connection create(
      URI uri,
      TransportFactory transportFactory,
      Serializer serializer) throws TransportException {
    checkNotNull(uri);
    checkNotNull(transportFactory);
    checkNotNull(serializer);

    Transport webSocket;
    try {
      webSocket = transportFactory.create(uri);
    } catch (IOException ex) {
      throw new TransportException(ex);
    }

    return new Connection(webSocket, serializer);
  }

  /**
   * Connects to the RTM Service, using the connection created with and performs transport-specific connect
   * operations.
   * <p>
   * For example, the method performs the WebSocket handshake and establishes the WebSocket connection.
   *
   * @param listener   Implementation of the {@link ConnectionListener}.
   * @param dispatcher Construct that waits for and dispatches events from transport. Could be null. In this case all
   *                   events will be fired from transport thread.
   * @throws TransportException Transport cannot connect.
   */
  public void connect(ConnectionListener listener, ExecutorService dispatcher)
      throws TransportException {
    mUserListener = listener;
    TransportListener transListener = new InnerTransportListener(listener);
    if (null != dispatcher) {
      transListener = DispatcherProxy.wrap(transListener, dispatcher);
    }
    mTransport.connect(transListener);
  }

  /**
   * Returns the {@link ConnectionListener} for the current {@link Connection} object.
   *
   * @return {@link ConnectionListener}.
   */
  public ConnectionListener getListener() {
    return mUserListener;
  }

  /**
   * Asynchronously sends a Protocol Data Unit (PDU) to the RTM Service without acknowledge from
   * the RTM Service.
   * <p>
   * This method combines the specified
   * operation with the PDU body into a PDU and sends it to the RTM Service. The PDU body must be able to be
   * serialized into a JSON object.
   * <p>
   * The <a href="http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/ListenableFuture.html">ListenableFuture</a>
   * returned by this method is complete when the request is sent to the RTM Service.
   *
   * @param operation PDU operation, for example, {@code "rtm/publish"}.
   * @param body      PDU body.
   * @return Result of an asynchronous send operation.
   */
  public ListenableFuture<Void> sendNoAck(String operation, Object body) {
    ListenableFuture<PduRaw> raw = send(new Pdu<Object>(operation, body, null));
    // transform typed response to Void
    return Futures.transform(raw, new Function<PduRaw, Void>() {
      @Override
      public Void apply(PduRaw input) {
        return null;
      }
    });
  }

  /**
   * Asynchronously sends a Protocol Data Unit (PDU) to the RTM Service.
   * <p>
   * This method combines the specified operation with the PDU body into a PDU and
   * sends it to the RTM Service. The PDU body must be able to be serialized into a JSON object.
   * <p>
   * With this method, use the {@code responseClazz} parameter to specify the format of the PDU body returned
   * by the RTM Service. The body of the PDU is converted to the format {@code responseClazz} parameter before
   * it is returned.
   * <p>
   * The <a href="http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/ListenableFuture.html">ListenableFuture</a>
   * returned by this method is complete when the PDU response is received from the RTM Service.
   * <p>
   * The {@code ListenableFuture} can fail due to the following error:
   * <ul>
   * <li>{@link PduException} - The RTM Service returns a negative response.</li>
   * </ul>
   *
   * @param operation     PDU operation, for example, {@code "rtm/publish"}.
   * @param body          PDU body.
   * @param responseClazz Instance of the response class.
   * @param <T>           Type of response class.
   * @return Result of an asynchronous send operation.
   */
  public <T> ListenableFuture<Pdu<T>> send(String operation, Object body,
                                           final Class<T> responseClazz) {
    ListenableFuture<PduRaw> untypedResponse = send(new Pdu<Object>(operation, body, generateId()));
    return Futures.transform(untypedResponse, new Function<PduRaw, Pdu<T>>() {
      @Override
      public Pdu<T> apply(PduRaw input) {
        if (null == input) {
          return null;
        }
        return input.convertBodyTo(responseClazz);
      }
    });
  }

  /**
   * Asynchronously sends a Protocol Data Unit (PDU) to the RTM Service. The typed response from
   * the RTM Service is passed to the callback.
   * <p>
   * This method combines the specified operation with the PDU body into a PDU and
   * sends it to the RTM Service. The PDU body must be able to be serialized into a JSON object.
   * <p>
   * With this method, use the {@code responseClazz} parameter to specify the format of the PDU
   * body returned by the RTM Service. The body of the PDU is converted to the format
   * {@code responseClazz} parameter before it is returned.
   * <p>
   * This method should be used when RTM sends multiple PDUs response. All incoming PDUs from the
   * RTM Service will be passed to {@link Callback#onResponse(Object)}.
   *
   * @param operation     PDU operation, for example, {@code "rtm/publish"}.
   * @param body          PDU body.
   * @param responseClazz Instance of the response class.
   * @param callback      The callback to invoke when response is received.
   * @param <T>           Type of response class.
   */
  public <T> void sendWithCallback(String operation, Object body, final Class<T> responseClazz,
                                   final Callback<Pdu<T>> callback) {
    Pdu<Object> pdu = new Pdu<Object>(operation, body, generateId());
    sendWithCallback(pdu, new Callback<PduRaw>() {
      @Override
      public void onResponse(PduRaw result) {
        Pdu<T> typed = (null != result) ? result.convertBodyTo(responseClazz) : null;
        callback.onResponse(typed);
      }

      @Override
      public void onFailure(Throwable t) {
        callback.onFailure(t);
      }
    });
  }

  /**
   * Stops a specific connection and releases all allocated resources. All communication with the RTM Service stops
   * when you call this method and the events are not propagated to any listeners.
   * <p>
   * In general, use this method after the connection to the RTM Service is already closed and you need to clean up
   * resources.
   */
  public void dispose() {
    this.isDisposed = true;
    close();
  }

  /**
   * Closes a specific connection. The {@link ConnectionListener#onDisconnected()} event is propagated to all
   * listeners.
   */
  public void close() {
    this.mTransport.close();
    for (Map.Entry<String, ResponseWaiter> pair : mResponseWaiters.entrySet()) {
      pair.getValue().dispose();
    }
    mResponseWaiters.clear();
  }

  private void processIncomingPDU(String json) {
    try {
      checkNotNull(json);
      LOG.debug("[recv] " + json);
      PduRaw pdu = mSerializer.parsePdu(json);
      checkNotNull(pdu);

      if (pdu.isUnsolicited()) {
        mUserListener.onUnsolicitedPDU(pdu);
        return;
      }

      String pduId = pdu.getId();

      ResponseWaiter waiter = mResponseWaiters.get(pduId);

      if (null == waiter) {
        mUserListener.onError(new PduException("Unexpected PDU received", pdu));
        return;
      }

      boolean isChunkResponse = pdu.getAction().endsWith("/data");
      if (!isChunkResponse) {
        mResponseWaiters.remove(pduId);
      }
      Callback<PduRaw> callback = waiter.getCallback();
      if (pdu.isOkOutcome() || isChunkResponse) {
        callback.onResponse(pdu);
      } else {
        callback.onFailure(new PduException("Received PDU has negative outcome", pdu));
      }
    } catch (Exception e) {
      mUserListener.onError(e);
    }
  }

  private String generateId() {
    Integer value = mIdCounter.getAndIncrement();
    return value.toString();
  }

  private ListenableFuture<PduRaw> send(final Pdu<?> pdu) {
    final SettableFuture<PduRaw> future = SettableFuture.create();

    // add callback to be sure that we remove waiters if user cancels future
    Futures.addCallback(future, new FutureCallback<PduRaw>() {
      @Override
      public void onSuccess(PduRaw result) {
        if (null != pdu.getId()) {
          mResponseWaiters.remove(pdu.getId());
        }
      }

      @Override
      public void onFailure(Throwable t) {
        if (null != pdu.getId()) {
          mResponseWaiters.remove(pdu.getId());
        }
      }
    });

    // set waiter callback to pass values to future
    sendWithCallback(pdu, new Callback<PduRaw>() {
      @Override
      public void onResponse(PduRaw result) {
        future.set(result);
      }

      @Override
      public void onFailure(Throwable t) {
        if (t instanceof CancellationException) {
          future.cancel(true);
        } else {
          future.setException(t);
        }
      }
    });
    return future;
  }

  private void sendWithCallback(final Pdu<?> pdu, final Callback<PduRaw> callback) {
    checkNotNull(pdu);
    checkNotNull(callback);

    if (this.isDisposed) {
      callback.onFailure(new RuntimeException("Connection is disposed or closed"));
      return;
    }

    final ResponseWaiter responseWaiter = new ResponseWaiter(pdu, callback);

    final String id = pdu.getId();
    final boolean isAckRequired = !Strings.isNullOrEmpty(id);

    // if id is not null then add it to the map of response waiters

    if (isAckRequired) {
      ResponseWaiter alreadyExistedWaiter = mResponseWaiters.putIfAbsent(id, responseWaiter);

      // if we found already created waiter for this id then something is going wrong

      if (null != alreadyExistedWaiter) {
        callback.onFailure(
            new IllegalStateException("Response with the same id has been already scheduled"));
        return;
      }
    }

    try {
      String json = mSerializer.toJson(pdu);
      LOG.debug("[send] " + json);
      mTransport.send(json);
      if (!isAckRequired) {
        callback.onResponse(null);
      }
    } catch (Exception e) {
      LOG.error("Failed to send PDU", e);
      callback.onFailure(e);
      mResponseWaiters.remove(id);
    }
  }

  private static class ResponseWaiter {
    final Pdu<?> mRequestPdu;
    final Callback<PduRaw> mCallback;

    ResponseWaiter(Pdu<?> requestPdu, Callback<PduRaw> callback) {
      mRequestPdu = requestPdu;
      mCallback = callback;
    }

    Callback<PduRaw> getCallback() {
      return mCallback;
    }

    void dispose() {
      mCallback.onFailure(new CancellationException());
    }
  }

  private class InnerTransportListener implements TransportListener {
    private TransportListener mUserListener;

    InnerTransportListener(TransportListener userListener) {
      mUserListener = userListener;
    }

    @Override
    public void onConnected() {
      if (isDisposed) { return; }
      mUserListener.onConnected();
    }

    @Override
    public void onDisconnected() {
      if (isDisposed) { return; }
      dispose();
      mUserListener.onDisconnected();
    }

    @Override
    public void onMessage(final String message) {
      if (isDisposed) { return; }
      processIncomingPDU(message);
    }

    @Override
    public void onTransportError(Exception ex) {
      if (isDisposed) { return; }
      mUserListener.onTransportError(ex);
    }

    @Override
    public void onConnectingError(Exception ex) {
      if (isDisposed) { return; }
      mUserListener.onConnectingError(ex);
    }
  }
}
