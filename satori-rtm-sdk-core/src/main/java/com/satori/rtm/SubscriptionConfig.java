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
   * Creates a subscription configuration with a set of subscription modes and a subscription listener.
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
   * <p>
   * See the section "...with position" in the chapter "Subscribing" of <em>Satori Docs</em> for more information.
   *
   * @return current position for the subscription associated with this configuration
   */
  public String getPosition() {
    return mSubscribeRequest.getPosition();
  }

  /**
   * Sets the desired subscription {@code position} for a subscription request.
   * <p>
   * See the section "...with position" in the chapter "Subscribing" of <em>Satori Docs</em> for more information.
   *
   * @param position desired position for the subscription associated with this configuration
   * @return the current {@code SubscriptionConfig} object
   */
  public SubscriptionConfig setPosition(String position) {
    mSubscribeRequest.setPosition(position);
    return this;
  }

  /**
   * Sets the history {@code age} value to use in the subscription request.
   * <p>
   * To use this method, you also have to configure history settings for the channel in the
   * Dev Portal.
   *
   * @param age age value in seconds
   * @return the current {@code SubscriptionConfig} object
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
   * To use this method, you also have to configure channel history settings in the
   * Dev Portal.
   *
   * @param count count value
   * @return the current {@code SubscriptionConfig} object
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
   * Sets the streamfilter for the subscription.
   * <p>
   * A streamfilter is an SQL statement that selects and processes messages in the channel.
   * <p>
   * See the chapter "Views (formerly filters)" in the <em>Satori Docs</em> for more information
   *
   * @param filter filter string
   * @return the current {@code SubscriptionConfig}
   */
  public SubscriptionConfig setFilter(String filter) {
    mSubscribeRequest.setFilter(filter);
    return this;
  }

  /**
   * Configures subscription in the way to not send repeatable messages in subscription/data,
   * e.g. client will receive [1,2,1,3] from channel [1,1,2,2,2,1,3,3].
   *
   * @param only specify only option
   * @return the current {@code SubscriptionConfig} object
   */
  public SubscriptionConfig setOnly(String only) {
    mSubscribeRequest.setOnly(only);
    return this;
  }

  /**
   * Determines if this is a prefix subscription, i.e. subscribing to all channels that match the prefix
   * specified by "channel". Default value for prefix is false. In future if a new channel is created that
   * matches the prefix then client will be automatically subscribed to this new channel.
   *
   * @param prefix specify prefix option
   * @return the current {@code SubscriptionConfig} object
   */
  public SubscriptionConfig setPrefix(Boolean prefix) {
    mSubscribeRequest.setPrefix(prefix);
    return this;
  }

  /**
   * Sets the period of time, in seconds, that RTM runs the streamfilter on the channel before it
   * sends the result to the RTM client.
   * See the chapter "Views (formerly filters)" in the <em>Satori Docs</em> for more information
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
   * @return {@code true} if the error isn't recoverable, otherwise {@code false}
   */
  protected boolean onError(SubscriptionError error) {
    return true;
  }

  /*
   * Creates a PDU body for a subscription request.
   * Override this method if custom re-subscription behaviour is needed.
   *
   */
  protected SubscribeRequest createSubscribeRequest(String subscriptionId) {
    SubscribeRequest request = new SubscribeRequest();
    Boolean emptyFilter = Strings.isNullOrEmpty(mSubscribeRequest.getFilter());
    Boolean prefixSubscription =  mSubscribeRequest.getPrefix();
    if (emptyFilter || prefixSubscription != null && prefixSubscription) {
      request.setChannel(subscriptionId);
    }
    if (!emptyFilter || prefixSubscription != null && prefixSubscription) {
      request.setSubscriptionId(subscriptionId);
    }
    request.setPosition(mSubscribeRequest.getPosition());
    request.setHistory(mSubscribeRequest.getHistory());
    request.setFastForward(mSubscriptionModes.contains(SubscriptionMode.FAST_FORWARD));
    request.setFilter(mSubscribeRequest.getFilter());
    request.setPeriod(mSubscribeRequest.getPeriod());
    request.setOnly(mSubscribeRequest.getOnly());
    request.setPrefix(mSubscribeRequest.getPrefix());
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
