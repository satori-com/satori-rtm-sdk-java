package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (<strong>PDU</strong>) for a subscribe request.
 * <p>
 * The PDU could have the following basic structures:
 * <pre>{@literal
 * {
 *    "action": "rtm/subscribe",
 *    "body": {
 *         "filter": fSQL,
 *         "subscription_id": SubscriptionId,
 *         "force": Boolean, OPTIONAL
 *         "fast_forward": Boolean, OPTIONAL
 *         "period": Count, OPTIONAL
 *         "position": ChannelStreamPosition, OPTIONAL
 *         "history": {
 *             "count" : Count, Non-negative integer, OPTIONAL
 *             "age"   : Age, Non-negative integer, OPTIONAL
 *         }
 *    },
 *    "id": RequestId OPTIONAL
 * }}
 * </pre>
 * <pre>{@literal
 * {
 *    "action": "rtm/subscribe",
 *    "body": {
 *         "channel": Channel,
 *         "force": Boolean, OPTIONAL
 *         "fast_forward": Boolean, OPTIONAL
 *         "position": ChannelStreamPosition, OPTIONAL
 *         "history": {
 *             "count" : Count, Non-negative integer, OPTIONAL
 *             "age"   : Age, Non-negative integer, OPTIONAL
 *         }
 *    },
 *    "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class SubscribeRequest {
  private String channel;
  private String subscription_id;
  private String position;
  private History history;
  private Boolean fast_forward;
  private String filter;
  private Integer period;

  public SubscribeRequest() { }

  public SubscribeRequest(String channel, String position) {
    this.channel = channel;
    this.position = position;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getPosition() {
    return position;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  public History getHistory() {
    return history;
  }

  public void setHistory(History history) {
    this.history = history;
  }

  public String getSubscriptionId() {
    return subscription_id;
  }

  public void setSubscriptionId(String subscriptionId) {
    this.subscription_id = subscriptionId;
  }

  public void setFastForward(Boolean fastForward) {
    this.fast_forward = fastForward;
  }

  public Boolean isFastForward() {
    return fast_forward;
  }

  public String getFilter() {
    return filter;
  }

  public void setFilter(String filter) {
    this.filter = filter;
  }

  public Integer getPeriod() {
    return period;
  }

  public void setPeriod(Integer period) {
    this.period = period;
  }

  static public class History {
    private Integer age;

    private Integer count;

    public History() { }

    public Integer getAge() {
      return age;
    }

    public void setAge(Integer age) {
      this.age = age;
    }

    public Integer getCount() {
      return count;
    }

    public void setCount(Integer maxCount) {
      this.count = maxCount;
    }
  }
}

