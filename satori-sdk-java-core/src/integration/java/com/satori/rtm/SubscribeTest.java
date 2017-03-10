package com.satori.rtm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.concurrent.ExecutionException;

@RunWith(JUnit4.class)
public class SubscribeTest extends AbstractRealTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void subscribeBeforeStart() throws ExecutionException, InterruptedException {
    RtmClient client = clientBuilder().build();
    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.CREATED,
            SubscriptionListenerType.SUBSCRIBING,
            SubscriptionListenerType.DELETED
        ));

    assertThat(client.isConnected(), is(false));
    assertThat(getEvent(), equalTo("on-created"));
    assertThat(dispatcher.poll(), nullValue());

    client.start();

    assertThat(getEvent(), equalTo("on-enter-subscribing"));
    assertThat(getEvent(), equalTo("on-leave-subscribing"));

    client.stop();

    assertThat(dispatcher.poll(), nullValue());
  }

  @Test
  public void subscriptionShouldBeActiveAfterStopAndStart() throws InterruptedException {
    RtmClient client = clientBuilder().build();
    client.start();
    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.SUBSCRIPTION_DATA
        ));

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.stop();
    client.start();

    assertThat(getEvent(), equalTo("on-leave-subscribed"));

    client.publish(channel, "text1", Ack.NO);

    assertThat(getEvent(), equalTo("on-enter-subscribed"));
    assertThat(getEvent(), equalTo("text1"));

    client.stop();
  }

  @Test
  public void subscribeError() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .build();
    client.start();
    String errorChan = generateChannel("subscribe_error");
    client.createSubscription(errorChan, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBING,
            SubscriptionListenerType.FAILED
        ));

    assertThat(getEvent(), equalTo("on-enter-subscribing"));
    assertThat(getEvent(), equalTo("on-leave-subscribing"));
    assertThat(getEvent(), equalTo("on-enter-failed"));

    client.stop();

    assertThat(getEvent(), equalTo("on-leave-failed"));
    assertThat(client.isConnected(), equalTo(false));

    client.start();

    assertThat(getEvent(), equalTo("on-enter-subscribing"));
    assertThat(getEvent(), equalTo("on-leave-subscribing"));
    assertThat(getEvent(), equalTo("on-enter-failed"));

    client.stop();
  }

  @Test
  public void changeListenersAfterResubscribe() throws InterruptedException {
    RtmClient client = clientBuilder().build();
    for (int i = 0; i < 10; i++) {
      final int index = i;
      client.createSubscription(channel, SubscriptionMode.SIMPLE,
          new SubscriptionAdapter() {
            @Override
            public void onCreated() {
              dispatcher.add(String.format("on-created-%s", index));
            }

            @Override
            public void onDeleted() {
              dispatcher.add(String.format("on-deleted-%s", index));
            }
          });
      client.removeSubscription(channel);
    }

    for (int i = 0; i < 10; i++) {
      assertThat(getEvent(), equalTo(String.format("on-created-%s", i)));
      assertThat(getEvent(), equalTo(String.format("on-deleted-%s", i)));
    }

    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.SUBSCRIPTION_DATA
        ));
    client.start();

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.publish(channel, "test", Ack.NO);

    assertThat(getEvent(), equalTo("test"));

    client.stop();

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
  }

  @Test
  public void doubleSubscribeShouldThrowError() throws InterruptedException, ExecutionException {
    RtmClient client = clientBuilder().build();
    try {

      SubscriptionListener listener = logSubscriptionListener(SubscriptionListenerType.SUBSCRIBED);
      client.start();
      client.createSubscription(channel, SubscriptionMode.SIMPLE, listener);
      assertThat(getEvent(), equalTo("on-enter-subscribed"));
      exception.expect(RuntimeException.class);
      client.createSubscription(channel, SubscriptionMode.SIMPLE, listener);
    } finally {
      client.stop();
    }
  }
}
