package examples;

import com.satori.rtm.Ack;
import com.satori.rtm.RtmClient;
import com.satori.rtm.RtmClientBuilder;
import com.satori.rtm.SubscriptionAdapter;
import com.satori.rtm.SubscriptionConfig;
import com.satori.rtm.SubscriptionMode;
import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import com.satori.rtm.model.SubscriptionError;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FilterSubscription {
  static String endpoint = "<ENDPOINT>";
  static String appkey = "<APPKEY>";


  public static void main(String[] args) throws InterruptedException {
    final RtmClient client = new RtmClientBuilder(endpoint, appkey)
        .build();
    final CountDownLatch signal = new CountDownLatch(1);

    client.start();

    SubscriptionAdapter listener = new SubscriptionAdapter() {
      @Override
      public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
        System.out.println("Subscribed to channel: " + reply.getSubscriptionId());

        // check subscription
        Map<String, Integer> obj = new HashMap<String, Integer>();
        obj.put("a", 1);
        obj.put("b", 2);

        client.publish("mychannel", obj, Ack.NO);
      }

      @Override
      public void onSubscriptionError(SubscriptionError error) {
        System.out.println("Subscription is failed: " + error.getError());
      }

      @Override
      public void onSubscriptionData(SubscriptionData data) {
        for (Map map: data.getMessagesAsType(Map.class)) {
          System.out.println("{a=" + map.get("a") + ", MAX(b)=" + map.get("MAX(b)") + "}");
        }
        signal.countDown();
      }
    };

    SubscriptionConfig cfg = new SubscriptionConfig(SubscriptionMode.SIMPLE, listener)
        .setFilter("SELECT a, MAX(b) FROM mychannel GROUP BY a");

    client.createSubscription("filter", cfg);

    signal.await(15, TimeUnit.SECONDS);
    client.shutdown();
  }
}
