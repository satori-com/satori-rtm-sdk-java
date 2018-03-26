package com.satori.rtm;


public enum RequestReturnMode {
  PREVIOUS_VALUE("previous_message"),
  PREVIOUS_VALUE_ON_OK("previous_message_on_ok"),
  PREVIOUS_VALUE_ON_ERROR("previous_value_message_error"),
  NONE(null);

  private final String value;

  RequestReturnMode(final String val){
    value = val;
  }

  @Override
  public String toString(){
    return value;
  }
}
