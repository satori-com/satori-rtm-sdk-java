package com.satori.rtm;

import java.util.EnumSet;

public enum SubscriptionMode {
  TRACK_POSITION,
  FAST_FORWARD,
  AUTO_RECONNECT;

  /**
   * Tries to avoid data loss during reconnect, but may lose on slow connections.
   * <p>
   * SDK tracks the stream position from responses and tries to restore
   * subscription from the latest known position on reconnects. RTM forwards the
   * subscription to the earliest possible position if the stream position is expired
   * on reconnect or a client has slow connection.
   */
  public static final EnumSet<SubscriptionMode> RELIABLE =
      EnumSet.of(TRACK_POSITION, FAST_FORWARD, AUTO_RECONNECT);

  /**
   * May lose data during reconnect and on slow connections.
   * <p>
   * SDK doesn't track the stream position and restores subscription from it's
   * actual position. RTM forwards the subscription to the earliest possible
   * position if a client has slow connection.
   */
  public static final EnumSet<SubscriptionMode> SIMPLE =
      EnumSet.of(FAST_FORWARD, AUTO_RECONNECT);

  /**
   * Tries to avoid any data loss, could get out_of_sync and expired_position errors
   * on reconnect and slow connections.
   * <p>
   * SDK tracks the stream position from responses and tries to restore
   * subscription from the latest known position on reconnects. If the stream
   * position is expired then an expired_position error is thrown. If connection is
   * slow then out_of_sync error is thrown.
   */
  public static final EnumSet<SubscriptionMode> ADVANCED =
      EnumSet.of(TRACK_POSITION, AUTO_RECONNECT);
}
