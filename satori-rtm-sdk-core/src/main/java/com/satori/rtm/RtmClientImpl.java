package com.satori.rtm;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.auth.AuthException;
import com.satori.rtm.auth.AuthProvider;
import com.satori.rtm.connection.Connection;
import com.satori.rtm.connection.ConnectionListener;
import com.satori.rtm.connection.Serializer;
import com.satori.rtm.model.DeleteReply;
import com.satori.rtm.model.DeleteRequest;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduRaw;
import com.satori.rtm.model.PublishReply;
import com.satori.rtm.model.ReadReply;
import com.satori.rtm.model.ReadRequest;
import com.satori.rtm.model.SystemWideException;
import com.satori.rtm.model.WriteReply;
import com.satori.rtm.model.WriteRequest;
import com.satori.rtm.transport.TransportFactory;
import com.satori.rtm.utils.FutureUtils;
import com.satori.rtm.utils.TrampolineExecutorService;
import com.satori.rtm.utils.TryCatchProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

class RtmClientImpl implements RtmClient {
  private static final Logger LOG = LoggerFactory.getLogger(RtmClient.class);
  private final RtmClientStateMachine mClientFSM;
  private final RtmClientListener mUserListener;
  private final TransportFactory mTransportFactory;
  private final AuthProvider mAuthProvider;
  private final URI mURI;
  private final ScheduledExecutorService mScheduledExecutorService;
  private final boolean mIsExtDispatcher;
  private final boolean mIsExtScheduler;
  private final RtmService mRtmService;
  private ExecutorService mDispatcher;
  private Connection mConnection;
  private Serializer mJsonSerializer;
  private boolean mShouldDispatchTransport;

  public RtmClientImpl(final URI uri, final RtmClientBuilder opts) {
    this.mURI = uri;
    this.mUserListener = TryCatchProxy.wrap(opts.mUserListener, RtmClientListener.class);
    this.mAuthProvider = opts.mAuthProvider;
    this.mTransportFactory = opts.mTransportFactory;
    this.mJsonSerializer = opts.mJsonSerializer;
    // create scheduler if it wasn't passed from the builder
    this.mIsExtScheduler = (null != opts.mScheduledExecutorService);
    this.mIsExtDispatcher = (null != opts.mDispatcher);
    this.mScheduledExecutorService =
        mIsExtScheduler ? opts.mScheduledExecutorService : Executors.newScheduledThreadPool(1);
    this.mDispatcher = mIsExtDispatcher ? opts.mDispatcher : new TrampolineExecutorService();
    this.mShouldDispatchTransport = opts.mShouldDispatchTransport;
    this.mRtmService = RtmService.create(opts.mPendingActionQueueLength, mDispatcher);
    this.mClientFSM = new RtmClientStateMachine(
        this,
        new RtmClientStateMachineListener(),
        mScheduledExecutorService,
        opts.mIsAutoReconnect,
        opts.mMinReconnectInterval,
        opts.mMaxReconnectInterval,
        mDispatcher
    );
    mDispatcher.submit(new Runnable() {
      @Override
      public void run() { mClientFSM.enterStartState(); }
    });
  }

  @Override
  public void start() {
    mDispatcher.submit(new Runnable() {
      @Override
      public void run() { mClientFSM.onStart(); }
    });
  }

  @Override
  public void stop() {
    mDispatcher.submit(new Runnable() {
      @Override
      public void run() { mClientFSM.onStop(); }
    });
  }

  @Override
  public void restart() {
    stop();
    start();
  }

  @Override
  public void shutdown() {
    stop();
    // shutdown scheduler if it wasn't passed from the builder
    if (!mIsExtScheduler) {
      mScheduledExecutorService.shutdown();
    }
    if (!mIsExtDispatcher) {
      mDispatcher.shutdown();
    }
  }

  @Override
  public <T> ListenableFuture<Pdu<PublishReply>> publish(final String channel, final T message,
                                                         Ack ack) {
    return mRtmService.getPubSub().publish(channel, message, ack);
  }


  @Override
  public void createSubscription(String channel, EnumSet<SubscriptionMode> modes,
                                 SubscriptionListener listener) {
    createSubscription(channel, new SubscriptionConfig(modes, listener));
  }

  @Override
  public void createSubscription(final String channelOrSubId, final SubscriptionConfig config) {
    mRtmService.getPubSub().createSubscription(channelOrSubId, config);
  }

  @Override
  public void removeSubscription(final String subscriptionId) {
    mRtmService.getPubSub().removeSubscription(subscriptionId);
  }

  @Override
  public boolean isConnected() {
    return mClientFSM.isConnected();
  }

  @Override
  public Connection getConnection() {
    return mConnection;
  }

  @Override
  public ListenableFuture<Pdu<ReadReply>> read(String key) {
    return read(new ReadRequest(key, null));
  }

  @Override
  public ListenableFuture<Pdu<ReadReply>> read(ReadRequest request) {
    return mRtmService.read(request);
  }

  @Override
  public <T> ListenableFuture<Pdu<WriteReply>> write(String key, T value, Ack ack) {
    WriteRequest<T> writeRequest = new WriteRequest<T>(key, value);
    return write(writeRequest, ack);
  }

