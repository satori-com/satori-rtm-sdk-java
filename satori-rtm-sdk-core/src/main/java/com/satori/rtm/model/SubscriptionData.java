package com.satori.rtm.model;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.util.List;

/**
 * Represents a message coming from RTM.
 * <p>
 * A Subscription Data PDU delivers channel messages (possibly filtered and transformed messages in
 * case of a viewed subscription). A single Subscription Data PDU can contain multiple messages
 * grouped in the array messages field.
 * <p>
 * The PDU has the following structure:
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

  public SubscriptionData() {
    this(null, null, null);
  }

  public SubscriptionData(String id, String position, List<AnyJson> messages) {
    this.subscription_id = id;
    this.position = position;
    this.messages = messages;
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
