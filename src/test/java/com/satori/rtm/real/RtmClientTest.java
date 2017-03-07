package com.satori.rtm.real;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.satori.rtm.Ack;
import com.satori.rtm.RtmClient;
import com.satori.rtm.RtmClientAdapter;
import com.satori.rtm.RtmClientBuilder;
import com.satori.rtm.SubscriptionAdapter;
import com.satori.rtm.SubscriptionConfig;
import com.satori.rtm.SubscriptionMode;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PublishReply;
import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import org.junit.Test;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


public class RtmClientTest extends AbstractRealTest {
  @Test
  public void zeroPendingQueue() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setPendingActionQueueLength(0)
        .build();
    logFuture(client.publish("channel", "offline message", Ack.YES));
    assertThat(getEvent(), startsWith("future-failure"));
  }

  @Test
  public void limitedPendingQueue() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setPendingActionQueueLength(1)
        .build();
    // try to queue two item
    logFuture(client.publish("channel", 1, Ack.YES), "p1");
    logFuture(client.publish("channel", 2, Ack.YES), "p2");

    // get exception on second one because length of offline queue == 1
    assertThat(getEvent(), startsWith("p2-failure"));
    client.start();
    // after start first publish should be processed
    assertThat(getEvent(), equalTo("p1-success"));

    // next publish should work as well
    logFuture(client.publish("channel", 3, Ack.YES), "p3");
    assertThat(getEvent(), equalTo("p3-success"));

    client.stop();
  }

  @Test
  public void stopClientInCallback() throws InterruptedException {
    RtmClient client = clientBuilder().setListener(new RtmClientAdapter() {
      @Override
      public void onEnterStopped(RtmClient client) {
        dispatcher.add("on-enter-stopped");
      }

      @Override
      public void onLeaveStopped(RtmClient client) {
        dispatcher.add("on-leave-stopped");
        client.stop();
      }
    }).build();
    client.start();

    assertThat(getEvent(), equalTo("on-enter-stopped"));
    assertThat(getEvent(), equalTo("on-leave-stopped"));
    assertThat(getEvent(), equalTo("on-enter-stopped"));
  }

  @Test
  public void unexpectedDisconnect() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setMinReconnectInterval(100)
        .setMinReconnectInterval(100)
        .setTransportFactory(new SandboxWebSocketFactory())
        .build();

    client.start();
    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(SubscriptionListenerType.SUBSCRIBED));

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.publish(channel, "force_disconnect", Ack.NO);

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.stop();

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
  }

  @Test
  public void publishManyPendingMessagesInOnConnectCallback() throws InterruptedException {
    RtmClient client = clientBuilder()
        .build();
    for (int i = 0; i < 1024; i++) {
      client.publish(channel, "text", Ack.NO);
    }
    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(SubscriptionListenerType.SUBSCRIBED));
    client.start();
    assertThat(getEvent(), equalTo("on-enter-subscribed"));
    client.stop();
    assertThat(getEvent(), equalTo("on-leave-subscribed"));
  }

  @Test
  public void unknownSchemaError() throws InterruptedException {
    RtmClient client = new RtmClientBuilder("foobar", "appkey")
        .setConnectionTimeout(1000)
        .setListener(new RtmClientAdapter() {
          @Override
          public void onConnectingError(RtmClient client, Exception ex) {
            dispatcher.add(ex.getMessage());
          }
        })
        .build();
    client.start();

    assertThat(getEvent(), equalTo("The scheme part is empty."));

    client.stop();
  }

  @Test
  public void unknownHostError() throws InterruptedException {
    RtmClient client = new RtmClientBuilder("ws://foobar/", "appkey")
        .setConnectionTimeout(1000)
        .setListener(new RtmClientAdapter() {
          @Override
          public void onConnectingError(RtmClient client, Exception ex) {
            dispatcher.add(ex.getMessage());
          }
        })
        .build();
    client.start();

    assertThat(getEvent(), equalTo(
        "com.neovisionaries.ws.client.WebSocketException: Failed to connect to 'foobar:80': foobar"));

    client.stop();
  }

  @Test
  public void clientCallback() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setMinReconnectInterval(100)
        .setMaxReconnectInterval(100)
        .setTransportFactory(new SandboxWebSocketFactory())
        .setListener(logClientListener(
            ClientListenerType.ERROR,
            ClientListenerType.STOPPED,
            ClientListenerType.CONNECTING,
            ClientListenerType.CONNECTED,
            ClientListenerType.AWAITING))
        .build();

    client.start();

    assertThat(getEvent(), equalTo("on-enter-stopped"));
    assertThat(getEvent(), equalTo("on-leave-stopped"));
    assertThat(getEvent(), equalTo("on-enter-connecting"));
    assertThat(getEvent(), equalTo("on-leave-connecting"));
    assertThat(getEvent(), equalTo("on-enter-connected"));

    client.publish(channel, "force_disconnect", Ack.NO);

    assertThat(getEvent(), equalTo("on-leave-connected"));
    assertThat(getEvent(), equalTo("on-enter-awaiting"));
    assertThat(getEvent(), equalTo("on-leave-awaiting"));
    assertThat(getEvent(), equalTo("on-enter-connecting"));
    assertThat(getEvent(), equalTo("on-leave-connecting"));
    assertThat(getEvent(), equalTo("on-enter-connected"));

    client.stop();

    assertThat(getEvent(), equalTo("on-leave-connected"));
    assertThat(getEvent(), equalTo("on-enter-stopped"));
  }

  @Test
  public void exceptionInClientCallbackNotBreakClient() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setListener(new RtmClientAdapter() {
          @Override
          public void onEnterConnecting(RtmClient client) {
            throw new RuntimeException("some user exception");
          }
        })
        .build();
    client.start();

    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.SUBSCRIPTION_DATA
        ));

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.publish(channel, "text", Ack.NO);

    assertThat(getEvent(), equalTo("text"));

    client.removeSubscription(channel);

    assertThat(getEvent(), equalTo("on-leave-subscribed"));

    client.stop();
  }

  @Test
  public void exceptionInSubscriptionCallback() throws InterruptedException {
    RtmClient client = clientBuilder().build();
    client.start();
    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        new SubscriptionAdapter() {
          @Override
          public void onSubscriptionData(SubscriptionData data) {
            for (String message : data.getMessagesAsStrings()) {
              dispatcher.add(message);
            }
            throw new RuntimeException("channel user exception");
          }

          @Override
          public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
            dispatcher.add("on-enter-subscribed");
          }

          @Override
          public void onLeaveSubscribed(SubscribeRequest request, SubscribeReply reply) {
            dispatcher.add("on-leave-subscribed");
          }

          @Override
          public void onEnterSubscribing(SubscribeRequest request) {
            throw new RuntimeException("some user exception");
          }
        });

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.publish(channel, "text1", Ack.NO);
    client.publish(channel, "text2", Ack.NO);

    assertThat(getEvent(), equalTo("text1"));
    assertThat(getEvent(), equalTo("text2"));

    client.removeSubscription(channel);

    assertThat(getEvent(), equalTo("on-leave-subscribed"));

    client.stop();
  }

  @Test
  public void publishAndStop() throws InterruptedException, ExecutionException {
    RtmClient client = clientBuilder()
        .setListener(logClientListener(ClientListenerType.CONNECTED))
        .build();
    client.start();
    assertThat(getEvent(), equalTo("on-enter-connected"));
    Pdu<PublishReply> replay = awaitFuture(client.publish(channel, "data", Ack.YES));
    client.stop();
    SubscriptionConfig config = new SubscriptionConfig(SubscriptionMode.SIMPLE,
        logSubscriptionListener(SubscriptionListenerType.SUBSCRIPTION_DATA));
    config.setPosition(replay.getBody().getPosition());
    client.createSubscription(channel, config);
    client.start();
    assertThat(getEvent(), equalTo("on-leave-connected"));
    assertThat(getEvent(), equalTo("on-enter-connected"));
    assertThat(getEvent(), equalTo("data"));
    client.stop();
    assertThat(getEvent(), equalTo("on-leave-connected"));
  }

  @Test
  public void autoReconnectDisabled() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .setListener(logClientListener(ClientListenerType.CONNECTED, ClientListenerType.STOPPED))
        .setAutoReconnect(false)
        .build();
    client.start();
    assertThat(getEvent(), equalTo("on-enter-stopped"));
    assertThat(getEvent(), equalTo("on-leave-stopped"));
    assertThat(getEvent(), equalTo("on-enter-connected"));
    client.publish("any", "force_disconnect", Ack.NO);
    assertThat(getEvent(), equalTo("on-leave-connected"));
    assertThat(getEvent(), equalTo("on-enter-stopped"));
    client.stop();
  }

  @Test
  public void passCustomDispatcher() throws InterruptedException {
    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("d-%d").build();
    final ExecutorService dispatcher = Executors.newSingleThreadExecutor(namedThreadFactory);

    RtmClient client = clientBuilder()
        .setDispatcher(dispatcher, true)
        .setListener(new RtmClientAdapter() {
          @Override
          public void onEnterConnected(RtmClient client) {
            addEvent(Thread.currentThread().getName() + "-enter-connected");
          }
        })
        .build();
    client.start();
    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        new SubscriptionAdapter() {
          @Override
          public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
            addEvent(Thread.currentThread().getName() + "-enter-subscribed");
          }

          @Override
          public void onSubscriptionData(SubscriptionData data) {
            for (String msg : data.getMessagesAsStrings()) {
              addEvent(Thread.currentThread().getName() + "-" + msg);
            }
          }
        });

    assertThat(getEvent(), equalTo("d-0-enter-connected"));
    assertThat(getEvent(), equalTo("d-0-enter-subscribed"));
    client.publish(channel, "message", Ack.NO);
    assertThat(getEvent(), equalTo("d-0-message"));
    client.stop();
  }
}
