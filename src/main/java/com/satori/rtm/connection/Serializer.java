package com.satori.rtm.connection;

import com.satori.rtm.model.InvalidJsonException;
import com.satori.rtm.model.PduRaw;

/**
 * General interface for JSON serializer.
 */
public interface Serializer {
  /**
   * Serializes any object into a JSON string.
   *
   * @param obj An object.
   * @return A JSON string.
   */
  String toJson(final Object obj);

  /**
   * Deserializes a JSON string into an untyped Protocol Data Unit (PDU).
   *
   * @param json a JSON string.
   * @return An untyped PDU.
   * @throws InvalidJsonException when a JSON string has a malformed format
   */
  PduRaw parsePdu(String json) throws InvalidJsonException;
}
