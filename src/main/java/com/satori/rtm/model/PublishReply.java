package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (PDU) for a positive reply to a publish request.
 * <p>
 * The PDU has the following basic structure for a positive response:
 * <pre>{@literal
 * {
 *    "action": "rtm/publish/ok",
 *    "body": {
 *        "position": ChannelStreamPosition
 *    },
 *    "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class PublishReply {
  private String position;

  public PublishReply() {
  }

  public String getPosition() {
    return position;
  }
}
