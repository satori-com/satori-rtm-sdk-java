package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (PDU) for a search request.
 * <p>
 * The PDU has the following basic structure:
 * <pre>{@literal
 * {
 *    "action": "rtm/search",
 *    "body": {
 *        "prefix": string()
 *    },
 *    "id": RequestId
 * }}
 * </pre>
 */
public class SearchRequest {
  private String prefix;

  public SearchRequest(String prefix) {
    this.prefix = prefix;
  }

  public String getPrefix() {
    return prefix;
  }
}
