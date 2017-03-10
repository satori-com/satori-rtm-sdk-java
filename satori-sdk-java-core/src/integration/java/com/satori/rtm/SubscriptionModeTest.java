package com.satori.rtm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import org.junit.Test;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

public class SubscriptionModeTest extends AbstractRealTest {
  @Test
  public void resubscribeWhenFailed() throws InterruptedException {
    final RtmClient client = clientBuilder()
        .build();
    client.start();

    SubscriptionListener listener = new SubscriptionAdapter() {
      @Override
      public void onEnterFailed() {
        dispatcher.add("on-error");
        client.removeSubscription(channel);
        client.createSubscription(channel, SubscriptionMode.ADVANCED, this);
      }

      @Override
      public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
        dispatcher.add("on-subscribed");
      }

      @Override
      public void onEnterSubscribing(SubscribeRequest request) {
        dispatcher.add("on-subscribing-" + request.getPosition());
      }
    };

    SubscriptionConfig cfg = new SubscriptionConfig(SubscriptionMode.ADVANCED, listener)
        .setPosition("invalid_next_format");
    client.createSubscription(channel, cfg);
    assertThat(getEvent(), equalTo("on-subscribing-invalid_next_format"));
    assertThat(getEvent(), equalTo("on-error"));
    assertThat(getEvent(), equalTo("on-subscribing-null"));
    assertThat(getEvent(), equalTo("on-subscribed"));
    client.stop();
  }

  @Test
  public void checkFastForwardFlagInModeWithFastForward() throws InterruptedException {
    final RtmClient client = clientBuilder()
        .build();
    client.start();
    client.createSubscription(channel, EnumSet.of(SubscriptionMode.FAST_FORWARD),
        new SubscriptionAdapter() {
          @Override
          public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
            dispatcher.add("fast_forward: " + request.isFastForward());
          }
        });
    assertThat(getEvent(), equalTo("fast_forward: true"));
    client.stop();
  }

  @Test
  public void checkFastForwardFlagInModeWithoutFastForward() throws InterruptedException {
    final RtmClient client = clientBuilder()
        .build();
    client.start();
    client.createSubscription(channel, EnumSet.noneOf(SubscriptionMode.class),
        new SubscriptionAdapter() {
          @Override
          public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
            dispatcher.add("fast_forward: " + request.isFastForward());
          }
        });
    assertThat(getEvent(), equalTo("fast_forward: false"));
    client.stop();
  }

  @Test
  public void checkNextInModeWithTrackNext() throws InterruptedException {
    final RtmClient client = clientBuilder()
        .build();
    client.start();
    final AtomicReference<SubscribeReply> ref = new AtomicReference<SubscribeReply>(null);
    SubscriptionConfig cfg = new SubscriptionConfig(
        EnumSet.of(SubscriptionMode.TRACK_POSITION, SubscriptionMode.AUTO_RECONNECT),
        new SubscriptionAdapter() {
          @Override
          public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
            dispatcher.add("next: " + request.getPosition());
            ref.compareAndSet(null, reply);
          }
        }
    );
    client.createSubscription(channel, cfg);
    assertThat(getEvent(), equalTo("next: null"));
    client.stop();
    client.start();
    assertThat(getEvent(), equalTo("next: " + ref.get().getPosition()));
    client.stop();
  }

  @Test
  public void checkNextInModeWithoutTrackNext() throws InterruptedException {
    final RtmClient client = clientBuilder()
        .build();
    client.start();
    SubscriptionConfig cfg = new SubscriptionConfig(
        EnumSet.of(SubscriptionMode.AUTO_RECONNECT),
        new SubscriptionAdapter() {
          @Override
          public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
            dispatcher.add("next: " + request.getPosition());
          }
        }
    );
    client.createSubscription(channel, cfg);
    assertThat(getEvent(), equalTo("next: null"));
    client.stop();
    client.start();
    assertThat(getEvent(), equalTo("next: null"));
    client.stop();
  }

  @Test
  public void checkReconnectWithoutReconnectFlag() throws InterruptedException {
    final RtmClient client = clientBuilder()
        .build();
    client.start();
    SubscriptionConfig cfg = new SubscriptionConfig(
        EnumSet.noneOf(SubscriptionMode.class),
        logSubscriptionListener(
            SubscriptionListenerType.CREATED,
            SubscriptionListenerType.DELETED,
            SubscriptionListenerType.SUBSCRIBED
        )
    );
    client.createSubscription(channel, cfg);
    assertThat(getEvent(), equalTo("on-created"));
    assertThat(getEvent(), equalTo("on-enter-subscribed"));
    client.stop();
    assertThat(getEvent(), equalTo("on-leave-subscribed"));
    assertThat(getEvent(), equalTo("on-deleted"));
  }

  @Test
  public void checkReconnectWithReconnectFlag() throws InterruptedException {
    final RtmClient client = clientBuilder()
        .build();
    client.start();
    SubscriptionConfig cfg = new SubscriptionConfig(
        EnumSet.of(SubscriptionMode.AUTO_RECONNECT),
        logSubscriptionListener(
            SubscriptionListenerType.CREATED,
            SubscriptionListenerType.DELETED,
            SubscriptionListenerType.SUBSCRIBED
        )
    );
    client.createSubscription(channel, cfg);
    assertThat(getEvent(), equalTo("on-created"));
    assertThat(getEvent(), equalTo("on-enter-subscribed"));
    client.stop();
    assertThat(getEvent(), equalTo("on-leave-subscribed"));
    client.start();
    assertThat(getEvent(), equalTo("on-enter-subscribed"));
    client.stop();
    assertThat(getEvent(), equalTo("on-leave-subscribed"));
  }
}
