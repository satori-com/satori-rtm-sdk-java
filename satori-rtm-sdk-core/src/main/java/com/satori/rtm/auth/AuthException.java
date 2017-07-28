package com.satori.rtm.auth;

/**
 * Signals that the authentication process failed
 */
public class AuthException extends Exception {
  public AuthException(Throwable throwable) {
    super(throwable);
  }
}
