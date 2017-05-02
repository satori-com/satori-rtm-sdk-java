package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (PDU) for a positive reply to a write request.
 * <p>
 * The PDU has the following basic structure for a positive reply:
 * <pre>{@literal
 * {
 *    "action": "rtm/write/ok",
 *    "body": {
 *        "position": ChannelStreamPosition
 *    },
 *    "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class WriteReply {
  private String position;

  public WriteReply() {
  }

  public String getPosition() {
    return position;
  }
}
