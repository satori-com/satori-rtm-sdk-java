package com.satori.rtm;

import com.satori.rtm.connection.Connection;
import com.satori.rtm.utils.AbstractStateMachine;
import com.satori.rtm.utils.State;
import java.util.concurrent.Future;


abstract class AbstractSubscription
    extends AbstractStateMachine<AbstractSubscription.AbstractState> {

  static final AbstractState SUBSCRIBING = new Subscribing();
  static final AbstractState UNSUBSCRIBING = new Unsubscribing();
  static final AbstractState SUBSCRIBED = new Subscribed();
  static final AbstractState UNSUBSCRIBED = new Unsubscribed();
  static final AbstractState FAILED = new Failed();

  private final RtmService mRtmService;
  Mode mMode;
  private Future<?> mRequest = null;

  AbstractSubscription(RtmService rtmService) {
    this.mRtmService = rtmService;
    this.mMode = Mode.LINKED;
    this.mState = UNSUBSCRIBED;
  }

  protected abstract Future<?> doSubscribeRequest(Connection connection);

  protected abstract Future<?> doUnsubscribeRequest(Connection connection);

  protected abstract void onEnterFailed();

  protected abstract void onLeaveFailed();

  protected abstract void onEnterSubscribed();

  protected abstract void onLeaveSubscribed();

  protected abstract void onEnterSubscribing();

  protected abstract void onLeaveSubscribing();

  protected abstract void onEnterUnsubscribed();

  protected abstract void onLeaveUnsubscribed();

  protected abstract void onEnterUnsubscribing();

  protected abstract void onLeaveUnsubscribing();

  protected abstract void dispose();

  RtmService getRtmService() {
    return this.mRtmService;
  }

  public void unsubscribe() {
    mMode = Mode.UNLINKED;
    getState().checkModeTransition(this);
  }

  public void enterStartState() {
    getState().enter(this);
  }

  public void onConnected() {
    if (getState() == UNSUBSCRIBED && Mode.LINKED == mMode) {
      getState().transition(this, SUBSCRIBING);
    }
  }

  public void onDisconnected() {
    if (getState() != UNSUBSCRIBED) {
      getState().transition(this, UNSUBSCRIBED);
    }
  }

  enum Mode {
    LINKED, UNLINKED, CYCLE
  }

  private static class Unsubscribed extends AbstractState {
    @Override
    public void enter(AbstractSubscription context) {
      if (Mode.CYCLE == context.mMode) {
        context.mMode = Mode.LINKED;
      }
      context.onEnterUnsubscribed();
      checkModeTransition(context);
    }

    @Override
    public void leave(final AbstractSubscription context) {
      context.onLeaveUnsubscribed();
    }

    @Override
    protected void checkModeTransition(AbstractSubscription context) {
      RtmService rtmService = context.getRtmService();
      if (Mode.LINKED == context.mMode && rtmService.isConnected()) {
        transition(context, SUBSCRIBING);
      }
      if (Mode.UNLINKED == context.mMode) {
        context.dispose();
      }
    }
  }

  private static class Subscribing extends AbstractState {
    @Override
    public void enter(final AbstractSubscription context) {
      final RtmService service = context.getRtmService();
      if (service.isConnected()) {
        Connection connection = service.getConnection();
        context.mRequest = context.doSubscribeRequest(connection);
      } else {
        context.onDisconnected();
      }
      context.onEnterSubscribing();
    }

    @Override
    public void leave(AbstractSubscription context) {
      if (null != context.mRequest) {
        context.mRequest.cancel(true);
        context.mRequest = null;
      }
      context.onLeaveSubscribing();
    }

    @Override
    protected void checkModeTransition(AbstractSubscription context) { }
  }

  private static class Subscribed extends AbstractState {
    @Override
    public void enter(AbstractSubscription context) {
      context.onEnterSubscribed();
      checkModeTransition(context);
    }

    @Override
    public void leave(AbstractSubscription context) {
      context.onLeaveSubscribed();
    }

    @Override
    protected void checkModeTransition(AbstractSubscription context) {
      if ((Mode.UNLINKED == context.mMode) || (Mode.CYCLE == context.mMode)) {
        transition(context, UNSUBSCRIBING);
      }
    }
  }

  private static class Unsubscribing extends AbstractState {
    @Override
    public void enter(final AbstractSubscription context) {
      final RtmService service = context.getRtmService();
      if (service.isConnected()) {
        Connection connection = service.getConnection();
        context.mRequest = context.doUnsubscribeRequest(connection);
      } else {
        context.onDisconnected();
      }
      context.onEnterUnsubscribing();
    }

    @Override
    public void leave(AbstractSubscription context) {
      if (null != context.mRequest) {
        context.mRequest.cancel(true);
        context.mRequest = null;
      }
      context.onLeaveUnsubscribing();
    }

    @Override
    protected void checkModeTransition(AbstractSubscription context) { }
  }

  private static class Failed extends AbstractState {
    @Override
    public void enter(AbstractSubscription context) {
      context.onEnterFailed();
      checkModeTransition(context);
    }

    @Override
    public void leave(AbstractSubscription context) {
      context.onLeaveFailed();
    }

    @Override
    protected void checkModeTransition(AbstractSubscription context) {
      if (Mode.UNLINKED == context.mMode) {
        transition(context, UNSUBSCRIBED);
      }
    }
  }

  static class AbstractState extends State<AbstractSubscription> {
    protected void checkModeTransition(AbstractSubscription context) {
      logUnexpectedStateAction(context, "checkModeTransition");
    }
  }
}
