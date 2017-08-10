package com.satori.rtm.model;

/**
 * Defines a method that converts any JSON into an object of a specified type.
 */
public interface AnyJson {
  /**
   * Deserializes a JSON element into an object of the specified type.
   * <p>
   * The type {@code T} can't be a generic type, because Java type erasure removes generic type
   * information during compilation. However, fields <em>in</em> the specified type can have
   * generic types.
   */
  <T> T convertToType(final Class<T> clazz);
}
