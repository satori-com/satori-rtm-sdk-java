package examples;

import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.Ack;
import com.satori.rtm.RtmClient;
import com.satori.rtm.RtmClientBuilder;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PublishReply;
import java.util.concurrent.ExecutionException;

public class CustomObjectPublish {
  static String endpoint = "<ENDPOINT>";
  static String appkey = "<APPKEY>";

  public static void main(String[] args) throws InterruptedException {
    final RtmClient client = new RtmClientBuilder(endpoint, appkey)
        .build();
    client.start();

    Person mike = new Person("Mike", 10);
    ListenableFuture<Pdu<PublishReply>> future = client.publish("channel", mike, Ack.YES);

    try {
      Pdu<PublishReply> reply = future.get();
      System.out.println("Message is published: " + reply.toString());
    } catch (ExecutionException ex) {
      System.out.println("Publish is failed: " + ex.getCause().getMessage());
    }

    client.shutdown();
  }


  static class Person {
    String name;
    Integer age;

    public Person() { }

    public Person(String name, Integer age) {
      this.name = name;
      this.age = age;
    }

    @Override
    public String toString() {
      return String.format("(name: %s, age: %s)", name, age);
    }
  }
}
