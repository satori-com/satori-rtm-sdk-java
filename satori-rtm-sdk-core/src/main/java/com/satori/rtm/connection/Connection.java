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
 * Access RTM at the connection level to connect, send and receive PDUs, and wait
 * for responses.
 * <p>
 * Create a {@code Connection} with the {@link Connection#create(URI, TransportFactory, Serializer) create()} method.
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
   * Creates a {@link Connection} object for a specific RTM endpoint {@code URI}.
   *
   * @param uri              RTM endpoint
   * @param transportFactory transport factory
   * @param serializer       JSON serializer
   * @return {@link Connection}.
   * @throws TransportException couldn't create transport for the {@code URI}
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
   * Connects to RTM.
   *
   * @param listener   {@link ConnectionListener} object
   * @param dispatcher transport event dispatcher. If {@code null}, all events are fired from the transport thread.
   * @throws TransportException the transport can't connect
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
   * Asynchronously sends a Protocol Data Unit (<strong>PDU</strong>) to RTM without acknowledgement
   * <p>
   * This method creates a PDU from an operation and an object. It then sends the PDU to RTM. The object
   * must be serializable into a JSON object.
   * <p>
   * This method returns a <a href="http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/ListenableFuture.html">ListenableFuture</a>
   * that completes when the request is sent to RTM.
   *
   * @param operation PDU operation, for example, {@code "rtm/publish"}
   * @param body      PDU body
   * @return result of the asynchronous send operation
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
   * Asynchronously sends a Protocol Data Unit (<strong>PDU</strong>) to RTM.
   * <p>
   * This method creates a PDU from an operation and an object. It then sends the PDU to RTM. The object
   * must be serializable into a JSON object.
   * <p>
   * Use {@code responseClazz} to specify the format of the PDU body returned by RTM. The body is converted to an
   * object of type {@code responseClazz} before it's returned.
   * <p>
   * This method returns a <a href="http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/ListenableFuture.html">ListenableFuture</a>
   * that completes when a response is received from RTM.
   * <p>
   * The {@code ListenableFuture} can fail due to the following error:
   * <ul>
   * <li>{@link PduException}: an RTM error occured.</li>
   * </ul>
   *
   * @param operation     PDU operation, for example, {@code "rtm/publish"}
   * @param body          PDU body
   * @param responseClazz a {@link Class} instance of the response object type
   * @param <T>           the response object type
   * @return result of the send operation
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
   * Stops a specific connection and releases all allocated resources. All communication with RTM stops
   * when you call this method and the events aren't propagated to any listeners.
   * <p>
   * In general, use this method after the connection to RTM is already closed and you need to clean up
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

      if (!pdu.isChunkResponse()) {
        mResponseWaiters.remove(pduId);
      }
      Callback<PduRaw> callback = waiter.getCallback();
      //TODO: Refactor this not to throw on well formed responses.
      if (pdu.isOkOutcome() || pdu.isChunkResponse() || pdu.isWriteError()) {
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

    // if id isn't null, add it to the map of response waiters

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
