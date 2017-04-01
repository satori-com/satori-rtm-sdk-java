package com.satori.rtm;

import com.google.common.base.Strings;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionError;
import com.satori.rtm.utils.TryCatchProxy;
import java.util.EnumSet;

/**
 * Defines the initial settings for a channel subscription. Includes the method used by Java SDK
 * to resubscribe after the WebSocket connection to the RTM Service drops and the SDK reconnects.
 * <p>
 * The Java SDK provides several strategies for resubscribing. See {@link SubscriptionMode}.
 * <p>
 * You can also define your own strategies with this class.
 */
public class SubscriptionConfig {
  final private SubscribeRequest mSubscribeRequest;
  final private EnumSet<SubscriptionMode> mSubscriptionModes;
  final private SubscriptionListener mUserListeners;

  /**
   * Creates a subscription configuration to use in channel subscription requests, with a specific
   * subscription modes.
   *
   * @param modes    Subscription modes.
   * @param listener Subscription listener.
   */
  public SubscriptionConfig(EnumSet<SubscriptionMode> modes, SubscriptionListener listener) {
    // wrap all user's code to try catch proxy
    SubscriptionListener safeListener = TryCatchProxy.wrap(listener, SubscriptionListener.class);
    this.mSubscriptionModes = modes;
    this.mUserListeners = safeListener;
    this.mSubscribeRequest = new SubscribeRequest();
  }

  /**
   * Gets the last {@code position} returned by the RTM Service for the channel subscription.
   *
   * @return Stream position.
   */
  public String getPosition() {
    return mSubscribeRequest.getPosition();
  }

  /**
   * Sets the {@code position} value to use in subscription requests.
   *
   * @param position Stream position.
   * @return SubscriptionConfig instance.
   */
  public SubscriptionConfig setPosition(String position) {
    mSubscribeRequest.setPosition(position);
    return this;
  }

  /**
   * Sets the history {@code age} value to use in subscription requests.
   * <p>
   * To use this method, set history settings for the channel in the Developer Portal.
   *
   * @param age Age value in seconds.
   * @return SubscriptionConfig instance.
   */
  public SubscriptionConfig setAge(Integer age) {
    if (null == age) {
      return this;
    }
    SubscribeRequest.History history = mSubscribeRequest.getHistory();
    if (null == history) {
      history = new SubscribeRequest.History();
      mSubscribeRequest.setHistory(history);
    }
    history.setAge(age);
    return this;
  }

  /**
   * Sets the history {@code count} value to use in subscription requests.
   * <p>
   * To use this method, set history settings for the channel in the Developer Portal.
   *
   * @param count Count value.
   * @return SubscriptionConfig instance.
   */
  public SubscriptionConfig setCount(Integer count) {
    if (null == count) {
      return this;
    }
    SubscribeRequest.History history = mSubscribeRequest.getHistory();
    if (null == history) {
      history = new SubscribeRequest.History();
      mSubscribeRequest.setHistory(history);
    }
    history.setCount(count);
    return this;
  }

  /**
   * Sets the {@code filter} value for a channel subscription.
   * <p>
   * A filter is a statement created with fSQL that defines the filter query to
   * run on the messages published to the channel
   *
   * @param filter Filter value.
   * @return SubscriptionConfig instance.
   */
  public SubscriptionConfig setFilter(String filter) {
    mSubscribeRequest.setFilter(filter);
    return this;
  }

  /**
   * Sets the {@code period} value to use with a filter in a channel subscription.
   * <p>
   * The {@code period} value is the period of time, in seconds, that the RTM Service runs the filter on
   * channel messages before it sends the result to the client application
   *
   * @param period Period value.
   * @return SubscriptionConfig instance.
   */
  public SubscriptionConfig setPeriod(Integer period) {
    mSubscribeRequest.setPeriod(period);
    return this;
  }

  SubscriptionListener getUserListener() {
    return mUserListeners;
  }

  /**
   * Called when a client application receives a new {@code position} value from the RTM Service.
   * <p>
   * Channel data messages and RTM Service replies to requests contain a {@code position} value.
   *
   * @param position Position value.
   */
  void onPosition(String position) {
    // method updates position from rtm replies
    // ignore position if it's not needed
    if (!mSubscriptionModes.contains(SubscriptionMode.TRACK_POSITION)) {
      return;
    }
    mSubscribeRequest.setPosition(position);
  }

  /**
   * Called when a client application receives a channel error from the RTM Service.
   *
   * @param error Error from RTM Service.
   * @return {@code true} if the error is not recoverable; {@code false} otherwise.
   */
  protected boolean onError(SubscriptionError error) {
    return true;
  }

  /**
   * Called when a client application successfully subscribes to a channel and receives a positive confirmation
   * from the RTM Service.
   *
   * @param position Position value from the RTM Service reply.
   */
  void onSuccessSubscribe(String position) {
    // reset all initial subscription settings
    mSubscribeRequest.setHistory(null);
    mSubscribeRequest.setPosition(null);
    // try to set new position received from subscribe reply
    onPosition(position);
  }

  /**
   * Creates a PDU body for a subscription request.
   * <p>
   * Use this method to create a subscription request when resubscribing to a channel.
   *
   * @param subscriptionId {@code subscription_id} to subscribe to.
   */
  protected SubscribeRequest createSubscribeRequest(String subscriptionId) {
    SubscribeRequest request = new SubscribeRequest();
    if (Strings.isNullOrEmpty(mSubscribeRequest.getFilter())) {
      request.setChannel(subscriptionId);
    } else {
      request.setSubscriptionId(subscriptionId);
    }
    request.setPosition(mSubscribeRequest.getPosition());
    request.setHistory(mSubscribeRequest.getHistory());
    request.setFastForward(mSubscriptionModes.contains(SubscriptionMode.FAST_FORWARD));
    request.setFilter(mSubscribeRequest.getFilter());
    request.setPeriod(mSubscribeRequest.getPeriod());
    return request;
  }
}
