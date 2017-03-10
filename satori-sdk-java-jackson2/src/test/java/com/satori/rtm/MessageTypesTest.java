package com.satori.rtm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@RunWith(JUnit4.class)
public class MessageTypesTest extends AbstractRealTest {
  @Test
  public void primitiveTypes() throws ExecutionException, InterruptedException {
    final BlockingQueue<JsonNode> queue = new LinkedBlockingQueue<JsonNode>();

    RtmClient client = clientBuilder().build();
    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        new SubscriptionAdapter() {
          @Override
          public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
            addEvent("enter-subscribed");
          }

          @Override
          public void onSubscriptionData(SubscriptionData data) {
            for (JsonNode el : data.getMessagesAsType(JsonNode.class)) {
              if (null != el) {
                queue.add(el);
              } else {
                queue.add(NullNode.instance);
              }
            }
          }
        });
    client.start();

    assertThat(getEvent(), is("enter-subscribed"));

    client.publish(channel, null, Ack.NO);
    client.publish(channel, Math.PI, Ack.NO);
    client.publish(channel, 42, Ack.NO);
    client.publish(channel, "Сообщение", Ack.NO);
    client.publish(channel, true, Ack.NO);
    client.publish(channel, false, Ack.NO);
    client.publish(channel, 1L << 57, Ack.NO);
    client.publish(channel, Collections.emptyList(), Ack.NO);
    client.publish(channel, Collections.emptyMap(), Ack.NO);

    assertThat(queue.poll(15, TimeUnit.SECONDS).isNull(), is(true));
    assertThat(queue.poll(15, TimeUnit.SECONDS).asDouble(), is(Math.PI));
    assertThat(queue.poll(15, TimeUnit.SECONDS).asInt(), is(42));
    assertThat(queue.poll(15, TimeUnit.SECONDS).asText(), is("Сообщение"));
    assertThat(queue.poll(15, TimeUnit.SECONDS).asBoolean(), is(true));
    assertThat(queue.poll(15, TimeUnit.SECONDS).asBoolean(), is(false));
    assertThat(queue.poll(15, TimeUnit.SECONDS).asLong(), is(1L << 57));
    assertThat(queue.poll(15, TimeUnit.SECONDS).isArray(), is(true));
    assertThat(queue.poll(15, TimeUnit.SECONDS).isObject(), is(true));
  }


  @Test
  public void publishComplexObjectWithJackson() throws InterruptedException, ExecutionException {
    RtmClient client = clientBuilder()
        .build();
    client.start();

    client.createSubscription(channel, SubscriptionMode.SIMPLE,
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

  private static class MyCustomBody {
    String fieldA;
    String fieldB;

    public MyCustomBody() { }

    MyCustomBody(String fieldA, String fieldB) {
      this.fieldA = fieldA;
      this.fieldB = fieldB;
    }
  }
}
