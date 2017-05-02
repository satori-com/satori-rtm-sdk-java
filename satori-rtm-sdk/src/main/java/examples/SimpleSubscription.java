package examples;

import com.satori.rtm.Ack;
import com.satori.rtm.RtmClient;
import com.satori.rtm.RtmClientBuilder;
import com.satori.rtm.SubscriptionAdapter;
import com.satori.rtm.SubscriptionMode;
import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import com.satori.rtm.model.SubscriptionError;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SimpleSubscription {
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
        System.out.println("Subscribed to: " + reply.getSubscriptionId());
        client.publish("mychannel", "message", Ack.NO);
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
    client.createSubscription("mychannel", SubscriptionMode.SIMPLE, listener);

    signal.await(15, TimeUnit.SECONDS);
    client.shutdown();
  }
}
