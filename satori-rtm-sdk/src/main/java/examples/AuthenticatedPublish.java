package examples;

import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.Ack;
import com.satori.rtm.RtmClient;
import com.satori.rtm.RtmClientBuilder;
import com.satori.rtm.auth.RoleSecretAuthProvider;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PublishReply;
import java.util.concurrent.ExecutionException;

public class AuthenticatedPublish {
  static String endpoint = "<ENDPOINT>";
  static String appkey = "<APPKEY>";
  static String role = "<ROLE>";
  static String roleSecret = "<ROLE_SECRET>";

  public static void main(String[] args) throws InterruptedException {
    final RtmClient client = new RtmClientBuilder(endpoint, appkey)
        .setAuthProvider(new RoleSecretAuthProvider(role, roleSecret))
        .build();
    client.start();

    ListenableFuture<Pdu<PublishReply>> future = client.publish("channel", "message", Ack.YES);

    try {
      Pdu<PublishReply> reply = future.get();
      System.out.println("Message is published: " + reply.toString());
    } catch (ExecutionException ex) {
      System.out.println("Publish is failed: " + ex.getCause().getMessage());
    }

    client.shutdown();
  }
}
