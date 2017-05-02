package com.satori.rtm;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.satori.rtm.model.InvalidJsonException;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SerializationTest extends AbstractRealTest {
  @Test
  public void serverReturnedBadJson() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .setListener(new RtmClientAdapter() {
          @Override
          public void onError(RtmClient client, Exception ex) {
            try {
              throw ex;
            } catch (InvalidJsonException invalidJsonException) {
              dispatcher.add("json-exception-" + invalidJsonException.getJson());
            } catch (Throwable t) {
              // ignore;
            }
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

    client.publish(channel, "bad_json", Ack.YES);
    MatcherAssert
        .assertThat(getEvent(), equalTo("json-exception-}{ bad_json_from_server }{"));
    client.stop();
    assertThat(getEvent(), equalTo("on-leave-subscribed"));
  }
}
