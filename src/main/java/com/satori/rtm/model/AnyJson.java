package com.satori.rtm.model;

public interface AnyJson {
  <T> T convertToType(final Class<T> clazz);
}
