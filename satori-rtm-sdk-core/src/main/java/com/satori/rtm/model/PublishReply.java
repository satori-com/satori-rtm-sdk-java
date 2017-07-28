package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (<strong>PDU</strong>) for a positive reply to a publish request.
 * <p>
 * To learn more about using {@code PublishReply}, see the section "Publishing" in the
 * <em>Satori Docs</em> chapter "Java SDK Quickstart".
 * <p>
 * The PDU has the following basic structure for a positive response:
 * <pre>{@literal
 * {
 *    "action": "rtm/publish/ok",
 *    "body": {
 *        "position": ChannelStreamPosition
 *    },
 *    "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class PublishReply {
  private String position;

  public PublishReply() {
  }

  public String getPosition() {
    return position;
  }
}
