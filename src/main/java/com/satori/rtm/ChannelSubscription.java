package com.satori.rtm;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.connection.Connection;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduException;
import com.satori.rtm.model.PduRaw;
import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import com.satori.rtm.model.SubscriptionError;
import com.satori.rtm.model.SubscriptionInfo;
import com.satori.rtm.model.UnsubscribeReply;
import com.satori.rtm.model.UnsubscribeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

/**
 * The subscription finite state machine manages all states and transitions for the subscription.
 */
class ChannelSubscription extends AbstractSubscription {
  private final static Logger LOG = LoggerFactory.getLogger(ChannelSubscription.class);
  private final String mSubscriptionId;

  private SubscriptionConfig mSubscriptionConfig;
  private SubscriptionConfig mNextSubscriptionConfig;

  private SubscribeRequest mSubscribeRequest;
  private SubscribeReply mSubscribeReply;
  private UnsubscribeRequest mUnsubscribeRequest;
  private UnsubscribeReply mUnsubscribeReply;


  ChannelSubscription(String subscriptionId,
                      SubscriptionConfig subscriptionConfig,
                      RtmService rtmService) {
    super(rtmService);
    this.mSubscriptionConfig = subscriptionConfig;
    this.mSubscriptionId = subscriptionId;
  }

  void updateSubscriptionConfig(SubscriptionConfig subscriptionConfig) {
    if (mMode != Mode.UNLINKED) {
      throw new RuntimeException("Subscribe while not unsubscribed");
    }
    if (null != mNextSubscriptionConfig) {
      mNextSubscriptionConfig.getUserListener().onDeleted();
    }
    mNextSubscriptionConfig = subscriptionConfig;
    mMode = Mode.CYCLE;
    this.mNextSubscriptionConfig.getUserListener().onCreated();
    getState().checkModeTransition(this);
  }

  @Override
  public void onDisconnected() {
    super.onDisconnected();
    if (!mSubscriptionConfig.isAutoReconnect()) {
      dispose();
    }
  }

  @Override
  public void enterStartState() {
    mSubscriptionConfig.getUserListener().onCreated();
    super.enterStartState();
  }


  @Override
  public void unsubscribe() {
    mMode = Mode.UNLINKED;
    getState().checkModeTransition(ChannelSubscription.this);
  }

  @Override
  public void dispose() {
    getRtmService().getPubSub().deleteSubscriptionFromRegistry(getSubscriptionId());
    mSubscriptionConfig.getUserListener().onDeleted();
  }

  private String getSubscriptionId() {
    return mSubscriptionId;
  }

  private SubscriptionListener getUserListener() {
    return mSubscriptionConfig.getUserListener();
  }


  void onChannelInfo(final Pdu<SubscriptionInfo> pdu) {
    LOG.info("Received rtm/subscription/info PDU: {}", pdu);
    SubscriptionInfo subscriptionInfo = pdu.getBody();
    String next = subscriptionInfo.getPosition();
    if (!Strings.isNullOrEmpty(next)) {
      mSubscriptionConfig.onPosition(next);
    }
    getUserListener().onSubscriptionInfo(subscriptionInfo);
  }

  void onSubscriptionError(final Pdu<SubscriptionError> pdu) {
    LOG.warn("Received rtm/subscription/error PDU: {}", pdu);
    SubscriptionError subscriptionError = pdu.getBody();
    getUserListener().onSubscriptionError(subscriptionError);
    if (mSubscriptionConfig.onError(subscriptionError)) {
      getState().transition(this, FAILED);
    } else {
      getState().transition(this, UNSUBSCRIBED);
    }
  }

  protected void onSubscriptionData(final Pdu<SubscriptionData> pdu) {
    SubscriptionData subscriptionData = pdu.getBody();
    mSubscriptionConfig.onPosition(subscriptionData.getPosition());
    getUserListener().onSubscriptionData(subscriptionData);
  }

  @Override
  protected Future<?> doSubscribeRequest(Connection connection) {
    String subscriptionId = getSubscriptionId();
    mSubscribeRequest = mSubscriptionConfig.createSubscribeRequest(subscriptionId);

    ListenableFuture<Pdu<SubscribeReply>> request =
        connection.send("rtm/subscribe", mSubscribeRequest, SubscribeReply.class);

    Futures.addCallback(request, new FutureCallback<Pdu<SubscribeReply>>() {
      public void onSuccess(Pdu<SubscribeReply> result) {
        if (SUBSCRIBING == getState()) {
          mSubscribeReply = result.getBody();
          mSubscriptionConfig.onSuccessSubscribe(mSubscribeReply.getPosition());
          getState().transition(ChannelSubscription.this, SUBSCRIBED);
        }
      }

      public void onFailure(Throwable t) {
        if (t instanceof CancellationException) {
          return;
        }

        if (SUBSCRIBING == getState()) {
          Pdu<SubscriptionError> pdu = extractSubscriptionErrorPdu(t);
          SubscriptionError subscriptionError = (null != pdu) ? pdu.getBody() : null;
          if (null != pdu) {
            LOG.warn("Received negative response while handling rtm/subscribe request: {}", pdu);
            getUserListener().onSubscriptionError(subscriptionError);
          } else {
            LOG.warn("Exception while handling rtm/subscribe request", t);
          }
          if (mSubscriptionConfig.onError(subscriptionError)) {
            getState().transition(ChannelSubscription.this, FAILED);
          } else {
            getState().transition(ChannelSubscription.this, UNSUBSCRIBED);
          }
        }
      }
    });
    return request;
  }

