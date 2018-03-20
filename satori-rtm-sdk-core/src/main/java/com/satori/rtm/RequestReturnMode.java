package com.satori.rtm;


public enum RequestReturnMode {
  PREVIOUS_VALUE("previous_value"),
  PREVIOUS_VALUE_ON_OK("previous_value_on_ok"),
  PREVIOUS_VALUE_ON_ERROR("previous_value_on_error"),
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
