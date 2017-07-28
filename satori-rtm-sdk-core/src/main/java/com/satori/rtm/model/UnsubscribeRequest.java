package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (<strong>PDU</strong>) for an unsubscribe request.
 * <p>
 * The PDU has the following basic structure:
 * <pre>{@literal
 * {
 *     "action": "rtm/unsubscribe",
 *     "body": {
 *         "subscription_id": ChannelName
 *     },
 *     "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class UnsubscribeRequest {
  private String subscription_id;

  public UnsubscribeRequest() {
  }

  public UnsubscribeRequest(String subscriptionId) {
    this.subscription_id = subscriptionId;
  }

  public String getSubscriptionId() {
    return subscription_id;
  }
}
