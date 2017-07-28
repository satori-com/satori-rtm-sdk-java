package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (<strong>PDU</strong>) for a publish request.
 * <p>
 * The PDU has the following basic structure:
 * <pre>{@literal
 * {
 *    "action": "rtm/publish",
 *    "body": {
 *         "channel": ChannelName,
 *         "message": JSONValue
 *    },
 *    "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class PublishRequest<T> {
  private String channel;

  private T message;

  public PublishRequest() {
  }

  public PublishRequest(String channel, T message) {
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
