package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (PDU) for a subscription-specific error.
 * <p>
 * A Subscription Error PDU notifies of subscription termination (forceful unsubscription) due to a
 * subscription-related error.
 * <p>
 * The PDU has the following structure:
 * <pre>{@literal
 * {
 *    "subscription_id": string(),
 *    "error": string(),
 *    "reason": text(),
 *    "position": string(),
 *    "missed_message_count": count()
 * }}
 * </pre>
 */
public class SubscriptionError extends CommonError {
  private String subscription_id;
  private String position;
  private Integer missed_message_count;

  public SubscriptionError() {
  }

  public SubscriptionError(String subscriptionId, String code, String message) {
    super(code, message);
    this.subscription_id = subscriptionId;
  }

  public String getSubscriptionId() {
    return subscription_id;
  }

  public String getPosition() {
    return position;
  }

  public Integer getMissedMessageCount() {
    return missed_message_count;
  }
}
