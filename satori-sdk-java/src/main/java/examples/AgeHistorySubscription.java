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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AgeHistorySubscription {
  static String endpoint = "<ENDPOINT>";
  static String appkey = "<APPKEY>";
  
  public static void main(String[] args) throws Exception {
    final RtmClient client = new RtmClientBuilder(endpoint, appkey)
        .build();
    client.start();

    // publish any message before creating subscription
    client.publish("mychannel", "message", Ack.YES).get();

    final CountDownLatch signal = new CountDownLatch(1);


    SubscriptionAdapter listener = new SubscriptionAdapter() {
      @Override
      public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
        System.out.println("Subscribed to channel: " + reply.getSubscriptionId());
      }

      @Override
      public void onSubscriptionError(SubscriptionError error) {
        System.out.println("Subscription is failed: " + error.getError());
      }

      @Override
      public void onSubscriptionData(SubscriptionData data) {
        for (String msg: data.getMessagesAsStrings()) {
          System.out.println("Got message: " + msg);
        }
        signal.countDown();
      }
    };


    SubscriptionConfig cfg = new SubscriptionConfig(SubscriptionMode.SIMPLE, listener)
        .setAge(60 /* seconds */);
    client.createSubscription("mychannel", cfg);

    signal.await(15, TimeUnit.SECONDS);
    client.shutdown();
  }
}
