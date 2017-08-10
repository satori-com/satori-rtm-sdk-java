package com.satori.rtm.model;

/**
 * Represents the body value of a PDU for a subscription info.
 *
 * {@code SubscriptionInfo} objects provide information about a subscription, including the type of
 * info and the reason why RTM sent the info.
 * <p>
 * RTM doesn't terminate your subscription after it sends a subscription info.
 */
public class SubscriptionInfo {
  private String subscription_id;
  private String info;
  private String reason;
  private String position;
  private Integer missed_message_count;

  public SubscriptionInfo() {
  }

  public SubscriptionInfo(
      String subscription_id,
      String info,
      String reason,
      String position) {
    this.subscription_id = subscription_id;
    this.info = info;
    this.reason = reason;
    this.position = position;
  }

  public String getSubscriptionId() {
    return subscription_id;
  }

  public String getInfo() {
    return info;
  }

  public String getPosition() {
    return position;
  }

  public String getReason() {
    return reason;
  }

  public Integer getMissedMessageCount() {
    return missed_message_count;
  }
}
