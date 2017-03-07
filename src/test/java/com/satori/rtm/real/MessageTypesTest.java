package com.satori.rtm.real;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.satori.rtm.Ack;
import com.satori.rtm.RtmClient;
import com.satori.rtm.SubscriptionAdapter;
import com.satori.rtm.SubscriptionMode;
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
    final BlockingQueue<JsonElement> queue = new LinkedBlockingQueue<JsonElement>();

    RtmClient client = clientBuilder().build();
    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        new SubscriptionAdapter() {
          @Override
          public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
            addEvent("enter-subscribed");
          }

          @Override
          public void onSubscriptionData(SubscriptionData data) {
            for (JsonElement el : data.getMessagesAsType(JsonElement.class)) {
              if (null != el) {
                queue.add(el);
              } else {
                queue.add(JsonNull.INSTANCE);
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

    assertThat(queue.poll(15, TimeUnit.SECONDS), is((JsonElement) JsonNull.INSTANCE));
    assertThat(queue.poll(15, TimeUnit.SECONDS).getAsDouble(), is(Math.PI));
    assertThat(queue.poll(15, TimeUnit.SECONDS).getAsInt(), is(42));
    assertThat(queue.poll(15, TimeUnit.SECONDS).getAsString(), is("Сообщение"));
    assertThat(queue.poll(15, TimeUnit.SECONDS).getAsBoolean(), is(true));
    assertThat(queue.poll(15, TimeUnit.SECONDS).getAsBoolean(), is(false));
    assertThat(queue.poll(15, TimeUnit.SECONDS).getAsLong(), is(1L << 57));
    assertThat(queue.poll(15, TimeUnit.SECONDS).getAsJsonArray(), is(new JsonArray()));
    assertThat(queue.poll(15, TimeUnit.SECONDS).getAsJsonObject(), is(new JsonObject()));
  }
}
