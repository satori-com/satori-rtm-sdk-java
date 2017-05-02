package com.satori.rtm.connection;

public class StaticJsonBinder {
  public static Serializer createSerializer() {
    return new JacksonSerializer();
  }
}
