package com.satori.rtm;

import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import com.satori.rtm.model.SubscriptionError;
import com.satori.rtm.model.SubscriptionInfo;
import com.satori.rtm.model.UnsubscribeReply;
import com.satori.rtm.model.UnsubscribeRequest;
import java.util.EnumSet;

/**
 * Defines callback methods for events in the subscription lifecycle.
 * <p>
 * To set this for a subscription, call
 * {@link RtmClient#createSubscription(String, EnumSet, SubscriptionListener) RtmClient.createSubscription()}
 * or instantiate a {@code SubscriptionConfig} with a {@code SubscriptionListener}
 * ({@link SubscriptionConfig#SubscriptionConfig(EnumSet, SubscriptionListener)})
 * <p>
 * {@link SubscriptionAdapter} is an empty implementation of this interface.
 */
public interface SubscriptionListener {
  /**
   * Called when a subscription is created.
   * <p>
   * You can create a subscription even if your the client isn't connected to RTM. The
   * RTM SDK manages the subscription workflow and sends the actual request when the client
   * connects to RTM.
   */
  void onCreated();

  /**
   * Called when a subscription is deleted.
   */
  void onDeleted();

  /**
   * Called when a subscription enters the {@code unsubscribed} state. This is the initial state for
   * the subscription lifecycle.
   * <p>
   * A subscription enters this state in the following situations:
   * <ul>
   * <li>The subscription was created and initialized with the {@code unsubscribed} state</li>
   * <li>The connection is dropped</li>
   * <li>{@link RtmClient#removeSubscription(String) RtmClient.removeSubscription()} method is called</li>
   * </ul>
   *
   * @param request unsubscribe request PDU
   * @param reply   unsubscribe response PDU
   */
  void onEnterUnsubscribed(UnsubscribeRequest request, UnsubscribeReply reply);

  /**
   * Called when a subscription leaves the {@code unsubscribed} state. {@code unsubscribed} is the
   * initial state for the subscription lifecycle.
   * <p>
   * A subscription leaves this state when connection is established
   *
   * @param request unsubscribe request PDU
   * @param reply   unsubscribe response PDU
   */
  void onLeaveUnsubscribed(UnsubscribeRequest request, UnsubscribeReply reply);

  /**
   * Called when a subscription enters the {@code subscribing} state.
   * <p>
   * A subscription enters this state when the client is attempting to subscribe to a channel.
   *
   * @param request subscribe request PDU
   */
  void onEnterSubscribing(SubscribeRequest request);

  /**
   * Called when a subscription leaves the {@code subscribing} state.
   * <p>
   * A subscription leaves this state when the subscribe request completes successfully or fails.
   *
   * @param request subscribe request PDU.
   */
  void onLeaveSubscribing(SubscribeRequest request);

  /**
   * Called when a subscription enters the {@code subscribed} state.
   * <p>
   * A subscription enters this state when the subscribe request completes successfully and
   * an acknowledgement from RTM is received.
   *
   * @param request subscribe request PDU
   * @param reply   subscribe reply PDU
   */
  void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply);

  /**
   * Called when a subscription leaves the {@code subscribed} state.
   * <p>
   * A subscription leaves the state when you're no longer subscribed to the channel. For example,
   * the subscription leaves this state when you explicitly remove the subscription from the
   * channel or the connection drops.
   *
   * @param request subscribe request PDU
   * @param reply   subscribe reply PDU
   */
  void onLeaveSubscribed(SubscribeRequest request, SubscribeReply reply);

  /**
   * Called when a subscription enters {@code unsubscribing} state.
   * <p>
   * A subscription enters this state when the client attempts to unsubscribe by calling
   * {@link RtmClient#removeSubscription(String) RtmClient.removeSubscription()}.
   *
   * @param request unsubscribe request PDU
   */
  void onEnterUnsubscribing(UnsubscribeRequest request);

  /**
   * Called when a subscription leaves the {@code unsubscribing} state.
   * <p>
   * A subscription leaves this state when the unsubscribe request completes successfully or fails.
   *
   * @param request unsubscribe request PDU.
   */
  void onLeaveUnsubscribing(UnsubscribeRequest request);

  /**
   * Called when a subscription enters the {@code failed} state.
   * <p>
   * A subscription enters this state when a channel error is sent from RTM.
   */
  void onEnterFailed();

  /**
   * Called when a subscription leaves the {@code failed} state.
   * <p>
   * A subscription leaves this state when the client disconnects or user removes subscription explicitly.
   */
  void onLeaveFailed();

  /**
   * Called when the client receives a message from RTM that was published to the subscription.
   *
   * @param data Subscription data PDU.
   */
  void onSubscriptionData(SubscriptionData data);

  /**
   * Called when the client receives a subscription error from RTM.
   *
   * @param error Subscription error.
   */
  void onSubscriptionError(SubscriptionError error);

  /**
   * Called when the client receives a subscription info from RTM.
   *
   * @param info Subscription info body.
   */
  void onSubscriptionInfo(SubscriptionInfo info);
}
