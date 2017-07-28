package com.satori.rtm;

/**
 * An empty implementation of the {@link RtmClientListener} interface.
 *
 */
public abstract class RtmClientAdapter implements RtmClientListener {
  @Override
  public void onTransportError(RtmClient client, Exception ex) {
  }

  @Override
  public void onConnectingError(RtmClient client, Exception ex) {
  }

  @Override
  public void onError(RtmClient client, Exception ex) {
  }

  @Override
  public void onEnterStopped(RtmClient client) {
  }

  @Override
  public void onLeaveStopped(RtmClient client) {
  }

  @Override
  public void onEnterConnecting(RtmClient client) {
  }

  @Override
  public void onLeaveConnecting(RtmClient client) {
  }

  @Override
  public void onEnterConnected(RtmClient client) {
  }

  @Override
  public void onLeaveConnected(RtmClient client) {
  }

  @Override
  public void onEnterAwaiting(RtmClient client) {
  }

  @Override
  public void onLeaveAwaiting(RtmClient client) {
  }
}
