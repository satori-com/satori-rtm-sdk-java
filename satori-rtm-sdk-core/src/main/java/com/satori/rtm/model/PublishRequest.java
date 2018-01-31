package com.satori.rtm.model;

import com.google.common.base.Preconditions;

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

  //TODO: Use separate generic type for the TTL message, in case message types are different.
  private T ttl_message;

  private Long ttl;

  public PublishRequest() { }

  public PublishRequest(String channel, T message) {
    this.channel = channel;
    this.message = message;
  }

  public PublishRequest(String channel, T message, final long ttl, final T ttl_message) {
    Preconditions.checkArgument(ttl > 0, String.format("ttl must be non negative: %s", ttl));
    this.channel = channel;
    this.message = message;
    this.ttl = ttl;
    this.ttl_message = ttl_message;
  }

  public String getChannel() {
    return channel;
  }

  public T getMessage() {
    return message;
  }

  public T getTtlMessage() {
    return ttl_message;
  }

  public Long getTtl() {
    return ttl;
  }

  public PublishRequest<T> withTtl(final long newTtl, final T newTtlMessage) {
    return new PublishRequest<T>(channel, message, newTtl, newTtlMessage);
  }

  @Override
  public String toString() {
    return "PublishRequest{" +
        "channel='" + channel + '\'' +
        ", message=" + message +
        ", ttl=" + ttl +
        ", ttlMessage=" + ttl_message +
        '}';
  }
}