  @Override
  protected Future<?> doUnsubscribeRequest(Connection connection) {
    String subscriptionId = getSubscriptionId();

    final UnsubscribeRequest mUnsubscribeRequest = new UnsubscribeRequest(subscriptionId);
    ListenableFuture<Pdu<UnsubscribeReply>> request =
        connection.send("rtm/unsubscribe", mUnsubscribeRequest, UnsubscribeReply.class);

    Futures.addCallback(request, new FutureCallback<Pdu<UnsubscribeReply>>() {
      public void onSuccess(Pdu<UnsubscribeReply> result) {
        if (UNSUBSCRIBING == getState()) {
          mUnsubscribeReply = result.getBody();
          mSubscriptionConfig.onPosition(mUnsubscribeReply.getPosition());
          getState().transition(ChannelSubscription.this, UNSUBSCRIBED);
        }
      }

      public void onFailure(Throwable t) {
        if (t instanceof CancellationException) {
          return;
        }

        if (UNSUBSCRIBING == getState()) {
          Pdu<SubscriptionError> pdu = extractSubscriptionErrorPdu(t);
          if (null != pdu) {
            LOG.warn("Received negative response while handling rtm/unsubscribe request: {}", pdu);
            getUserListener().onSubscriptionError(pdu.getBody());
          } else {
            LOG.warn("Exception while handling rtm/unsubscribe request", t);
          }
          // rtm/unsubscribe/error indicates some internal error
          // try to close/open connection to restart the client
          getRtmService().getConnection().close();
        }
      }
    });

    return request;
  }

  @Override
  protected void onEnterUnsubscribed() {
    if (mNextSubscriptionConfig != null && Mode.LINKED == mMode) {
      mSubscriptionConfig.getUserListener().onDeleted();
      mSubscriptionConfig = mNextSubscriptionConfig;
      mSubscriptionConfig.getUserListener().onCreated();
    }
    mNextSubscriptionConfig = null;

    getUserListener().onEnterUnsubscribed(mUnsubscribeRequest, mUnsubscribeReply);
  }

  @Override
  protected void onLeaveUnsubscribed() {
    getUserListener().onLeaveUnsubscribed(mUnsubscribeRequest, mUnsubscribeReply);
    mUnsubscribeRequest = null;
    mUnsubscribeReply = null;
  }

  @Override
  protected void onEnterSubscribing() {
    getUserListener().onEnterSubscribing(mSubscribeRequest);
  }

  @Override
  protected void onLeaveSubscribing() {
    getUserListener().onLeaveSubscribing(mSubscribeRequest);
  }

  @Override
  protected void onEnterSubscribed() {
    getUserListener().onEnterSubscribed(mSubscribeRequest, mSubscribeReply);
  }

  @Override
  protected void onLeaveSubscribed() {
    getUserListener().onLeaveSubscribed(mSubscribeRequest, mSubscribeReply);
    mSubscribeRequest = null;
    mSubscribeReply = null;
  }

  @Override
  protected void onEnterUnsubscribing() {
    getUserListener().onEnterUnsubscribing(mUnsubscribeRequest);
  }

  @Override
  protected void onLeaveUnsubscribing() {
    getUserListener().onLeaveUnsubscribing(mUnsubscribeRequest);
  }

  @Override
  protected void onEnterFailed() {
    getUserListener().onEnterFailed();
  }

  @Override
  protected void onLeaveFailed() {
    getUserListener().onLeaveFailed();
  }

  private Pdu<SubscriptionError> extractSubscriptionErrorPdu(Throwable t) {
    if (t instanceof PduException) {
      PduException exception = (PduException) t;
      PduRaw raw = exception.getPdu();
      return raw.convertBodyTo(SubscriptionError.class);
    }
    return null;
  }

  Mode getMode() {
    return mMode;
  }

  SubscriptionConfig getSubscriptionConfig() {
    return mSubscriptionConfig;
  }

  @Override
  public String getName() {
    return "Channel[" + getSubscriptionId() + "]";
  }
}
