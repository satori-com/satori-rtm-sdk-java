package com.satori.rtm.model;

/**
 * Represents the body Protocol Data Unit (PDU) for a positive reply to a subscribe request.
 * <p>
 * The PDU has the following basic structure for a positive reply:
 * <pre>{@literal
 * {
 *    "action": "rtm/subscribe/ok",
 *    "body": {
 *       "position": ChannelStreamPosition
 *       "subscription_id": SubscriptionId
 *    },
 *    "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class SubscribeReply {
  private String subscription_id;
  private String position;

  public SubscribeReply() {
  }

  public SubscribeReply(String subscriptionId, String position) {
    this.subscription_id = subscriptionId;
    this.position = position;
  }

  public String getSubscriptionId() {
    return subscription_id;
  }

  public String getPosition() {
    return position;
  }
}
