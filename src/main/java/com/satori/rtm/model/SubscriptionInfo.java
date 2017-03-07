package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (PDU) for subscription_id-specific notifications.
 * <p>
 * For example, the {@code SubscriptionInfo} can include information about the fast-forward feature.
 * <p>
 * The {@code subscription_id} field specifies the subscription_id and the {@code info}
 * field specifies the unique identifier for the notification.
 * <p>
 * The {@code reason}
 * field contains text that describes the notification. This field is variable and
 * may change in the future, and should not be parsed. You can use this text, for example,
 * to include in log files.
 * <p>
 * For more information on informational messages from the RTM Service, see the <em>RTM API Reference</em>.
 *
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
