package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (<strong>PDU</strong>) for a write request.
 * <p>
 * The PDU has the following basic structure:
 * <pre>{@literal
 * {
 *     "action": "rtm/write",
 *     "body": {
 *         "channel": ChannelName,
 *         "message": Message,
 *         "position": Position OPTIONAL
 *     },
 *     "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class WriteRequest<T> {
  private String channel;
  private T message;
  private String position;

  public WriteRequest() { }

  public WriteRequest(String channel, T message) {
    this(channel, message, null);
  }

  public WriteRequest(String channel, T message, String position) {
    this.channel = channel;
    this.message = message;
    this.position = position;
  }

  public String getChannel() {
    return channel;
  }

  public T getMessage() {
    return message;
  }

  public String getPosition() {
    return position;
  }
}
