package com.satori.rtm;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.satori.rtm.connection.Connection;
import com.satori.rtm.model.DeleteReply;
import com.satori.rtm.model.DeleteRequest;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduRaw;
import com.satori.rtm.model.ReadReply;
import com.satori.rtm.model.ReadRequest;
import com.satori.rtm.model.WriteReply;
import com.satori.rtm.model.WriteRequest;
import com.satori.rtm.utils.FutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contract for RTM Service-specific API.
 */
class RtmService {
  private final static Logger LOG = LoggerFactory.getLogger(RtmService.class);
  private final Object mQueueLock = new Object();
  private final BlockingQueue<Runnable> mPendingQueue;
  private final AtomicReference<Connection> mConnection;
  private PubSub mPubSub;

  private RtmService(Integer pendingQueueLength) {
    this.mConnection = new AtomicReference<Connection>(null);
    this.mPendingQueue =
        0 < pendingQueueLength ? new LinkedBlockingQueue<Runnable>(pendingQueueLength) : null;
  }

  public static RtmService create(Integer pendingQueueLength, ExecutorService dispatcher) {
    RtmService service = new RtmService(pendingQueueLength);
    service.setPubSub(new PubSub(service, dispatcher));
    return service;
  }

  public boolean isConnected() {
    return (null != mConnection.get());
  }

  public Connection getConnection() {
    return mConnection.get();
  }

  public PubSub getPubSub() {
    return mPubSub;
  }

  void setPubSub(PubSub pubSub) {
    this.mPubSub = pubSub;
  }

  public ListenableFuture<Pdu<ReadReply>> read(ReadRequest request) {
    return send("rtm/read", request, Ack.YES, ReadReply.class);
  }

  public <T> ListenableFuture<Pdu<WriteReply>> write(WriteRequest<T> request, Ack ack) {
    return send("rtm/write", request, ack, WriteReply.class);
  }

  public ListenableFuture<Pdu<DeleteReply>> delete(DeleteRequest request, Ack ack) {
    return send("rtm/delete", request, ack, DeleteReply.class);
  }

  /**
   * Invoked by client after the WebSocket connection is established.
   *
   * @param connection Established connection.
   */
  public void onConnected(Connection connection) {
    mConnection.set(connection);
    mPubSub.onConnected();
    drainPendingQueue();
  }

  /**
   * Invoked by client when the WebSocket connection is lost.
   */
  public void onDisconnected() {
    mConnection.set(null);
    mPubSub.onDisconnected();
  }

  /**
   * Invoked by client when the client receives an unsolicited Protocol Data Unit (PDU).
   *
   * @param unsolicitedPdu Unsolicited PDU.
   */
  public void onUnsolicitedPDU(PduRaw unsolicitedPdu) {
    checkNotNull(unsolicitedPdu);
    String action = unsolicitedPdu.getAction();
    checkNotNull(action);
    if (action.startsWith("rtm/subscription")) {
      mPubSub.onUnsolicitedPDU(unsolicitedPdu);
    }
  }

  private void performAction(Runnable action) {
    if (isConnected()) {
      action.run();
    } else {
      maybeEnqueuePendingAction(action);
    }
  }

  private void maybeEnqueuePendingAction(Runnable action) {
    if (null == mPendingQueue) {
      // offline queue has zero size
      throw new IllegalStateException("Offline operations are disabled");
    }

    synchronized (mQueueLock) {
      // make sure that we aren't connected before enqueueing
      if (!isConnected()) {
        mPendingQueue.add(action);
      } else {
        action.run();
      }
    }
  }

  private void drainPendingQueue() {
    if (null == mPendingQueue) {
      return;
    }

    synchronized (mQueueLock) {
      LOG.debug("Drain pending actions (#{})", mPendingQueue.size());
      while (isConnected() && (0 < mPendingQueue.size())) {
        Runnable action = mPendingQueue.poll();
        try {
          action.run();
        } catch (Exception ex) {
          LOG.warn("Error while performing the pending action", ex);
        }
      }
    }
  }

  <T> ListenableFuture<Pdu<T>> send(final String action, final Object payload,
                                    final Ack ack, final Class<T> clazz) {
    final SettableFuture<Pdu<T>> future = SettableFuture.create();

    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        Connection connection = getConnection();
        if (null == connection) {
          throw new IllegalStateException("You aren't connected to RTM");
        }

        ListenableFuture<Pdu<T>> response;
        if (ack == Ack.NO) {
          // if we don't need ack then transform return value to null and return future with null
          response = Futures.transform(
              connection.sendNoAck(action, payload),
              new Function<Void, Pdu<T>>() {
                @Override
                public Pdu<T> apply(Void input) {
                  return null;
                }
              });

          FutureUtils.delegateTo(response, future);
        } else {
          response = connection.send(action, payload, clazz);
          FutureUtils.delegateTo(response, future);
        }
      }
    };

    try {
      performAction(runnable);
    } catch (Exception ex) {
      future.setException(ex);
    }

    FutureUtils.addExceptionLogging(future, "RTM action is failed", LOG);
    return future;
  }
}
