package com.satori.rtm.model;

/**
 * Represents a Protocol Data Unit (PDU). A PDU is a JSON-encoded message sent in a separate WebSocket Frame.
 * The PDU contains system specific information as well as the user-specified payload.
 * <p>
 * In the example below, the user-specified payload is the {@code message} element.
 * <p>
 * <strong>Sample PDU Specification</strong>
 * <pre>{@literal
 * {
 *    "action": "<service>/<operation>/<outcome>",
 *    "body": {
 *         "channel": ChannelName,
 *         "message": JSONValue
 *    },
 *    "id": RequestId OPTIONAL
 * }}
 * </pre>
 *
 * @param <TBody> User-specified payload.
 */
public class Pdu<TBody> {
  private final String action;
  private final String id;
  private final TBody body;

  public Pdu() {
    this(null, null);
  }

  public Pdu(String action, TBody body, String id) {
    this.action = action;
    this.id = id;
    this.body = body;
  }

  public Pdu(String action, TBody body) {
    this(action, body, null);
  }

  /**
   * Returns the body of the PDU. The body is the {@code message} element in the PDU.
   *
   * @return PDU body. Could be null.
   */
  public TBody getBody() {
    return body;
  }

  /**
   * Returns the action element of the PDU.
   *
   * @return Action
   */
  public String getAction() {
    return action;
  }

  /**
   * Returns the identifier ({@code id}) element of the PDU.
   *
   * @return Identifier
   */
  public String getId() {
    return id;
  }

  /**
   * Returns {@code true} if the PDU is an unsolicited PDU from the RTM Service
   * and {@code false} otherwise.
   * <p>
   * An unsolicited PDU does not have an identifier ({@code id}) and is not a response
   * to a request from the client. It is either a message published to a channel
   * by another application or an error from the RTM Service.
   *
   * @return Returns {@code true} if the PDU is unsolicited; {@code false} otherwise.
   */
  public boolean isUnsolicited() {
    return null == id;
  }

  /**
   * Returns {@code true} if the PDU is a system error from the RTM Service and {@code false}
   * otherwise. This type of error generally indicates an internal error with the RTM Service.
   * <p>
   * In addition, if this type of error is received, the client should disconnect and reconnect
   * the WebSocket connection and the RTM Service may disconnect.
   *
   * @return Returns {@code true} if the PDU is a system error; {@code false} otherwise.
   */
  public boolean isSystemError() {
    return action.equals("/error");
  }

  /**
   * Returns {@code true} if the PDU is a positive response to a request
   * sent by the application and {@code false} otherwise. The {@code action} element of a positive
   * response PDU ends with {@code /ok}, for example, {@code 'rtm/publish/ok'}.
   *
   * @return Returns {@code true} if the PDU is a positive response; {@code false} otherwise.
   */
  public boolean isOkOutcome() {
    return action.endsWith("/ok");
  }

  /**
   * Returns {@code true} if the PDU is a negative response to a request
   * sent by the application and {@code false} otherwise. The {@code action} element of a negative
   * response PDU ends with {@code /error}, for example, {@code 'rtm/publish/error'}.
   *
   * @return Returns {@code true} if the PDU is a positive response; {@code false} otherwise.
   */
  public boolean isErrorOutcome() {
    return action.endsWith("/error");
  }


  @Override
  public String toString() {
    return "{" +
        "\"id\":\"" + id + "\"," +
        "\"action\":\"" + action + "\"," +
        "\"body\":" + body.toString() +
        "}";
  }
}
