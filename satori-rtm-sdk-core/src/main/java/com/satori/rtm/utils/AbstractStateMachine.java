package com.satori.rtm.utils;

public abstract class AbstractStateMachine<T extends State> {
  protected T mState;

  public T getState() {
    return mState;
  }

  public void setState(T newState) {
    this.mState = newState;
  }

  public String getName() {
    return getClass().getSimpleName();
  }
}
