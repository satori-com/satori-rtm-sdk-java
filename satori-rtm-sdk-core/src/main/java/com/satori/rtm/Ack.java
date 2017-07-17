package com.satori.rtm;

/**
 * Defines the acknowledgement mode for RTM operations.
 */
public enum Ack {
  /**
   * RTM doesn't send an acknowledgement response to the operation.
   */
  NO,
  /**
   * RTM sends acknowledgement response to the operation.
   */
  YES
}
