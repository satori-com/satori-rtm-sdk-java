package examples;

import com.google.common.base.Strings;
import com.satori.rtm.Ack;
import com.satori.rtm.RtmClient;
import com.satori.rtm.RtmClientBuilder;
import com.satori.rtm.SubscriptionAdapter;
import com.satori.rtm.SubscriptionMode;
import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BasicPublishSubscribe {
  public static void main(String[] args) throws InterruptedException {
    String endpoint = (0 < args.length) ? args[0] : null;
    String appKey = (1 < args.length) ? args[1] : null;

    if (Strings.isNullOrEmpty(endpoint) || Strings.isNullOrEmpty(appKey)) {
      System.out.println("Usage: ./program <endpoint> <appkey>");
      System.exit(1);
    }

    final CountDownLatch signal = new CountDownLatch(1);
    final RtmClient client = new RtmClientBuilder(endpoint, appKey)
        .build();

    client.start();
    client.createSubscription("channel", SubscriptionMode.SIMPLE,
        new SubscriptionAdapter() {
          @Override
          public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
            client.publish("channel", "message", Ack.NO);
          }

          @Override
          public void onSubscriptionData(SubscriptionData data) {
            System.out.println("onSubscriptionData: " + data);
            signal.countDown();
          }
        });
    signal.await(15, TimeUnit.SECONDS);
    client.shutdown();
  }
}
