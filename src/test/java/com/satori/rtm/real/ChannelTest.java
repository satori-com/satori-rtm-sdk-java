package com.satori.rtm.real;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.Ack;
import com.satori.rtm.RtmClient;
import com.satori.rtm.SubscriptionAdapter;
import com.satori.rtm.SubscriptionConfig;
import com.satori.rtm.SubscriptionListener;
import com.satori.rtm.SubscriptionMode;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduException;
import com.satori.rtm.model.PublishReply;
import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import com.satori.rtm.model.SubscriptionError;
import com.satori.rtm.transport.TransportException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(JUnit4.class)
public class ChannelTest extends AbstractRealTest {
  @Test
  public void subscribeWithIncorrectNextAndResubscribeWithHistory() throws InterruptedException {
    final RtmClient client = clientBuilder()
        .build();
    client.start();
    SubscriptionListener listener = new SubscriptionAdapter() {

      @Override
      public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
        SubscribeRequest.History history = request.getHistory();
        Integer count = (null != history) ? history.getCount() : null;
        dispatcher.add(String.format("on-subscribed-enter-%s-%s", request.getPosition(), count));
      }
    };
    SubscriptionConfig config =
        new SubscriptionConfig(SubscriptionMode.SIMPLE, listener) {
          @Override
          protected boolean onError(SubscriptionError error) {
            boolean isInvalidFormatError = "invalid_format".equals(error.getError());
            if (isInvalidFormatError && (null != getPosition())) {
              this.setPosition(null);
              this.setCount(100);
              return false;
            }
            return true;
          }
        };
    client.createSubscription(channel, config.setPosition("invalid_next"));
    assertThat(getEvent(), equalTo("on-subscribed-enter-null-100"));
    client.stop();
  }

  @Test
  public void publishingDataWithDisconnect()
      throws URISyntaxException, IOException, TransportException, ExecutionException,
      InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .setMinReconnectInterval(100)
        .setMaxReconnectInterval(100)
        .setListener(logClientListener(
            ClientListenerType.CONNECTED,
            ClientListenerType.CONNECTING
        ))
        .build();
    client.start();
    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIPTION_DATA,
            SubscriptionListenerType.SUBSCRIBED));

    assertThat(getEvent(), equalTo("on-enter-connecting"));
    assertThat(getEvent(), equalTo("on-leave-connecting"));
    assertThat(getEvent(), equalTo("on-enter-connected"));
    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.publish(channel, "text1", Ack.NO);

    assertThat(getEvent(), equalTo("text1"));

    client.publish(channel, "force_disconnect", Ack.NO);

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
    assertThat(getEvent(), equalTo("on-leave-connected"));
    assertThat(getEvent(), equalTo("on-enter-connecting"));
    assertThat(getEvent(), equalTo("on-leave-connecting"));
    assertThat(getEvent(), equalTo("on-enter-connected"));
    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.publish(channel, "text2", Ack.NO);
    client.publish(channel, "text3", Ack.NO);

    assertThat(getEvent(), equalTo("text2"));
    assertThat(getEvent(), equalTo("text3"));

    client.stop();

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
    assertThat(getEvent(), equalTo("on-leave-connected"));
  }

  @Test
  public void publishingDataWhileClientIsStopped() throws InterruptedException {
    RtmClient client = clientBuilder()
        .build();
    client.start();
    client.createSubscription(channel, SubscriptionMode.RELIABLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.SUBSCRIPTION_DATA
        ));

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.publish(channel, "text1", Ack.NO);

    assertThat(getEvent(), equalTo("text1"));

    client.stop();
    assertThat(getEvent(), equalTo("on-leave-subscribed"));

    client.publish(channel, "text2", Ack.NO);
    client.publish(channel, "text3", Ack.NO);
    client.start();

    assertThat(getEvent(), equalTo("on-enter-subscribed"));
    assertThat(getEvent(), equalTo("text2"));
    assertThat(getEvent(), equalTo("text3"));

    client.stop();

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
  }

  @Test
  public void subscriptionError() throws ExecutionException, InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .build();
    client.start();
    client.createSubscription(channel, SubscriptionMode.RELIABLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.FAILED
        ));

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.publish(channel, "trigger_out_of_sync", Ack.NO);

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
    assertThat(getEvent(), equalTo("on-enter-failed"));

    client.stop();
    client.start();

    assertThat(getEvent(), equalTo("on-leave-failed"));
    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.stop();
  }

  @Test
  public void channelInfo() throws ExecutionException, InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .build();
    client.start();
    SubscriptionListener listener = logSubscriptionListener(
        SubscriptionListenerType.SUBSCRIBED,
        SubscriptionListenerType.SUBSCRIPTION_INFO
    );
    SubscriptionConfig config =
        new SubscriptionConfig(SubscriptionMode.SIMPLE, listener);
    client.createSubscription(channel, config);

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.publish(channel, "trigger_channel_info", Ack.NO);

    assertThat(getEvent(), equalTo("on-subscription-info"));

    client.stop();
  }

  @Test
  public void checkFilterAndFastForwardParameters() throws InterruptedException {
    RtmClient client = clientBuilder()
        .build();
    client.start();

    SubscriptionListener listener = new SubscriptionAdapter() {
      @Override
      public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
        addEvent("fast-forward-" + request.isFastForward());
        addEvent("filter-" + request.getFilter());
      }
    };

    SubscriptionConfig config =
        new SubscriptionConfig(SubscriptionMode.SIMPLE, listener)
            .setFilter("select * from `" + channel + "`");
    client.createSubscription(channel, config);

    assertThat(getEvent(), equalTo("fast-forward-true"));
    assertThat(getEvent(), equalTo("filter-select * from `" + channel + "`"));

    client.stop();
  }

  @Test
  public void checkPeriodParameter() throws InterruptedException {
    RtmClient client = clientBuilder()
        .build();
    client.start();

    SubscriptionListener listener = new SubscriptionAdapter() {
      @Override
      public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
        addEvent("request-period-" + request.getPeriod());
      }
    };

    SubscriptionConfig config =
        new SubscriptionConfig(SubscriptionMode.SIMPLE, listener)
            .setPeriod(1);
    client.createSubscription(channel, config);

    assertThat(getEvent(), equalTo("request-period-1"));

    client.stop();
  }

  @Test
  public void checkUnicodeWhileDisconnect() throws InterruptedException, ExecutionException {
    RtmClient clientSubscriber = clientBuilder().build();

    clientSubscriber.start();
    clientSubscriber.createSubscription(channel, SubscriptionMode.ADVANCED,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.SUBSCRIPTION_DATA
        ));

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    clientSubscriber.stop();

    assertThat(getEvent(), equalTo("on-leave-subscribed"));

    RtmClient clientPublisher = clientBuilder().build();
    clientPublisher.start();

    StringBuffer sb = new StringBuffer();
    sb.append(Character.toChars(127467));
    sb.append("Юникод");
    awaitFuture(clientPublisher.publish(channel, sb.toString(), Ack.YES));

    clientSubscriber.start();

    assertThat(getEvent(), equalTo("on-enter-subscribed"));
    assertThat(getEvent(), equalTo(sb.toString()));

    clientSubscriber.stop();
    clientPublisher.stop();

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
  }

  @Test
  public void checkContinuity() throws InterruptedException, ExecutionException {
    RtmClient clientSubscriber = clientBuilder().build();

    clientSubscriber.start();
    clientSubscriber
        .createSubscription(channel, SubscriptionMode.SIMPLE, logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.SUBSCRIPTION_DATA
        ));

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    clientSubscriber.stop();

    assertThat(getEvent(), equalTo("on-leave-subscribed"));

    RtmClient clientPublisher = clientBuilder().build();
    clientPublisher.start();
    awaitFuture(clientPublisher.publish(channel, "test1", Ack.YES));

    clientSubscriber.start();

    assertThat(getEvent(), equalTo("on-enter-subscribed"));
    assertThat(getEvent(), equalTo("test1"));

    clientSubscriber.stop();
    clientPublisher.stop();

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
  }

  @Test
  public void rememberPositionFromDataAndResubscribeWithIt() throws InterruptedException {
    final AtomicReference<String> firstNext = new AtomicReference<String>(null);

    RtmClient client = clientBuilder().build();
    client.start();
    SubscriptionAdapter listener = new SubscriptionAdapter() {
      @Override
      public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
        dispatcher.add("enter-subscribed");
      }

      @Override
      public void onSubscriptionData(SubscriptionData data) {
        firstNext.compareAndSet(null, data.getPosition());
        for (String message : data.getMessagesAsStrings()) {
          dispatcher.add(message);
        }
      }
    };
    client.createSubscription(channel, SubscriptionMode.SIMPLE, listener);

    assertThat(getEvent(), equalTo("enter-subscribed"));

    client.publish(channel, "text1", Ack.NO);

    assertThat(getEvent(), equalTo("text1"));

    client.publish(channel, "text2", Ack.NO);
    client.publish(channel, "text3", Ack.NO);

    assertThat(getEvent(), equalTo("text2"));
    assertThat(getEvent(), equalTo("text3"));

    client.removeSubscription(channel);
    SubscriptionConfig config =
        new SubscriptionConfig(SubscriptionMode.SIMPLE, listener);
    config.setPosition(firstNext.get());
    client.createSubscription(channel, config);

    assertThat(getEvent(), equalTo("enter-subscribed"));
    assertThat(getEvent(), equalTo("text2"));
    assertThat(getEvent(), equalTo("text3"));

    client.stop();
  }

  @Test
  public void publishWithComplexObject() throws InterruptedException, ExecutionException {
    RtmClient client = clientBuilder().build();
    client.start();

    client.createSubscription(channel, SubscriptionMode.RELIABLE,
        new SubscriptionAdapter() {
          @Override
          public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
            dispatcher.add("on-enter-subscribed");
          }

          @Override
          public void onSubscriptionData(SubscriptionData data) {
            for (MyCustomBody message : data.getMessagesAsType(MyCustomBody.class)) {
              dispatcher.add(String.format("%s-%s", message.fieldA, message.fieldB));
            }
          }
        });
    assertThat(getEvent(), equalTo("on-enter-subscribed"));
    awaitFuture(client.publish(channel, new MyCustomBody("valueA", "valueB"), Ack.YES));

    assertThat(getEvent(), equalTo("valueA-valueB"));

    client.stop();
  }

  @Test
  public void onAlienMessageFromServerShouldCallOnError() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .setListener(logClientListener(ClientListenerType.ERROR))
        .build();

    client.start();
    client.publish(channel, "create_alien", Ack.NO);

    assertThat(getEvent(), startsWith("on-error-Unexpected PDU received"));

    client.stop();
  }


  @Test
  public void allPendingPublishesShouldBeCancelledAfterDisconnect()
      throws InterruptedException, ExecutionException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .setListener(logClientListener(ClientListenerType.AWAITING))
        .build();

    client.start();

    ListenableFuture<Pdu<PublishReply>> reply1 =
        client.publish("channel", "packet_without_response", Ack.YES);
    ListenableFuture<Pdu<PublishReply>> reply2 =
        client.publish("channel", "packet_without_response", Ack.YES);

    client.publish(channel, "force_disconnect", Ack.NO);

    assertThat(getEvent(), equalTo("on-enter-awaiting"));
    assertThat(reply1.isCancelled(), is(true));
    assertThat(reply2.isCancelled(), is(true));

    client.stop();
  }

  @Test
  public void publishNoAck() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .build();

    client.start();

    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.SUBSCRIPTION_DATA
        ));

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.publish(channel, "no-ack-test1", Ack.NO);
    client.publish(channel, "no-ack-test2", Ack.NO);

    assertThat(getEvent(), equalTo("no-ack-test1"));
    assertThat(getEvent(), equalTo("no-ack-test2"));

    client.stop();

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
  }


  @Test
  public void checkLastNSubscription() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .build();
    client.start();

    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.SUBSCRIPTION_DATA
        ));

    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.publish(channel, "test1", Ack.NO);
    client.publish(channel, "test2", Ack.NO);
    client.publish(channel, "test3", Ack.NO);

    assertThat(getEvent(), equalTo("test1"));
    assertThat(getEvent(), equalTo("test2"));
    assertThat(getEvent(), equalTo("test3"));


    client.removeSubscription(channel);

    assertThat(getEvent(), equalTo("on-leave-subscribed"));

    client.stop();
    client.start();
    SubscriptionConfig cfg = new SubscriptionConfig(SubscriptionMode.SIMPLE,
        logSubscriptionListener(SubscriptionListenerType.SUBSCRIPTION_DATA));
    cfg.setCount(1);
    client.createSubscription(channel, cfg);

    assertThat(getEvent(), equalTo("test3"));

    client.removeSubscription(channel);
    client.stop();
  }

  @Test
  public void receiveGlobalErrorWithoutDisconnectFromServer() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .setListener(logClientListener(
            ClientListenerType.CONNECTED,
            ClientListenerType.ERROR
        ))
        .build();

    client.start();

    assertThat(getEvent(), equalTo("on-enter-connected"));

    client.publish("channel", "system_wide_error_without_server_disconnect", Ack.YES);

    assertThat(getEvent(), startsWith("on-error-System Wide error received"));
    assertThat(getEvent(), equalTo("on-leave-connected"));

    client.stop();
  }

  @Test
  @Ignore
  public void receiveGlobalErrorWithDisconnectFromServer() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .setListener(logClientListener(
            ClientListenerType.CONNECTED,
            ClientListenerType.ERROR
        ))
        .build();

    client.start();

    assertThat(getEvent(), equalTo("on-enter-connected"));

    client.publish("channel", "system_wide_error_with_server_disconnect", Ack.YES);

    assertThat(getEvent(), equalTo(
        "on-error-Message: {\"error\":\"system_wide_error\",\"error_text\":\"system_wide_desc\"}"));
    assertThat(getEvent(), equalTo("on-leave-connected"));

    client.stop();
  }

  @Test
  public void receiveUnknownOutcome() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .setListener(logClientListener(
            ClientListenerType.CONNECTED
        ))
        .build();

    client.start();

    assertThat(getEvent(), equalTo("on-enter-connected"));

    try {
      awaitFuture(client.publish("channel", "unknown_outcome", Ack.YES));
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof PduException) {
        dispatcher.add("on-unknown-outcome");
      }
    }

    assertThat(getEvent(), equalTo("on-unknown-outcome"));

    client.stop();

    assertThat(getEvent(), equalTo("on-leave-connected"));
  }

  @Test
  public void getChannelPositionAndSubscribeFromIt()
      throws ExecutionException, InterruptedException {
    RtmClient client = clientBuilder()
        .build();
    client.start();
    Pdu<PublishReply> reply = awaitFuture(client.publish(channel, "test-a", Ack.YES));
    awaitFuture(client.publish(channel, "test-b", Ack.YES));
    String next = reply.getBody().getPosition();
    SubscriptionConfig cfg = new SubscriptionConfig(SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIPTION_DATA
        ));
    cfg.setPosition(next);
    client.createSubscription(channel, cfg);

    assertThat(getEvent(), equalTo("test-a"));
    assertThat(getEvent(), equalTo("test-b"));

    client.stop();
  }

  @Test
  public void twoChannelsTest() throws InterruptedException {
    RtmClient client = clientBuilder().build();
    client.start();

    String secondChannel = generateChannel("secondChannel");

    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.SUBSCRIPTION_DATA
        ));

    client.createSubscription(secondChannel, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.UNSUBSCRIBED,
            SubscriptionListenerType.SUBSCRIPTION_DATA
        ));

    assertThat(getEvent(), equalTo("on-enter-unsubscribed"));
    assertThat(getEvent(), equalTo("on-leave-unsubscribed"));

    assertThat(getEvent(), equalTo("on-enter-subscribed"));
    assertThat(getEvent(), equalTo("on-enter-subscribed"));

    client.publish(channel, "t1", Ack.NO);
    assertThat(getEvent(), equalTo("t1"));

    client.publish(secondChannel, "t2", Ack.NO);
    assertThat(getEvent(), equalTo("t2"));

    client.removeSubscription(secondChannel);

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
    assertThat(getEvent(), equalTo("on-enter-unsubscribed"));

    client.publish(secondChannel, "n2", Ack.NO);
    client.publish(channel, "n1", Ack.NO);

    assertThat(getEvent(), equalTo("n1"));

    client.stop();
  }

  @Test
  public void reconnectOnSubscriptionError() throws InterruptedException {
    final RtmClient client = clientBuilder().build();
    final AtomicInteger count = new AtomicInteger(0);
    client.start();

    client.createSubscription("$$$", SubscriptionMode.SIMPLE,
        new SubscriptionAdapter() {
          @Override
          public void onSubscriptionError(SubscriptionError error) {
            dispatcher.add("channel-error");
            if (count.getAndIncrement() < 3) {
              client.restart();
            }
          }
        });
    assertThat(getEvent(), equalTo("channel-error"));
    assertThat(getEvent(), equalTo("channel-error"));
    assertThat(getEvent(), equalTo("channel-error"));
    client.stop();
  }

  @Test
  public void publishReplyWithoutBody() throws ExecutionException {
    final RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .build();
    client.start();
    ListenableFuture<Pdu<PublishReply>> message =
        client.publish(channel, "publish_reply_without_body", Ack.YES);
    Pdu<PublishReply> pdu = awaitFuture(message);
    assertThat(pdu.getAction(), equalTo("rtm/publish/ok"));
    assertThat(pdu.getBody(), is(nullValue()));
  }

  @Test
  public void submitNullsShouldBeOk() throws ExecutionException {
    RtmClient client = clientBuilder().build();
    client.start();
    awaitFuture(client.publish(channel, null, Ack.YES));
    awaitFuture(client.write(channel, null, Ack.YES));
    client.stop();
  }

  public static class MyCustomBody {
    String fieldA;
    String fieldB;

    public MyCustomBody() {
    }

    public MyCustomBody(String fieldA, String fieldB) {
      this.fieldA = fieldA;
      this.fieldB = fieldB;
    }
  }
}