  @Override
  public <T> ListenableFuture<Pdu<WriteReply>> write(WriteRequest<T> writeRequest, Ack ack) {
    return mRtmService.write(writeRequest, ack);
  }

  @Override
  public ListenableFuture<Pdu<DeleteReply>> delete(String key, Ack ack) {
    return mRtmService.delete(new DeleteRequest(key), ack);
  }

  public ExecutorService getDispatcher() {
    return mDispatcher;
  }

  private void connect() {
    try {
      mConnection = tryCreateConnection();
    } catch (Exception ex) {
      mClientFSM.onConnectingFailed();
    }
  }

  private Connection tryCreateConnection() {
    try {
      final Connection connection = Connection.create(
          mURI,
          mTransportFactory,
          mJsonSerializer
      );
      ConnectionListener listener = new InnerConnectionListener();
      ExecutorService transportDispatcher = mShouldDispatchTransport ? mDispatcher : null;
      connection.connect(listener, transportDispatcher);
      return connection;
    } catch (Exception ex) {
      mClientFSM.onConnectingFailed();
      LOG.warn(String.format("Unable to connect to the server %s", mURI), ex);
      mUserListener.onConnectingError(this, ex);
      return null;
    }
  }

  /**
   * Listens the connection events and triggers related state machine transitions.
   */
  private class InnerConnectionListener implements ConnectionListener {
    private final RtmClientImpl mClient = RtmClientImpl.this;

    @Override
    public void onUnsolicitedPDU(PduRaw pdu) {
      try {
        if (pdu.isSystemError()) {
          mConnection.close();
          mUserListener.onError(mClient, new SystemWideException(pdu));
          return;
        }
        mRtmService.onUnsolicitedPDU(pdu);
      } catch (Exception ex) {
        String msg = "Error when calling onUnsolicitedPDU listener: " + pdu;
        LOG.error(msg, ex);
        mUserListener.onError(mClient, ex);
      }
    }

    @Override
    public void onError(Exception error) {
      LOG.warn("General connection error", error);
      mUserListener.onError(mClient, error);
    }

    @Override
    public void onConnected() {
      if (null == mAuthProvider) {
        // onConnected event could be triggered from WritingThread in nv-websocket-client library
        // in some specific cases. It could cause a deadlock in WritingThread if user sends a lot
        // of text frames from his callback.
        // As a workaround we dispatch onConnected event from scheduledExecutorService thread.
        // See: https://github.com/TakahikoKawasaki/nv-websocket-client/issues/99
        mScheduledExecutorService.execute(new Runnable() {
          @Override
          public void run() {
            mDispatcher.execute(new Runnable() {
              @Override
              public void run() {
                mClientFSM.onConnected();
              }
            });
          }
        });
        return;
      }

      ListenableFuture<Void> authResult = mAuthProvider.authenticate(mConnection);
      FutureUtils.addExceptionLogging(authResult, "Authentication is failed", LOG);
      Futures.addCallback(authResult, new FutureCallback<Void>() {
        @Override
        public void onSuccess(Void result) {
          mClientFSM.onConnected();
        }

        @Override
        public void onFailure(Throwable t) {
          mUserListener.onError(mClient, new AuthException(t));
          mClientFSM.onConnectingFailed();
        }
      });
    }

    @Override
    public void onDisconnected() {
      mConnection = null;
      mClientFSM.onDisconnected();
    }

    @Override
    public void onTransportError(Exception ex) {
      LOG.warn("Transport error", ex);
      mClientFSM.onInternalError();
      mUserListener.onTransportError(mClient, ex);
    }

    @Override
    public void onConnectingError(Exception ex) {
      LOG.warn(String.format("Unable to connect to the server %s", mURI), ex);
      mClientFSM.onConnectingFailed();
      mUserListener.onConnectingError(mClient, ex);
    }

    @Override
    public void onMessage(String message) { }
  }

  private class RtmClientStateMachineListener extends RtmClientAdapter {
    @Override
    public void onEnterStopped(RtmClient client) {
      if (null != mConnection) {
        mConnection.dispose();
      }
      mConnection = null;
      mUserListener.onEnterStopped(client);
    }

    @Override
    public void onLeaveStopped(RtmClient client) {
      mUserListener.onLeaveStopped(client);
    }

    @Override
    public void onEnterConnecting(RtmClient client) {
      connect();
      mUserListener.onEnterConnecting(client);
    }

    @Override
    public void onLeaveConnecting(RtmClient client) {
      mUserListener.onLeaveConnecting(client);
    }

    @Override
    public void onEnterConnected(RtmClient client) {
      // inform RTM that we're connected
      mRtmService.onConnected(mConnection);
      mUserListener.onEnterConnected(client);
    }

    @Override
    public void onLeaveConnected(RtmClient client) {
      // inform RTM that we're disconnected
      mRtmService.onDisconnected();
      mUserListener.onLeaveConnected(client);
    }

    @Override
    public void onEnterAwaiting(RtmClient client) {
      if (null != mConnection) {
        mConnection.dispose();
      }
      mConnection = null;
      mUserListener.onEnterAwaiting(client);
    }

    @Override
    public void onLeaveAwaiting(RtmClient client) {
      mUserListener.onLeaveAwaiting(client);
    }
  }
}

