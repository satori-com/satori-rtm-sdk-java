package com.satori.rtm.model;

/**
 * Indicates a message with an incorrect or invalid JSON format received from RTM.
 */
public class InvalidJsonException extends Exception {
  private final String mJson;

  public InvalidJsonException(String json, Exception e) {
    super(String.format("Unable to parse json %s", json), e);
    this.mJson = json;
  }

  public String getJson() {
    return mJson;
  }
}
