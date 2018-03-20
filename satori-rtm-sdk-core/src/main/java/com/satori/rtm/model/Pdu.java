package com.satori.rtm.model;

/**
 * Represents a Protocol Data Unit (<strong>PDU</strong>).
 * <p>
 * A PDU contains JSON written with an RTM-specific syntax. Clients and RTM communicate with each
 * other using information in PDUs. Each PDU is encoded into a single WebSocket frame.
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
 * @param <TBody> type of the user-specified payload
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
   * Returns the body of the PDU. The body is the {@code body} element in the PDU.
   *
   * @return PDU body, or {@code null} if the PDU doesn't contain a payload.
   */
  public TBody getBody() {
    return body;
  }

  /**
   * Returns the action element of the PDU.
   *
   * @return a String containing the action
   */
  public String getAction() {
    return action;
  }

  /**
   * Returns the identifier ({@code id}) element of the PDU.
   *
   * @return a string containing the id
   */
  public String getId() {
    return id;
  }

  /**
   * Returns {@code true} if the PDU is an unsolicited PDU from RT and {@code false} otherwise.
   * <p>
   * An unsolicited PDU doesn't have an identifier ({@code id}) and isn't a response
   * to a request from the client. It either contains a message published to the channel, or it's an error PDU
   * sent by RTM.
   *
   * @return returns {@code true} if the PDU is unsolicited, otherwise {@code false}.
   */
  public boolean isUnsolicited() {
    return null == id;
  }

  /**
   * Returns {@code true} if the PDU is a system error from RTM and {@code false}
   * otherwise. This type of error usually indicates an internal error within RTM.
   * <p>
   * Respond to this type of error by disconnecting and reconnecting the client.
   *
   * @return returns {@code true} if the PDU is a system error, otherwise {@code false}.
   */
  public boolean isSystemError() {
    return action.equals("/error");
  }


  /**
   * Returns {@code true} if the PDU is a write error from RTM and {@code false}
   * otherwise. This type of error usually indicates a position error with the write request.
   *
   * @return returns {@code true} if the PDU is a write error, otherwise {@code false}.
   */
  public boolean isWriteError() {
    return action.equals("rtm/write/error");
  }

  /**
   * Returns {@code true} if the PDU is a positive response to a request
   * sent by the application, and {@code false} otherwise. The {@code action} element of a positive
   * response PDU ends with the string "/ok", for example, {@code "rtm/publish/ok"}.
   *
   * @return returns {@code true} if the PDU is a positive response, otherwise {@code false}.
   */
  public boolean isOkOutcome() {
    return action.endsWith("/ok");
  }

  /**
   * Returns {@code true} if the PDU is a negative response to a request
   * sent by the application, and {@code false} otherwise. The {@code action} element of a negative
   * response PDU ends with the string "/error", for example, {@code "rtm/publish/error"}.
   *
   * @return returns {@code true} if the PDU is a negative response, otherwise {@code false}.
   */
  public boolean isErrorOutcome() {
    return action.endsWith("/error");
  }

  /**
   * Returns {@code true} if the PDU is a chunk response sent by the application,
   * and {@code false} otherwise.
   *
   * @return returns {@code true} if the PDU is a chunk response, otherwise {@code false}.
   */
  public boolean isChunkResponse() {
    return action.endsWith("/data");
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
