package examples;

import com.satori.rtm.*;
import com.satori.rtm.auth.*;

public class Authenticate {
  static String endpoint = "YOUR_ENDPOINT";
  static String appkey = "YOUR_APPKEY";
  static String role = "YOUR_ROLE";
  static String roleSecret = "YOUR_SECRET";

  public static void main(String[] args) throws InterruptedException {
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
            } else {
              System.out.println("Error occurred: " + ex.getMessage());
            }
          }

          @Override
          public void onEnterConnected(RtmClient client) {
            System.out.println("Connected to Satori RTM and authenticated as " + role);
          }
        })
        .build();

    client.start();
  }
}
