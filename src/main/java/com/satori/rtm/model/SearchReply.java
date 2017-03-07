package com.satori.rtm.model;

import java.util.List;

/**
 * Represents the body Protocol Data Unit (PDU) for a positive reply to a search request.
 * <p>
 * The PDU has the following basic structure for a positive reply:
 * <pre>{@literal
 * {
 *    "action": "rtm/search/data",
 *    "body": {
 *       "channels": [Channel]
 *    },
 *    "id": RequestId
 * }}
 * </pre>
 */
public class SearchReply {
  private List<String> channels;

  public SearchReply() { }

  public List<String> getChannels() {
    return channels;
  }
}
