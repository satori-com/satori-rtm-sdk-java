package com.satori.rtm.model;

/**
 * Represents an error sent by the RTM Service due to an internal error.
 */
public class SystemWideException extends PduException {
  public SystemWideException(PduRaw errorPdu) {
    super("System Wide error received", errorPdu);
  }
}
