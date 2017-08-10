package com.satori.rtm.model;

/**
 * Represents a Protocol Data Unit (PDU) with a body as raw JSON.
 * <p>
 * This class stores PDU when type of {@code body} is not known yet. The method
 * {@link #convertBodyTo(Class)  convertBodyTo()} converts the {@code body} to a specific type.
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
