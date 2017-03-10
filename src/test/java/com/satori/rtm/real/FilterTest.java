package com.satori.rtm.real;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.google.gson.JsonObject;
import com.satori.rtm.Ack;
import com.satori.rtm.RtmClient;
import com.satori.rtm.SubscriptionAdapter;
import com.satori.rtm.SubscriptionConfig;
import com.satori.rtm.SubscriptionMode;
import com.satori.rtm.model.AnyJson;
import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import org.junit.Test;

public class FilterTest extends AbstractRealTest {
  @Test
  public void allowToHaveTwoSubscriptionWithDifferentName() throws InterruptedException {
    RtmClient client = clientBuilder()
        .build();
    client.start();

    SubscriptionConfig config = new SubscriptionConfig(
        SubscriptionMode.RELIABLE,
        new SubscriptionAdapter() {
          @Override
          public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
            addEvent(reply.getSubscriptionId());
          }

          @Override
          public void onSubscriptionData(SubscriptionData data) {
            for (AnyJson json : data.getMessages()) {
              addEvent(json.toString());
            }
          }
        })
        .setFilter("select * FROM `" + channel + "`");

    client.createSubscription("s1", config);
    assertThat(getEvent(), equalTo("s1"));

    client.createSubscription("s2", config);
    assertThat(getEvent(), equalTo("s2"));

    JsonObject json = new JsonObject();
    json.addProperty("foo", "bar");
    client.publish(channel, json, Ack.NO);

    assertThat(getEvent(), equalTo("{\"foo\":\"bar\"}"));
    assertThat(getEvent(), equalTo("{\"foo\":\"bar\"}"));

    client.stop();
  }

  @Test
  public void incorrectFilter() throws InterruptedException {
    RtmClient client = clientBuilder()
        .build();

    SubscriptionConfig config =
        new SubscriptionConfig(SubscriptionMode.RELIABLE,
            logSubscriptionListener(
                SubscriptionListenerType.FAILED,
                SubscriptionListenerType.SUBSCRIPTION_ERROR
            )
        );
    config.setFilter("wrong_filter");
    client.createSubscription(channel, config);

    client.start();

    assertThat(getEvent(), equalTo("invalid_filter"));
    assertThat(getEvent(), equalTo("on-enter-failed"));

    client.stop();
  }


  @Test(expected = IllegalStateException.class)
  public void channelAndFilterSubscription() throws InterruptedException {
    RtmClient client = clientBuilder()
        .build();
    SubscriptionAdapter listener = new SubscriptionAdapter();
    client.createSubscription(channel, new SubscriptionConfig(
        SubscriptionMode.RELIABLE,
        listener));

    client.createSubscription(channel, new SubscriptionConfig(
        SubscriptionMode.RELIABLE,
        listener).setFilter("select * FROM `" + channel + "`"));
  }
}
