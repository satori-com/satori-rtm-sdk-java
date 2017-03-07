package com.satori.rtm.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class State<T extends AbstractStateMachine> {
  private static final Logger LOG = LoggerFactory.getLogger(State.class);

  public void enter(T context) {
  }

  public void leave(T context) {
  }

  @SuppressWarnings("unchecked")
  public void transition(T context, State<T> newState) {
    LOG.info("{} performing transition from {} to {}", context.getName(), context.getState(),
        newState);
    if (null != context.getState()) {
      (context.getState()).leaveSafe(context);
    }
    context.setState(newState);
    (context.getState()).enterSafe(context);
  }

  protected void logUnexpectedStateAction(T context, String action) {
    LOG.error("{} detects unexpected action {} in state {}", context.getName(), action,
        context.getState());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  private void enterSafe(T context) {
    try {
      enter(context);
    } catch (Exception ex) {
      String msg = String.format("Unable enter to state %s", this.getClass().getSimpleName());
      LOG.error(msg, ex);
    }
  }

  private void leaveSafe(T context) {
    try {
      leave(context);
    } catch (Exception ex) {
      String msg = String.format("Unable leave from state %s", this.getClass().getSimpleName());
      LOG.error(msg, ex);
    }
  }
}
