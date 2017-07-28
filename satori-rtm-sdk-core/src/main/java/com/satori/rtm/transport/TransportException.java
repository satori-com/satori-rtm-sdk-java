package com.satori.rtm.transport;

/**
 * General transport exception.
 * <p>
 * Indicates an error occurred when sending or receiving data or connecting to RTM.
 */
public class TransportException extends Exception {
  public TransportException(String reason) {
    super(reason);
  }

  public TransportException(Throwable e) {
    super(e);
  }
}
