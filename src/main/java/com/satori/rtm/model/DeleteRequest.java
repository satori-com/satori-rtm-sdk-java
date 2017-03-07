package com.satori.rtm.model;


/**
 * Represents the body of a Protocol Data Unit (PDU) for a delete request.
 * <p>
 * The PDU has the following basic structure:
 * <pre>{@literal
 * {
 *     "action": "rtm/delete",
 *     "body": {
 *         "channel": ChannelName,
 *         "purge": Boolean, OPTIONAL
 *     },
 *     "id": RequestId OPTIONAL
 * }}
 * </pre>
 */
public class DeleteRequest {
  private String channel;
  private Boolean purge;

  public DeleteRequest(String channel) {
    this(channel, null);
  }

  public DeleteRequest(String channel, Boolean purge) {
    this.channel = channel;
    this.purge = purge;
  }

  public String getChannel() {
    return channel;
  }

  public Boolean getPurge() {
    return purge;
  }
}
