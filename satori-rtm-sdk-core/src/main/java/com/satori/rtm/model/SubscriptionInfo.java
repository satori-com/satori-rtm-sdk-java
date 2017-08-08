package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (PDU) for subscription-specific notification.
 * <p>
 * A Subscription Info PDU contains information about a subscription, which includes the type of
 * info and the reason why RTM sent it. The subscription is not terminated after receiving a
 * notification.
 * <p>
 * The PDU has the following structure:
 * <pre>{@literal
 * {
 *    "subscription_id": string(),
 *    "info": string(),
 *    "reason": text()
 *    "position": string(),
 *    "missed_message_count": count()
 * }}
 * </pre>
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
