package examples;

import com.satori.rtm.RtmClient;
import com.satori.rtm.RtmClientAdapter;
import com.satori.rtm.RtmClientBuilder;
import com.satori.rtm.auth.AuthException;
import com.satori.rtm.auth.RoleSecretAuthProvider;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Authentication {
  static String endpoint = "<ENDPOINT>";
  static String appkey = "<APPKEY>";
  static String role = "<ROLE>";
  static String roleSecret = "<ROLE_SECRET>";

  public static void main(String[] args) throws InterruptedException {
    final CountDownLatch signal = new CountDownLatch(1);
    final RtmClient client = new RtmClientBuilder(endpoint, appkey)
        .setAuthProvider(new RoleSecretAuthProvider(role, roleSecret))
        .setListener(new RtmClientAdapter() {
          @Override
          public void onConnectingError(RtmClient client, Exception ex) {
            System.out.println("Failed to connect: " + ex.getMessage());
          }

          @Override
          public void onError(RtmClient client, Exception ex) {
            if (ex instanceof AuthException) {
              System.out.println("Failed to authenticate: " + ex.getMessage());
            }
          }

          @Override
          public void onEnterConnected(RtmClient client) {
            System.out.println("Successfully connected and authenticated");
            signal.countDown();
          }
        })
        .build();

    client.start();

    signal.await(15, TimeUnit.SECONDS);
    client.shutdown();
  }
}
