package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (PDU) for a common error.
 * <p>
 * The {@code error} field specifies the unique identifier for the error.
 * <p>
 * The {@code reason} field contains text that describes the error. This field is variable and
 * may change in the future, and should not be parsed. You can use this text, for example,
 * to include in error log files.
 * <p>
 * For more information on error messages from the RTM Service, see the <em>RTM API Reference</em>.
 *
 * <pre>{@literal
 * {
 *    "error": string(),
 *    "reason": text()
 * }}
 * </pre>
 */
public class CommonError {
  private String error;

  private String reason;

  public CommonError() {
  }

  public CommonError(String error, String reason) {
    this.error = error;
    this.reason = reason;
  }

  public String getError() {
    return error;
  }

  public String getReason() {
    return reason;
  }
}
