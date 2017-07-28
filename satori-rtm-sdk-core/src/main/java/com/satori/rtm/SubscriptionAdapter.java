package com.satori.rtm;

import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import com.satori.rtm.model.SubscriptionError;
import com.satori.rtm.model.SubscriptionInfo;
import com.satori.rtm.model.UnsubscribeReply;
import com.satori.rtm.model.UnsubscribeRequest;

/**
 * An empty implementation of the {@link SubscriptionListener} interface.
 *
 */
public class SubscriptionAdapter implements SubscriptionListener {
  @Override
  public void onEnterUnsubscribed(UnsubscribeRequest request, UnsubscribeReply reply) {
  }

  @Override
  public void onLeaveUnsubscribed(UnsubscribeRequest request, UnsubscribeReply reply) {
  }

  @Override
  public void onEnterSubscribing(SubscribeRequest request) {
  }

  @Override
  public void onLeaveSubscribing(SubscribeRequest request) {
  }

  @Override
  public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
  }

  @Override
  public void onLeaveSubscribed(SubscribeRequest request, SubscribeReply reply) {
  }

  @Override
  public void onEnterUnsubscribing(UnsubscribeRequest request) {
  }

  @Override
  public void onLeaveUnsubscribing(UnsubscribeRequest request) {
  }

  @Override
  public void onEnterFailed() {
  }

  @Override
  public void onLeaveFailed() {
  }

  @Override
  public void onCreated() {
  }

  @Override
  public void onDeleted() {
  }

  @Override
  public void onSubscriptionData(SubscriptionData data) {
  }

  @Override
  public void onSubscriptionError(SubscriptionError error) {
  }

  @Override
  public void onSubscriptionInfo(SubscriptionInfo info) {

  }
}
