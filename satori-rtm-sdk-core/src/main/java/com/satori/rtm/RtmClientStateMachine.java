package com.satori.rtm;

import com.satori.rtm.utils.AbstractStateMachine;
import com.satori.rtm.utils.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The connection finite state machine manages all states and transitions for entire connection
 * <p/>
 * This class isn't accessible to end users.
 */
class RtmClientStateMachine extends AbstractStateMachine<RtmClientStateMachine.AbstractState> {
  private final static Logger LOG = LoggerFactory.getLogger(RtmClientStateMachine.class);
  private final static StoppedState STOPPED = new StoppedState();
  private final static ConnectingState CONNECTING = new ConnectingState();
  private final static ConnectedState CONNECTED = new ConnectedState();
  private final static AwaitingState AWAITING = new AwaitingState();
  private static Random RANDOM = new Random();
  private final ExecutorService mDispatcher;
  private final ScheduledExecutorService mTimerExecutionService;
  private final RtmClient mClient;
  private final RtmClientListener mListener;
  private final boolean mIsAutoReconnect;
  private final long mMaxReconnectInterval;
  private final long mMinReconnectInterval;
  private final long mJitter;
  private final String mName;
  private Integer mFailCount = 0;
  private volatile boolean isTransportConnected;
  private Future<?> mReconnectTimer = null;

  RtmClientStateMachine(
      RtmClient client,
      RtmClientListener listener,
      ScheduledExecutorService scheduledExecutorService,
      boolean isAutoReconnect,
      long minReconnectInterval,
      long maxReconnectInterval,
      ExecutorService dispatcher) {
    if (maxReconnectInterval < minReconnectInterval) {
      throw new IllegalArgumentException(
          "minReconnectInterval is greater than maxReconnectInterval");
    }
    this.mClient = client;
    this.mListener = listener;
    this.mState = STOPPED;
    this.mDispatcher = dispatcher;
    this.mTimerExecutionService = scheduledExecutorService;
    this.mIsAutoReconnect = isAutoReconnect;
    this.mMinReconnectInterval = minReconnectInterval;
    this.mJitter = (long) (RANDOM.nextFloat() * minReconnectInterval);
    this.mMaxReconnectInterval = maxReconnectInterval;
    this.isTransportConnected = false;
    this.mName = "Connection[" + randomUniqId() + "]";
  }

  @Override
  public String getName() {
    return mName;
  }

  public RtmClientListener getListener() {
    return mListener;
  }

  public boolean isConnected() {
    return isTransportConnected;
  }

  void enterStartState() {
    getState().enter(RtmClientStateMachine.this);
  }

  void onConnected() {
    getState().onConnected(RtmClientStateMachine.this);
  }

  void onConnectingFailed() {
    getState().onConnectingFailed(RtmClientStateMachine.this);
  }

  void onDisconnected() {
    getState().onDisconnected(RtmClientStateMachine.this);
  }

  void onInternalError() {
    getState().onInternalError(RtmClientStateMachine.this);
  }

  void onStart() {
    getState().onStart(RtmClientStateMachine.this);
  }

  void onStop() {
    getState().onStop(RtmClientStateMachine.this);
  }

  private void onReconnectAttempt() {
    getState().onReconnectAttempt(RtmClientStateMachine.this);
  }

  long getNextAwaitInterval() {
    LOG.debug(String.format("Try to reconnect (#%d)", mFailCount));
    int maxPow = 30;
    double count = Math.min(mFailCount, maxPow);
    long offset = (long) Math
        .min(mMaxReconnectInterval, mJitter + mMinReconnectInterval * Math.pow(2, count));
    this.mFailCount += 1;
    return offset;
  }

  private void resetFailCount() {
    if (0 < this.mFailCount) {
      LOG.debug(String.format("Reset fail counter (was %d)", mFailCount));
    }
    this.mFailCount = 0;
  }

  private void scheduleReconnect(long interval) {
    this.mReconnectTimer = this.mTimerExecutionService.schedule(new TimerTask() {
      @Override
      public void run() {
        mDispatcher.submit(new Runnable() {
          @Override
          public void run() {
            if (!isConnected()) {
              onReconnectAttempt();
            }
          }
        });
      }
    }, interval, TimeUnit.MILLISECONDS);
  }

  private void cancelReconnectTimer() {
    this.mReconnectTimer.cancel(false);
    this.mReconnectTimer = null;
  }

  private String randomUniqId() {
    return Integer.toHexString((int) Math.floor((1 + Math.random()) * 0x10000));
  }

  static class StoppedState extends AbstractState {
    @Override
    public void enter(RtmClientStateMachine context) {
      context.getListener().onEnterStopped(context.mClient);
    }

