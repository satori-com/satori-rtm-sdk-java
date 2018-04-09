package com.satori.rtm.model;

import java.util.Objects;

public class PreviousMessage<T> {

  private T message;
  private String position;


  public T getMessage() {
    return message;
  }

  public String getPosition() {
    return position;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PreviousMessage<?> that = (PreviousMessage<?>) o;
    return Objects.equals(message, that.message) &&
        Objects.equals(position, that.position);
  }

  @Override
  public int hashCode() {

    return Objects.hash(message, position);
  }
}
