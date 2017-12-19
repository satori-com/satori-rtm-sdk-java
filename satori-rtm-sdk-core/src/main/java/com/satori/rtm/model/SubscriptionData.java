package com.satori.rtm.model;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.util.List;

/**
 * Represents the body value of a PDU for a subscription data.
 * <p>
 * Subscription data PDUs contain messages published to a subscribed channel. If the
 * subscription uses a streamview, the messages may be filtered and transformed. A single
 * {@code SubscriptionData} can contain multiple messages.
 * <p>
 * Subscription data PDUs have the following structure:
 * <pre>{@literal
 * {
 *     "action": "rtm/subscription/data",
 *     "body": {
 *         "position":Position,
 *         "messages":[Message],
 *         "subscription_id":SubscriptionId
 *     }
 * }}
 * </pre>
 */
public class SubscriptionData {
  private final String subscription_id;
  private final String position;
  private final List<AnyJson> messages;
  private final String channel;

  public SubscriptionData() {
    this(null, null, null, null);
  }

  public SubscriptionData(String id, String position, List<AnyJson> messages, String channel) {
    this.subscription_id = id;
    this.position = position;
    this.messages = messages;
    this.channel = channel;
  }

  /**
   * Returns the subscription id to which the messages are published.
   *
   * @return Subscription id.
   **/
  public String getSubscriptionId() {
    return subscription_id;
  }

  /**
   * Returns the channel to which the messages are published (if using "prefix" subscription).
   *
   * @return Channel name.
   **/
  public String getChannel() {
    return channel;
  }

  /**
   * Returns the {@code position} value for the message stream position to which the message or
   * messages are published.
   *
   * @return String {@code position} value.
   **/
  public String getPosition() {
    return position;
  }

  /**
   * Returns the messages as a string collection.
   *
   * @return Collection of messages as a {@literal List<String>}.
   **/
  public List<String> getMessagesAsStrings() {
    return getMessagesAsType(String.class);
  }

  /**
   * Converts the messages to specific class type and returns the result as a
   * {@literal List<class type>}. This method casts the messages to the class type provided.
   *
   * @param clazz Class instance of specific type.
   * @param <T>   Class type to convert to.
   * @return Collection of messages which are casted to the provided type
   */
  public <T> List<T> getMessagesAsType(final Class<T> clazz) {
    return Lists.transform(messages, new Function<AnyJson, T>() {
      @Override
      public T apply(AnyJson input) {
        if (null == input) {
          return null;
        }
        return input.convertToType(clazz);
      }
    });
  }

  /**
   * @return Messages {@literal Iterable<class type>} of abstract {@code AnyJson} type.
   **/
  public Iterable<AnyJson> getMessages() {
    return messages;
  }

  @Override
  public String toString() {
    return "{" +
        "\"subscription_id\":\"" + subscription_id + "\"," +
        "\"position\":\"" + position + "\"," +
        "\"messages\":[" + Joiner.on(",").join(messages) + "]" +
        "}";
  }
}
