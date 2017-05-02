package com.satori.rtm.auth;

/**
 * Signals that authentication process with the RTM Service failed.
 */
public class AuthException extends Exception {
  public AuthException(Throwable throwable) {
    super(throwable);
  }
}
