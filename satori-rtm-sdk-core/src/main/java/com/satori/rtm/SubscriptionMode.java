package com.satori.rtm;

import java.util.EnumSet;

public enum SubscriptionMode {
  TRACK_POSITION,
  FAST_FORWARD,
  /**
   * @deprecated As of release 1.0.2, not used anymore.
   */
  @Deprecated
  AUTO_RECONNECT;

  /**
   * RTM tracks the {@code position} value for the subscription and
   * tries to use it when resubscribing after the connection drops and the client reconnects.
   * If the {@code position} points to an expired message, RTM fast-forwards to the earliest
   * {@code position} that points to a non-expired message.
   * <p>
   * This mode reliably goes to the next available message when RTM is resubscribing. However,
   * RTM always fast-forwards the subscription if necessary, so it never returns an error for an
   * 'out-of-sync' condition.
   * <p>
   * To learn more about position tracking and fast-forwarding, see the sections "... with position"
   * and "... with fast-forward (advanced)" in the chapter "Subscribing" in <em>Satori Docs</em>.
   */
  public static final EnumSet<SubscriptionMode> RELIABLE =
      EnumSet.of(TRACK_POSITION, FAST_FORWARD);

  /**
   * RTM doesn't track the {@code position} value for the
   * subscription. Instead, when RTM resubscribes following a reconnection, it fast-forwards to
   * the earliest {@code position} that points to a non-expired message.
   * <p>
   * Because RTM always fast-forwards the subscription, it never returns an error for an
   * 'out-of-sync' condition.
   * <p>
   * To learn more about position tracking and fast-forwarding, see the sections "... with position"
   * and "... with fast-forward (advanced)" in the chapter "Subscribing" in <em>Satori Docs</em>.
   * RTM SDK doesn't track the position of the next message for the subscription.
   */
  public static final EnumSet<SubscriptionMode> SIMPLE =
      EnumSet.of(FAST_FORWARD);

  /**
   * RTM always tracks the {@code position} value for the subscription and tries to
   * use it when resubscribing after the connection drops and the client reconnects.
   * <p>
   * If the position points to an expired message, the resubscription attempt fails. RTM sends an
   * {@code expired_position} error and stops the subscription process.
   * <p>
   * If the subscription is active, and RTM detects that the current {@code position} value
   * points to an expired message, the subscription is in an 'out-of-sync' state. In this case,
   * RTM sends an {@code out_of_sync} error and unsubscribes you.
   * <p>
   * To learn more about position tracking and fast-forwarding, see the sections "... with position"
   * and "... with fast-forward (advanced)" in the chapter "Subscribing" in <em>Satori Docs</em>.
   * RTM always tracks the position of the next message for the subscription.
   */
  public static final EnumSet<SubscriptionMode> ADVANCED =
      EnumSet.of(TRACK_POSITION);
}
