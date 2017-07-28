package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (<strong>PDU</strong>) for a positive reply to an unsubscribe request.
 * <p>
 * The PDU has the following basic structure for a positive reply:
 * <pre>{@literal
 * {
 *     "action": "rtm/unsubscribe/ok",
 *     "body": {
 *         "position": ChannelStreamPosition
 *     },
 *     "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class UnsubscribeReply {
  private String position;

  public UnsubscribeReply() {
  }

  public UnsubscribeReply(String position) {
    this.position = position;
  }

  public String getPosition() {
    return position;
  }
}
