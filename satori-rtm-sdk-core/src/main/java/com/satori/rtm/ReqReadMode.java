package com.satori.rtm;


public enum ReqReadMode {
  PREVIOUS_VALUE("previous_message"),
  PREVIOUS_VALUE_ON_OK("previous_message_on_ok"),
  PREVIOUS_VALUE_ON_ERROR("previous_message_on_error"),
  NONE(null);

  private final String value;

  ReqReadMode(final String val){
    value = val;
  }

  @Override
  public String toString(){
    return value;
  }
}
