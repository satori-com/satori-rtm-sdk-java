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
 * Listener interface to define application functionality based on subscription state changes. For example,
 * you can define application functionality for when a channel receives a message, when the application
 * subscribes or unsubscribes to a channel, or any of the intermediate states in between.
 * <p>
 * Set an implementation of this interface for the client when it subscribes to a channel with
 * {@link RtmClient#createSubscription(String, EnumSet, SubscriptionListener)}.
 * <p>
 * All the methods for the listener implementation apply only to the channel for which you set up the listener.
 * <p>
 * {@link SubscriptionAdapter} is an empty implementation of this interface.
 *
 * @see SubscriptionAdapter
 */
public interface SubscriptionListener {
  /**
   * Called when a subscription is created.
   * <p>
   * Note that you can create a subscription when the client is not connected to the RTM Service. The Java SDK
   * manages the subscription workflow and sends the actual request when the client connects to the RTM Service.
   */
  void onCreated();

  /**
   * Called when a channel subscription is deleted.
   */
  void onDeleted();

  /**
   * Called when a subscription enters the {@code unsubscribed} state. This is the initial state for the
   * subscription state machine.
   * <p>
   * A subscription enters this state in the following situations:
   * <ul>
   * <li>Subscription has been created and initialized with {@code unsubscribed} state.</li>
   * <li>Connection is dropped</li>
   * <li>You explicitly unsubscribe from a channel. The {@code request} and {@code reply} parameters contain the
   * unsubscribe request and response PDUs, respectively.</li>
   * </ul>
   *
   * @param request Unsubscribe request PDU.
   * @param reply   Unsubscribe response PDU.
   */
  void onEnterUnsubscribed(UnsubscribeRequest request, UnsubscribeReply reply);

  /**
   * Called when a subscription leaves the {@code unsubscribed} state. This is the initial state for the
   * subscription state machine.
   * <p>
   * Subscription leaves this state when connection is established
   *
   * @param request Unsubscribe request PDU.
   * @param reply   Unsubscribe response PDU.
   */
  void onLeaveUnsubscribed(UnsubscribeRequest request, UnsubscribeReply reply);

  /**
   * Called when a subscription enters the {@code subscribing} state.
   * <p>
   * A subscription enters this state when the client is attempting to subscribe to a channel.
   *
   * @param request Unsubscribe request PDU.
   */
  void onEnterSubscribing(SubscribeRequest request);

  /**
   * Called when a subscription leaves the {@code subscribing} state.
   * <p>
   * A subscription leaves this state when the subscribe operation completes successfully or fails.
   *
   * @param request Unsubscribe request PDU.
   */
  void onLeaveSubscribing(SubscribeRequest request);

  /**
   * Called when a subscription enters the {@code subscribed} state.
   * <p>
   * A subscription enters this state when the subscribe operation completes successfully.
   *
   * @param request Subscribe request PDU.
   * @param reply   Subscribe reply PDU.
   */
  void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply);

  /**
   * Called when a subscription leaves the {@code subscribed} state.
   * <p>
   * A subscription leaves the state when you are no longer subscribed to the channel. For example, the subscription
   * leaves this state when the user explicitly removes the subscription from the channel or the connection drops.
   *
   * @param request Subscribe request PDU.
   * @param reply   Subscribe reply PDU.
   */
  void onLeaveSubscribed(SubscribeRequest request, SubscribeReply reply);

  /**
   * Called when a subscription enters {@code unsubscribing} state.
   * <p>
   * A subscription enters this state the client attempts to unsubscribe.
   *
   * @param request Unsubscribe request PDU.
   */
  void onEnterUnsubscribing(UnsubscribeRequest request);

  /**
   * Called when a subscription leaves the {@code unsubscribing} state.
   * <p>
   * A subscription leaves this state when the unsubscribe operation completes successfully or fails.
   *
   * @param request Unsubscribe request PDU.
   */
  void onLeaveUnsubscribing(UnsubscribeRequest request);

  /**
   * Called when a subscription enters the {@code failed} state.
   * <p>
   * A subscription enters this state when a channel error is sent from the RTM Service.
   */
  void onEnterFailed();

  /**
   * Called when a subscription leaves the {@code failed} state.
   * <p>
   * Subscription leaves this state when the client disconnects or user removes subscription explicitly.
   */
  void onLeaveFailed();

  /**
   * Called when the client receives a message from the RTM Service that was published to the subscription.
   *
   * @param data Subscription data PDU.
   */
  void onSubscriptionData(SubscriptionData data);

  /**
   * Called when the client receives a subscription error from the RTM Service.
   *
   * @param error Subscription error.
   */
  void onSubscriptionError(SubscriptionError error);

  /**
   * Called when the client receives a subscription info from the RTM Service.
   *
   * @param info Subscription info body.
   */
  void onSubscriptionInfo(SubscriptionInfo info);
}
