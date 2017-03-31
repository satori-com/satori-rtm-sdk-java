package examples;

import com.google.common.collect.Lists;
import com.satori.rtm.Ack;
import com.satori.rtm.RtmClient;
import com.satori.rtm.RtmClientBuilder;
import com.satori.rtm.SubscriptionAdapter;
import com.satori.rtm.SubscriptionMode;
import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CustomObjectSubscription {
  static String endpoint = "<ENDPOINT>";
  static String appkey = "<APPKEY>";

  public static void main(String[] args) throws InterruptedException {
    final RtmClient client = new RtmClientBuilder(endpoint, appkey)
        .build();
    final CountDownLatch signal = new CountDownLatch(1);

    client.start();

    client.createSubscription("mychannel", SubscriptionMode.SIMPLE,
        new SubscriptionAdapter() {
          @Override
          public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
            Person mike = new Person("Mike", 8, null);
            Person alice = new Person("Alice", 3, null);
            Person john = new Person("John", 32, Lists.newArrayList(mike, alice));
            client.publish("mychannel", john, Ack.NO);
          }

          @Override
          public void onSubscriptionData(SubscriptionData data) {
            for (Person person : data.getMessagesAsType(Person.class)) {
              System.out.println("Got person: " + person);
            }
            signal.countDown();
          }
        });

    signal.await(15, TimeUnit.SECONDS);
    client.shutdown();
  }

  static class Person {
    String name;
    Integer age;
    List<Person> children;

    public Person() { }

    public Person(String name, Integer age, List<Person> children) {
      this.name = name;
      this.age = age;
      this.children = children;
    }

    @Override
    public String toString() {
      return String.format("(name: %s, age: %s, children: %s)", name, age, children);
    }
  }
}
