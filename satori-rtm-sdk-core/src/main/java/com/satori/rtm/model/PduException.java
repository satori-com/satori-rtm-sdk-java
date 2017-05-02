package com.satori.rtm.model;

/**
 * Indicates the reply from the RTM Service has unexpected format or the RTM Service returns a negative response.
 */
public class PduException extends Exception {
  private final PduRaw mPdu;

  public PduException(String s, PduRaw pdu) {
    super(s + ": " + pdu.toString());
    this.mPdu = pdu;
  }

  public PduRaw getPdu() {
    return mPdu;
  }
}
