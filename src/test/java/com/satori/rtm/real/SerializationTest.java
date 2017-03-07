package com.satori.rtm.real;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.satori.rtm.Ack;
import com.satori.rtm.RtmClient;
import com.satori.rtm.RtmClientAdapter;
import com.satori.rtm.SubscriptionMode;
import com.satori.rtm.model.InvalidJsonException;
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
    assertThat(getEvent(), equalTo("json-exception-}{ bad_json_from_server }{"));
    client.stop();
    assertThat(getEvent(), equalTo("on-leave-subscribed"));
  }
}
