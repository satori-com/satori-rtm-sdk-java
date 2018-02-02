package com.satori.rtm;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduRaw;
import com.satori.rtm.model.PublishReply;
import com.satori.rtm.model.PublishRequest;
import com.satori.rtm.model.SubscriptionData;
import com.satori.rtm.model.SubscriptionError;
import com.satori.rtm.model.SubscriptionInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

class PubSub {
  private final Map<String, ChannelSubscription> mSubscriptions;
  private final Set<String> mSubIds;
  private final RtmService mRtmService;
  private ExecutorService mDispatcher;

  PubSub(RtmService rtmService, ExecutorService dispatcher) {
    this.mDispatcher = dispatcher;
    this.mRtmService = rtmService;
    this.mSubscriptions = new HashMap<String, ChannelSubscription>();
    this.mSubIds = Sets.newConcurrentHashSet();
  }

  void onConnected() {
    for (Map.Entry<String, ChannelSubscription> entry : mSubscriptions.entrySet()) {
      entry.getValue().onConnected();
    }
  }

  void onDisconnected() {
    for (Map.Entry<String, ChannelSubscription> entry : mSubscriptions.entrySet()) {
      entry.getValue().onDisconnected();
    }
  }

  public void createSubscription(final String subscriptionId, final SubscriptionConfig config) {
    if (Strings.isNullOrEmpty(subscriptionId)) {
      throw new IllegalArgumentException("subscriptionId can't be null");
    }
    boolean exist = mSubIds.contains(subscriptionId);
    if (exist) {
      throw new IllegalStateException("Subscription already exists");
    }
    mSubIds.add(subscriptionId);
    mDispatcher.submit(new Runnable() {
      @Override
      public void run() {
        mSubIds.add(subscriptionId);
        ChannelSubscription fsm = mSubscriptions.get(subscriptionId);
        if (null == fsm) {
          fsm = new ChannelSubscription(subscriptionId, config, mRtmService);
          mSubscriptions.put(subscriptionId, fsm);
          fsm.enterStartState();
        } else {
          fsm.updateSubscriptionConfig(config);
        }
      }
    });
  }

  void removeSubscription(final String subscriptionId) {
    if (Strings.isNullOrEmpty(subscriptionId)) {
      throw new IllegalArgumentException("subscriptionId can't be null");
    }
    boolean exist = mSubIds.remove(subscriptionId);
    if (!exist) {
      throw new IllegalStateException("Subscription doesn't exist");
    }
    mDispatcher.submit(new Runnable() {
      @Override
      public void run() {
        ChannelSubscription fsm = mSubscriptions.get(subscriptionId);
        fsm.unsubscribe();
      }
    });
  }

  void deleteSubscriptionFromRegistry(String subscriptionId) {
    mSubIds.remove(subscriptionId);
    mSubscriptions.remove(subscriptionId);
  }

  /**
   * @deprecated  Use {@link PubSub#publish(PublishRequest, Ack)}
   */
  @Deprecated
  public <T> ListenableFuture<Pdu<PublishReply>> publish(final String channel,
      final T message,
      final Ack ack) {
    final PublishRequest<T> publishRequest = new PublishRequest<T>(channel, message);
    return publish(publishRequest, ack);
  }

  public <T> ListenableFuture<Pdu<PublishReply>> publish(final PublishRequest<T> request, final Ack ack) {
    return mRtmService.send("rtm/publish", request, ack, PublishReply.class);
  }

  void onUnsolicitedPDU(PduRaw unsolicitedPdu) {
    String action = unsolicitedPdu.getAction();

    if ("rtm/subscription/error".equals(action)) {
      Pdu<SubscriptionError> pdu = unsolicitedPdu.convertBodyTo(SubscriptionError.class);
      SubscriptionError body = pdu.getBody();
      String id = body.getSubscriptionId();
      ChannelSubscription fsm = mSubscriptions.get(id);
      fsm.onSubscriptionError(pdu);
    } else if ("rtm/subscription/data".equals(action)) {
      Pdu<SubscriptionData> pdu = unsolicitedPdu.convertBodyTo(SubscriptionData.class);
      String id = pdu.getBody().getSubscriptionId();
      ChannelSubscription fsm = mSubscriptions.get(id);
      fsm.onSubscriptionData(pdu);
    } else if ("rtm/subscription/info".equals(action)) {
      Pdu<SubscriptionInfo> pdu = unsolicitedPdu.convertBodyTo(SubscriptionInfo.class);
      SubscriptionInfo body = pdu.getBody();
      String id = body.getSubscriptionId();
      ChannelSubscription fsm = mSubscriptions.get(id);
      fsm.onChannelInfo(pdu);
    }
  }
}
