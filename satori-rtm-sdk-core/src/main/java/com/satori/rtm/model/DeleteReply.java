package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (<strong>PDU</strong>) for a positive reply to a delete request.
 * <p>
 * The PDU has the following basic structure for a positive reply:
 * <pre>{@literal
 * {
 *    "action": "rtm/delete/ok",
 *    "body": {
 *        "position": ChannelStreamPosition
 *    },
 *    "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class DeleteReply {
  private String position;

  private PreviousMessage previous;

  public DeleteReply() {
  }

  public PreviousMessage getPrevious() {
    return previous;
  }

  public String getPosition() {
    return position;
  }
}
