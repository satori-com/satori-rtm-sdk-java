package com.satori.rtm.model;

public class PreviousMessage<T> {

  private T message;
  private String position;


  public T getMessage() {
    return message;
  }

  public String getPosition() {
    return position;
  }
}
