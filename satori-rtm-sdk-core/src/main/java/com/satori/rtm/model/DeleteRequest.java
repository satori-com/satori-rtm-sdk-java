package com.satori.rtm.model;

import com.satori.rtm.ReqReadMode;

/**
 * Represents the body of a Protocol Data Unit (<strong>PDU</strong>) for a delete request.
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

  private String read;

  public DeleteRequest(String channel) {
    this(channel, null, null);
  }

  public DeleteRequest(String channel, ReqReadMode returnMode) {
    this(channel, null, returnMode);
  }

  public DeleteRequest(String channel, Boolean purge, ReqReadMode returnMode) {
    this.channel = channel;
    this.purge = purge;
    this.read =  returnMode == null ? null : returnMode.toString();
  }

  public String getChannel() {
    return channel;
  }

  public Boolean getPurge() {
    return purge;
  }

  public ReqReadMode getRequestReturnMode() {
    return ReqReadMode.valueOf(read);
  }
}
