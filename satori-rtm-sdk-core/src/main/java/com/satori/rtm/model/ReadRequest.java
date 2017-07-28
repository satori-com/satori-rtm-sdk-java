package com.satori.rtm.model;

/**
 * Represents the body of a Protocol Data Unit (<strong>PDU</strong>) for a read request.
 * <p>
 * The PDU has the following basic structure:
 * <pre>{@literal
 * {
 *    "action": "rtm/read",
 *    "body": {
 *        "channel": string(),
 *        "position": ChannelStreamPosition, OPTIONAL
 *    },
 *    "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class ReadRequest {
  private String channel;
  private String position;

  public ReadRequest() { }

  public ReadRequest(String channel, String position) {
    this.channel = channel;
    this.position = position;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getPosition() {
    return position;
  }

  public void setPosition(String position) {
    this.position = position;
  }
}

