package com.satori.rtm.model;

/**
 * Represents an error sent by RTM because of an internal error.
 */
public class SystemWideException extends PduException {
  public SystemWideException(PduRaw errorPdu) {
    super("System Wide error received", errorPdu);
  }
}