    @Override
    public void leave(RtmClientStateMachine context) {
      context.getListener().onLeaveStopped(context.mClient);
    }

    @Override
    protected void onStart(RtmClientStateMachine context) {
      transition(context, CONNECTING);
    }

    @Override
    protected void onStop(RtmClientStateMachine context) {
    }

    @Override
    protected void onConnected(RtmClientStateMachine context) {
    }

    @Override
    protected void onReconnectAttempt(RtmClientStateMachine context) {
    }
  }

  static class ConnectingState extends AbstractState {
    @Override
    public void enter(RtmClientStateMachine context) {
      context.getListener().onEnterConnecting(context.mClient);
    }

    @Override
    public void leave(RtmClientStateMachine context) {
      context.getListener().onLeaveConnecting(context.mClient);
    }

    @Override
    protected void onConnected(RtmClientStateMachine context) {
      transition(context, CONNECTED);
    }

    @Override
    protected void onConnectingFailed(RtmClientStateMachine context) {
      AbstractState nextState = context.mIsAutoReconnect ? AWAITING : STOPPED;
      transition(context, nextState);
    }

    @Override
    protected void onDisconnected(RtmClientStateMachine context) {
      AbstractState nextState = context.mIsAutoReconnect ? AWAITING : STOPPED;
      transition(context, nextState);
    }

    @Override
    protected void onInternalError(RtmClientStateMachine context) {
      AbstractState nextState = context.mIsAutoReconnect ? AWAITING : STOPPED;
      transition(context, nextState);
    }

    @Override
    protected void onStart(RtmClientStateMachine context) {
    }

    @Override
    protected void onStop(RtmClientStateMachine context) {
      transition(context, STOPPED);
    }

    @Override
    protected void onReconnectAttempt(RtmClientStateMachine context) {
    }
  }

  static class ConnectedState extends AbstractState {
    @Override
    public void enter(RtmClientStateMachine context) {
      context.resetFailCount();
      context.getListener().onEnterConnected(context.mClient);
      context.isTransportConnected = true;
    }

    @Override
    public void leave(RtmClientStateMachine context) {
      context.isTransportConnected = false;
      context.getListener().onLeaveConnected(context.mClient);
    }

    @Override
    protected void onDisconnected(RtmClientStateMachine context) {
      AbstractState nextState = context.mIsAutoReconnect ? AWAITING : STOPPED;
      transition(context, nextState);
    }

    @Override
    protected void onInternalError(RtmClientStateMachine context) {
      AbstractState nextState = context.mIsAutoReconnect ? AWAITING : STOPPED;
      transition(context, nextState);
    }

    @Override
    protected void onStart(RtmClientStateMachine context) {
    }

    @Override
    protected void onStop(RtmClientStateMachine context) {
      transition(context, STOPPED);
    }

    @Override
    protected void onReconnectAttempt(RtmClientStateMachine context) {
    }
  }

  static class AwaitingState extends AbstractState {
    @Override
    public void enter(final RtmClientStateMachine context) {
      long interval = context.getNextAwaitInterval();
      context.scheduleReconnect(interval);
      context.getListener().onEnterAwaiting(context.mClient);
    }

    @Override
    public void leave(RtmClientStateMachine context) {
      context.cancelReconnectTimer();
      context.getListener().onLeaveAwaiting(context.mClient);
    }

    @Override
    protected void onDisconnected(RtmClientStateMachine context) {
    }

    @Override
    protected void onStart(RtmClientStateMachine context) {
      transition(context, CONNECTING);
    }

    @Override
    protected void onStop(RtmClientStateMachine context) {
      transition(context, STOPPED);
    }

    @Override
    protected void onReconnectAttempt(RtmClientStateMachine context) {
      transition(context, CONNECTING);
    }
  }

  static abstract class AbstractState extends State<RtmClientStateMachine> {
    protected void onConnected(RtmClientStateMachine context) {
      logUnexpectedStateAction(context, "onConnected");
    }

    protected void onConnectingFailed(RtmClientStateMachine context) {
      logUnexpectedStateAction(context, "onConnectedFailed");
    }

    protected void onDisconnected(RtmClientStateMachine context) {
      logUnexpectedStateAction(context, "onDisconnected");
    }

    protected void onInternalError(RtmClientStateMachine context) {
      logUnexpectedStateAction(context, "onInternalError");
    }

    protected void onStart(RtmClientStateMachine context) {
      logUnexpectedStateAction(context, "onStart");
    }

    protected void onStop(RtmClientStateMachine context) {
      logUnexpectedStateAction(context, "onStop");
    }

    protected void onReconnectAttempt(RtmClientStateMachine context) {
      logUnexpectedStateAction(context, "onReconnectAttempt");
    }
  }
}
