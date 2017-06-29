package examples;

import com.satori.rtm.*;
import com.satori.rtm.model.*;

public class SubscribeToOpenChannel {
  static String endpoint = "YOUR_ENDPOINT";
  static String appkey = "YOUR_APPKEY";
  static String channelName = "OPEN_CHANNEL";

  public static void main(String[] args) throws InterruptedException {
    final RtmClient client = new RtmClientBuilder(endpoint, appkey)
        .setListener(new RtmClientAdapter() {
          @Override
          public void onEnterConnected(RtmClient client) {
            System.out.println("Connected to Satori RTM!");
          }
        })
        .build();


    SubscriptionAdapter listener = new SubscriptionAdapter() {
      @Override
      public void onSubscriptionData(SubscriptionData data) {
        for (AnyJson json : data.getMessages()) {
          System.out.println("Got message: " + json.toString());
        }
      }
    };

    client.createSubscription(channel, SubscriptionMode.SIMPLE, listener);

    client.start();
  }
}
