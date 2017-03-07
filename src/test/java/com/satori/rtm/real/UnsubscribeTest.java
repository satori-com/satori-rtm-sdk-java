package com.satori.rtm.real;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.satori.rtm.Ack;
import com.satori.rtm.RtmClient;
import com.satori.rtm.SubscriptionMode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.concurrent.ExecutionException;

@RunWith(JUnit4.class)
public class UnsubscribeTest extends AbstractRealTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void unsubscribeWithoutActivatingClient() throws InterruptedException, ExecutionException {
    RtmClient client = clientBuilder().build();
    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.CREATED,
            SubscriptionListenerType.DELETED
        ));

    assertThat(getEvent(), equalTo("on-created"));

    client.removeSubscription(channel);

    assertThat(getEvent(), equalTo("on-deleted"));
  }

  @Test
  public void unsubscribeOnActivatedClient() throws InterruptedException, ExecutionException {
    RtmClient client = clientBuilder().build();
    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.CREATED,
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.DELETED
        ));

    assertThat(getEvent(), equalTo("on-created"));
    client.start();

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.removeSubscription(channel);

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
    assertThat(getEvent(), equalTo("on-deleted"));

    client.stop();
  }

  @Test
  public void unsubscribeErrorShouldRestartTheClient() throws InterruptedException {
    String specificChannel = generateChannel("unsubscribe_error");
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .setListener(logClientListener(ClientListenerType.CONNECTED))
        .build();

    client.start();
    client.createSubscription(specificChannel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.UNSUBSCRIBING,
            SubscriptionListenerType.DELETED
        ));

    assertThat(getEvent(), equalTo("on-enter-connected"));
    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.removeSubscription(specificChannel);

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
    assertThat(getEvent(), equalTo("on-enter-unsubscribing"));
    assertThat(getEvent(), equalTo("on-leave-unsubscribing"));

    assertThat(getEvent(), equalTo("on-deleted"));
    assertThat(getEvent(), equalTo("on-leave-connected"));
    assertThat(getEvent(), equalTo("on-enter-connected"));

    client.stop();
  }

  @Test
  public void doubleUnsubscribeThrowException() throws InterruptedException, ExecutionException {
    RtmClient client = clientBuilder().build();
    try {
      client.start();
      client.createSubscription(channel, SubscriptionMode.SIMPLE,
          logSubscriptionListener(
              SubscriptionListenerType.UNSUBSCRIBED,
              SubscriptionListenerType.DELETED
          ));

      assertThat(getEvent(), equalTo("on-enter-unsubscribed"));
      assertThat(getEvent(), equalTo("on-leave-unsubscribed"));

      client.removeSubscription(channel);

      assertThat(getEvent(), equalTo("on-enter-unsubscribed"));
      assertThat(getEvent(), equalTo("on-deleted"));

      exception.expect(RuntimeException.class);
      client.removeSubscription(channel);
    } finally {
      client.stop();
    }
  }

  @Test
  public void subscriptionErrorDuringUnsubscribing() throws InterruptedException {
    String specificChannel = generateChannel("unsubscribe_nack");
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .build();

    client.start();
    client.createSubscription(specificChannel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.UNSUBSCRIBING,
            SubscriptionListenerType.DELETED
        ));

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.removeSubscription(specificChannel);

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
    assertThat(getEvent(), equalTo("on-enter-unsubscribing"));

    client.publish(specificChannel, "out_of_sync", Ack.YES);

    assertThat(getEvent(), equalTo("on-leave-unsubscribing"));
    assertThat(getEvent(), equalTo("on-deleted"));
  }
}
