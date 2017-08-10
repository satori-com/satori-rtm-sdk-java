package com.satori.rtm.model;

/**
 * Represents the body value of a PDU for a subscription error.
 * <p>
 * A subscription error PDU notifies you that RTM has terminated your subscription (a forced
 * unsubscription) because of a subscription-related error.
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
