package com.satori.rtm;

/**
 * Sets up the acknowledgement mode for an operation.
 */
public enum Ack {
  /**
   * Operation doesn't need the acknowledgement from the RTM Service.
   */
  NO,
  /**
   * Operation should get the acknowledgement from the RTM Service.
   */
  YES
}
