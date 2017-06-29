package examples;

import com.google.common.util.concurrent.*;
import com.satori.rtm.*;
import com.satori.rtm.model.*;
import java.util.concurrent.*;

public class Publish {
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
    client.start();


    String channelName = "animals";
    Animal message = new Animal("zebra", new float[]{34.134358f, -118.321506f});
    ListenableFuture<Pdu<PublishReply>> reply = client.publish(channelName, message, Ack.YES);

    Futures.addCallback(reply, new FutureCallback<Pdu<PublishReply>>() {
      public void onSuccess(Pdu<PublishReply> pdu) {
        System.out.println("Publish confirmed");
      }

      public void onFailure(Throwable caught) {
        try {
          throw caught;
        } catch (PduException e) {
          PduRaw pdu = e.getPdu();
          CommonError error = pdu.convertBodyTo(CommonError.class).getBody();
          System.out.println(String.format("Failed to publish. RTM replied with the error %s: %s",
              error.getError(), error.getReason()));
        } catch (Throwable e) {
          System.out.println("Failed to publish: " + e);
        }
      }
    });

    Futures.<Object>whenAllComplete(reply).call(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        client.shutdown();
        return null;
      }
    });
  }
}
