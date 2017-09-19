package com.satori.rtm.model;

/**
 * Indicates the reply from RTM has unexpected format or RTM returns a negative response.
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

  public CommonError getReply() {
    return mPdu.convertBodyTo(CommonError.class).getBody();
  }
}
