package com.satori.rtm.model;

/**
 * An interface representing any JSON element.
 */
public interface AnyJson {
  /**
   * Deserializes the JSON element into an object of the specified type.
   * <p>
   * It is not suitable to use if the specified class is a generic type since it
   * will not have the generic type information because of the Type Erasure feature of Java. This
   * method works fine if the any of the fields of the specified object are generics, just the
   * object itself should not be a generic type.
   */
  <T> T convertToType(final Class<T> clazz);
}
