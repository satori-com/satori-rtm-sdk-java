package com.satori.rtm;

/**
 * Defines the acknowledgement mode for RTM operations.
 */
public enum Ack {
  /**
   * RTM doesn't send an acknowledgement response for the operation.
   */
  NO,
  /**
   * RTM sends an acknowledgement response for the operation.
   */
  YES
}
