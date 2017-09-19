package com.satori.rtm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduException;
import com.satori.rtm.model.ReadReply;
import com.satori.rtm.model.WriteReply;
import com.satori.rtm.model.WriteRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.concurrent.ExecutionException;

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
  public void casSuccess() throws ExecutionException {
    RtmClient client = clientBuilder().build();
    client.start();
    Pdu<WriteReply> reply = awaitFuture(client.write(channel, "value1", Ack.YES));
    awaitFuture(client
        .write(new WriteRequest<String>(channel, "value2", reply.getBody().getPosition()),
            Ack.YES));
    Pdu<ReadReply> pdu = awaitFuture(client.read(channel));
    assertThat(pdu.getBody().getMessageAsType(String.class), equalTo("value2"));
    client.stop();
  }

  @Test
  public void casFailed() throws InterruptedException {
    RtmClient client = clientBuilder().build();
    client.start();
    ListenableFuture<Pdu<WriteReply>> future =
        client.write(new WriteRequest<String>(channel, "value2", "bad_position"), Ack.YES);
    try {
      future.get();
      assertThat(false, is(true));
    } catch (ExecutionException e) {
      assertThat(e.getCause(), instanceOf(PduException.class));
      assertThat(((PduException) e.getCause()).getReply().getError(), equalTo("invalid_format"));
    }
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
}
