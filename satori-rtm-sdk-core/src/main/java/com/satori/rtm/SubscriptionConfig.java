package com.satori.rtm;

import com.google.common.base.Strings;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionError;
import com.satori.rtm.utils.TryCatchProxy;
import java.util.EnumSet;

/**
 * Provides settings that configure a subscription.
 */
public class SubscriptionConfig {
  final private SubscribeRequest mSubscribeRequest;
  final private EnumSet<SubscriptionMode> mSubscriptionModes;
  final private SubscriptionListener mUserListeners;

  /**
   * Creates a subscription configuration to use in the subscription requests, with a specific
   * subscription modes.
   *
   * @param modes    subscription modes
   * @param listener subscription listener
   */
  public SubscriptionConfig(EnumSet<SubscriptionMode> modes, SubscriptionListener listener) {
    // wrap all user's code to try catch proxy
    SubscriptionListener safeListener = TryCatchProxy.wrap(listener, SubscriptionListener.class);
    this.mSubscriptionModes = modes;
    this.mUserListeners = safeListener;
    this.mSubscribeRequest = new SubscribeRequest();
  }

  /**
   * Gets the current subscription {@code position}.
   *
   * @return subscription position
   */
  public String getPosition() {
    return mSubscribeRequest.getPosition();
  }

  /**
   * Sets a subscription {@code position} for the subscription request.
   *
   * @param position subscripttion position
   * @return SubscriptionConfig instance
   */
  public SubscriptionConfig setPosition(String position) {
    mSubscribeRequest.setPosition(position);
    return this;
  }

  /**
   * Sets the history {@code age} value to use in the subscription request.
   * <p>
   * To use this method, you also have to configure history settings for the channel in the
   * Developer Portal.
   *
   * @param age age value in seconds.
   * @return the current {@code SubscriptionConfig}.
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
   * To use this method, you also have to configure history settings for the channel in the
   * Developer Portal.
   *
   * @param count count value
   * @return the current {@code SubscriptionConfig}.
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
   * Sets a streamfilter value to use with a filter in a channel subscription.
   * <p>
   * A streamfilter is an SQL statement that selects and processes messages in the channel.
   *
   * @param filter filter string
   * @return the current {@code SubscriptionConfig}
   */
  public SubscriptionConfig setFilter(String filter) {
    mSubscribeRequest.setFilter(filter);
    return this;
  }

  /**
   * Sets the period of time, in seconds, that RTM runs the streamfilter on the channel before it
   * sends the result to the RTM client.
   *
   * @param period time period
   * @return the current {@code SubscriptionConfig}
   */
  public SubscriptionConfig setPeriod(Integer period) {
    mSubscribeRequest.setPeriod(period);
    return this;
  }

  /**
   * Called when a client application receives a subscription error from RTM.
   *
   * @param error subscription error
   * @return {@code true} if the error is not recoverable, otherwise {@code false}
   */
  protected boolean onError(SubscriptionError error) {
    return true;
  }

  /**
   * Creates a PDU body for a subscription request.
   * <p>
   * Override this method if custom resubscription behaviour is needed.
   *
   * @param subscriptionId {@code subscription_id} to subscribe to
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

  SubscriptionListener getUserListener() {
    return mUserListeners;
  }

  void onPosition(String position) {
    // method updates position from rtm replies
    // ignore position if it's not needed
    if (!mSubscriptionModes.contains(SubscriptionMode.TRACK_POSITION)) {
      return;
    }
    mSubscribeRequest.setPosition(position);
  }

  void onSuccessSubscribe(String position) {
    // reset all initial subscription settings
    mSubscribeRequest.setHistory(null);
    mSubscribeRequest.setPosition(null);
    // try to set new position received from subscribe reply
    onPosition(position);
  }
}
