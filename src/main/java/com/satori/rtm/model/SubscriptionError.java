package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (PDU) for a subscription-specific error.
 * <p>
 * The {@code subscription_id} field specifies the subscription_id with the error and the {@code error}
 * field specifies the unique identifier for the error.
 * <p>
 * The {@code error_text}
 * field contains text that describes the error. This field is variable and
 * may change in the future, and should not be parsed. You can use this text, for example,
 * to include in error log files.
 * <p>
 * The following are specific {@code SubscriptionError} errors:
 * <ul>
 * <li>{@code out_of_sync} error as an unsolicited error</li>
 * </ul>
 * <p>
 * For more information on error messages from the RTM Service, see the <em>RTM API Reference</em>.
 *
 * <pre>{@literal
 * {
 *    "subscription_id": string(),
 *    "error": string(),
 *    "error_text": text(),
 *    "missed_message_count": count()
 * }}
 * </pre>
 */
public class SubscriptionError extends CommonError {
  private String subscription_id;
  private String position;
  private Integer missed_message_count;

  public SubscriptionError() {
  }

  public SubscriptionError(String subscriptionId, String code, String message) {
    super(code, message);
    this.subscription_id = subscriptionId;
  }

  public String getSubscriptionId() {
    return subscription_id;
  }

  public String getPosition() {
    return position;
  }

  public Integer getMissedMessageCount() {
    return missed_message_count;
  }
}
