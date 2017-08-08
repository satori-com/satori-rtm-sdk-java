package com.satori.rtm.model;

/**
 * A Protocol Data Unit (PDU) with a typeless user payload.
 * <p>
 * This class stores general PDU structure without knowledge about the payload type.
 * The payload is stored as raw JSON and converted to specific class type on demand.
 * <p>
 * <strong>Sample PDU Specification</strong>
 * <pre>{@literal
 * {
 *    "action": "<service>/<operation>/<response>",
 *    "body": {
 *         "channel": "<channel name>",
 *         "message": "<JSON-encoded message>" // payload
 *    },
 *    "id": <PDU id> | "<PDU id>"
 * }}
 * </pre>
 */
public class PduRaw extends Pdu<AnyJson> {
  public PduRaw() { }

  public PduRaw(String action, AnyJson anyJson) {
    this(action, anyJson, null);
  }

  public PduRaw(String action, AnyJson anyJson, String id) {
    super(action, anyJson, id);
  }

  public <T> Pdu<T> convertBodyTo(Class<T> clazz) {
    AnyJson body = getBody();
    T typedBody = (null != body) ? body.convertToType(clazz) : null;
    return new Pdu<T>(getAction(), typedBody, getId()) {
      @Override
      public String toString() {
        return PduRaw.this.toString();
      }
    };
  }

  @Override
  public String toString() {
    return "{" +
        "\"action\":\"" + getAction() + "\"," +
        "\"id\":\"" + getId() + "\"," +
        "\"body\":" + getBody() +
        "}";
  }
}
