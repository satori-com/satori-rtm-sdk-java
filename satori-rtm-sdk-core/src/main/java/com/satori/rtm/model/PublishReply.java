package com.satori.rtm.model;

import java.util.Objects;

/**
 * Represents the body of a Protocol Data Unit (<strong>PDU</strong>) for a positive reply to a publish request.
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

  private PreviousMessage previous;

  public PublishReply() {
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PublishReply that = (PublishReply) o;
    return Objects.equals(position, that.position) &&
        Objects.equals(previous, that.previous);
  }

  @Override
  public int hashCode() {

    return Objects.hash(position, previous);
  }

  public PreviousMessage getPrevious() {
    return previous;

  }

  public String getPosition() {
    return position;
  }
}
