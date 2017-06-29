package examples;

import com.satori.rtm.*;
import com.satori.rtm.model.*;

public class SubscribeWithMultipleViews {
  static String endpoint = "YOUR_ENDPOINT";
  static String appkey = "YOUR_APPKEY";

  public static void main(String[] args) throws InterruptedException {
    final RtmClient client = new RtmClientBuilder(endpoint, appkey)
        .setListener(new RtmClientAdapter() {
          @Override
          public void onError(RtmClient client, Exception ex) {
            System.out.println("Error occurred: " + ex.getMessage());
          }

          @Override
          public void onConnectingError(RtmClient client, Exception ex) {
            System.out.println("Error occurred: " + ex.getMessage());
          }

          @Override
          public void onEnterConnected(RtmClient client) {
            System.out.println("Connected to Satori RTM!");
          }
        })
        .build();


    SubscriptionAdapter listener = new SubscriptionAdapter() {
      @Override
      public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
        System.out.println("Subscribed to: " + reply.getSubscriptionId());
      }

      @Override
      public void onLeaveSubscribed(SubscribeRequest request, SubscribeReply reply) {
        System.out.println("Unsubscribed from: " + reply.getSubscriptionId());
      }

      @Override
      public void onSubscriptionError(SubscriptionError error) {
        String txt = String.format(
            "Subscription failed. RTM sent the error %s: %s", error.getError(), error.getReason());
        System.out.println(txt);
      }

      @Override
      public void onSubscriptionData(SubscriptionData data) {
        for (AnyJson json : data.getMessages()) {
          if ("zebras".equals(data.getSubscriptionId())) {
            System.out.println("Got a zebra: " + json);
          } else {
            System.out.println("Got a count: " + json);
          }
        }
      }
    };

    SubscriptionConfig zebraCfg = new SubscriptionConfig(SubscriptionMode.SIMPLE, listener)
        .setFilter("SELECT * FROM `animals` WHERE who = 'zebra'");
    client.createSubscription("zebras", zebraCfg);

    SubscriptionConfig statsCfg = new SubscriptionConfig(SubscriptionMode.SIMPLE, listener)
        .setFilter("SELECT count(*) as count, who FROM `animals` GROUP BY who");
    client.createSubscription("stats", statsCfg);

    client.start();
  }
}
