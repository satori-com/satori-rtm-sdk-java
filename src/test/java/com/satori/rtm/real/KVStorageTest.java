package com.satori.rtm.real;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.Ack;
import com.satori.rtm.Callback;
import com.satori.rtm.RtmClient;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.ReadReply;
import com.satori.rtm.model.SearchReply;
import com.satori.rtm.model.WriteReply;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RunWith(JUnit4.class)
public class KVStorageTest extends AbstractRealTest {
  @Test
  public void readAfterPublish() throws ExecutionException, InterruptedException {
    RtmClient client = clientBuilder().build();
    client.start();

    awaitFuture(client.publish(channel, "message", Ack.YES));
    Pdu<ReadReply> pdu = awaitFuture(client.read(channel));
    assertThat(pdu.getBody().getMessageAsType(String.class), equalTo("message"));
    client.stop();
  }

  @Test
  public void doubleWriteAndOneRead() throws ExecutionException {
    RtmClient client = clientBuilder().build();
    client.start();
    awaitFuture(client.write(channel, "value1", Ack.YES));
    awaitFuture(client.write(channel, "value2", Ack.YES));
    Pdu<ReadReply> pdu = awaitFuture(client.read(channel));
    assertThat(pdu.getBody().getMessageAsType(String.class), equalTo("value2"));
    client.stop();
  }

  @Test
  public void writeDeleteRead() throws ExecutionException {
    RtmClient client = clientBuilder().build();
    client.start();

    awaitFuture(client.read(channel));
    awaitFuture(client.write(channel, "value", Ack.YES));
    awaitFuture(client.write(channel, "value2", Ack.YES));

    Pdu<ReadReply> pdu = awaitFuture(client.read(channel));
    assertThat(pdu.getBody().getMessageAsType(String.class), equalTo("value2"));

    awaitFuture(client.delete(channel, Ack.YES));
    pdu = awaitFuture(client.read(channel));
    assertThat(pdu.getBody().getMessageAsType(String.class), is(nullValue()));

    client.stop();
  }

  @Test
  public void search() throws ExecutionException, InterruptedException {
    RtmClient client = clientBuilder().build();
    client.start();

    final int numOfChannels = 5;
    final List<String> channels = new ArrayList<String>();
    final CountDownLatch lock = new CountDownLatch(1);


    // create some keys with same prefix
    List<ListenableFuture<Pdu<WriteReply>>> col =
        new ArrayList<ListenableFuture<Pdu<WriteReply>>>();
    for (int i = 0; i < numOfChannels; i++) {
      col.add(client.write(channel + i, "value", Ack.YES));
    }
    awaitFuture(Futures.successfulAsList(col));

    // search by prefix
    client.search(channel, new Callback<Pdu<SearchReply>>() {
      @Override
      public void onResponse(Pdu<SearchReply> result) {
        channels.addAll(result.getBody().getChannels());
        if ("rtm/search/ok".equals(result.getAction())) {
          lock.countDown();
        }
      }

      @Override
      public void onFailure(Throwable t) { }
    });

    boolean isAwaitSuccessful = lock.await(5, TimeUnit.SECONDS);
    assertThat(isAwaitSuccessful, is(true));
    for (int i = 0; i < numOfChannels; i++) {
      assertThat(channels, hasItem(channel + i));
    }
    client.stop();
  }
}
