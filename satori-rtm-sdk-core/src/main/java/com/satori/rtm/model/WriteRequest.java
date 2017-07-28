package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (<strong>PDU</strong>) for a write request.
 * <p>
 * The PDU has the following basic structure:
 * <pre>{@literal
 * {
 *     "action": "rtm/write",
 *     "body": {
 *         "channel": ChannelName
 *         "message": Message
 *     },
 *     "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class WriteRequest<T> {
  private String channel;
  private T message;


  public WriteRequest() { }

  public WriteRequest(String channel, T message) {
    this.channel = channel;
    this.message = message;
  }

  public String getChannel() {
    return channel;
  }

  public T getMessage() {
    return message;
  }
}
