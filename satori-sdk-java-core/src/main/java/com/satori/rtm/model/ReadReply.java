package com.satori.rtm.model;

/**
 * Represents the body of a read reply Protocol Data Unit (PDU).
 * <p>
 * The PDU has the following basic structure for a positive response:
 * <pre>{@literal
 * {
 *    "action": "rtm/read/ok",
 *    "body": {
 *        "position": ChannelStreamPosition,
 *        "message": JSONValue
 *    },
 *    "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class ReadReply {
  private String position;
  private AnyJson message;

  public ReadReply() {
  }

  public String getPosition() {
    return position;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  public AnyJson getMessage() {
    return message;
  }

  public void setMessage(AnyJson message) {
    this.message = message;
  }

  public <T> T getMessageAsType(final Class<T> clazz) {
    if (null == message) {
      return null;
    }
    return message.convertToType(clazz);
  }
}
