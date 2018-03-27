package com.satori.rtm;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.model.DeleteReply;
import com.satori.rtm.model.DeleteRequest;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduException;
import com.satori.rtm.model.PublishReply;
import com.satori.rtm.model.PublishRequest;
import com.satori.rtm.model.ReadReply;
import com.satori.rtm.model.WriteReply;
import com.satori.rtm.model.WriteRequest;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class KVStorageTest extends AbstractRealTest {
  @Parameters
  public static Iterable<? extends Object> requestReturnModes(){
    return Arrays.asList(ReqReadMode.values());
  }

  private ReqReadMode reqReadMode;
  public KVStorageTest(ReqReadMode returnMode){
    reqReadMode = returnMode;
  }

  @Test
  public void readAfterPublish() throws ExecutionException {
    RtmClient client = clientBuilder().build();
    client.start();

    awaitFuture(client.publish(channel, "message", Ack.YES));
    Pdu<ReadReply> pdu = awaitFuture(client.read(channel));
    assertThat(pdu.getBody().getMessageAsType(String.class), equalTo("message"));
    client.stop();
  }

  @Test
  public void publishResponseOK() throws ExecutionException {
    final String message = UUID.randomUUID().toString();
    final RtmClient client = clientBuilder().build();
    client.start();
    final PublishRequest<String> publishRequest = new PublishRequest<String>(channel, message,
        reqReadMode);
    awaitFuture(client.publish(publishRequest, Ack.YES));
    final Pdu<PublishReply> replyPdu = awaitFuture(client.publish(publishRequest, Ack.YES));
    final PublishReply publishReply = replyPdu.getBody();
    if (ReqReadMode.PREVIOUS_VALUE_ON_OK.equals(reqReadMode) || ReqReadMode.PREVIOUS_VALUE.equals(
        reqReadMode)) {
      assertNotNull(publishReply.getPrevious());
      assertNotNull(publishReply.getPrevious().getMessage());
      assertTrue(publishReply.getPrevious().getMessage().toString().equals(message));
    } else {
      assertNull(publishReply.getPrevious());
    }
    Pdu<ReadReply> pdu = awaitFuture(client.read(channel));
    assertThat(pdu.getBody().getMessageAsType(String.class), equalTo(message));
    client.stop();
  }

  @Test
  public void casResponseOK() throws ExecutionException {
    final String position = null;
    final String message = UUID.randomUUID().toString();
    final RtmClient client = clientBuilder().build();
    client.start();
    Pdu<WriteReply> replyPdu = awaitFuture(client.write(new WriteRequest<String>(channel, message, position,
        reqReadMode), Ack.YES));
    final String newPosition = replyPdu.getBody().getPosition();
    replyPdu = awaitFuture(client.write(new WriteRequest<String>(channel, message, newPosition,
        reqReadMode), Ack.YES));
    final WriteReply writeReply = replyPdu.getBody();
    if (ReqReadMode.PREVIOUS_VALUE_ON_OK.equals(reqReadMode) || ReqReadMode.PREVIOUS_VALUE.equals(
        reqReadMode)) {
      assertNotNull(writeReply.getPrevious());
      assertNotNull(writeReply.getPrevious().getMessage());
      assertTrue(writeReply.getPrevious().getMessage().toString().equals(message));
    } else {
      assertNull(writeReply.getPrevious());
    }
    Pdu<ReadReply> pdu = awaitFuture(client.read(channel));
    assertThat(pdu.getBody().getMessageAsType(String.class), equalTo(message));
    client.stop();
  }

  @Test
  public void casResponseError() throws ExecutionException {
    final String position = null;
    final String message = UUID.randomUUID().toString();
    final RtmClient client = clientBuilder().build();
    client.start();
    Pdu<WriteReply> replyPdu = awaitFuture(client.write(new WriteRequest<String>(channel, message, position,
        reqReadMode), Ack.YES));
    final String newPosition = replyPdu.getBody().getPosition().replace(":0", ":1");
    try {
      replyPdu = awaitFuture(client
          .write(new WriteRequest<String>(channel, message, newPosition, reqReadMode),
              Ack.YES));
    } catch (ExecutionException ex) {
      assertTrue(ex.getMessage().contains(PduException.class.getSimpleName()));
    }
    final WriteReply writeReply = replyPdu.getBody();
    if (ReqReadMode.PREVIOUS_VALUE_ON_ERROR.equals(reqReadMode) || ReqReadMode.PREVIOUS_VALUE.equals(
        reqReadMode)) {
      assertNotNull(writeReply.getPrevious());
      assertNotNull(writeReply.getPrevious().getMessage());
      assertTrue(writeReply.getPrevious().getMessage().toString().equals(message));
    } else {
      assertNull(writeReply.getPrevious());
    }
    Pdu<ReadReply> pdu = awaitFuture(client.read(channel));
    assertThat(pdu.getBody().getMessageAsType(String.class), equalTo(message));
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
      Pdu pdu = future.get();
      assertTrue(pdu.isErrorOutcome() && pdu.isWriteError());
      assertTrue(pdu.toString().contains("invalid_format"));
    } catch (ExecutionException ex) {
      assertThat(ex.getCause(), instanceOf(PduException.class));
      assertThat(((PduException) ex.getCause()).getReply().getError(), equalTo("invalid_format"));
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

  @Test
  public void deleteResponseOK() throws ExecutionException {
    final String position = null;
    final String message1 = UUID.randomUUID().toString();
    final String message2 = UUID.randomUUID().toString();
    final RtmClient client = clientBuilder().build();
    client.start();
    awaitFuture(client.write(new WriteRequest<String>(channel, message1, position), Ack.YES));
    awaitFuture(client.write(new WriteRequest<String>(channel, message2, position), Ack.YES));
    final Pdu<DeleteReply> deleteReplyPdu = awaitFuture(client.delete(new DeleteRequest(channel,
        reqReadMode), Ack.YES));
    final DeleteReply deleteReply = deleteReplyPdu.getBody();
    if (ReqReadMode.PREVIOUS_VALUE_ON_OK.equals(reqReadMode) || ReqReadMode.PREVIOUS_VALUE.equals(
        reqReadMode)) {
      assertNotNull(deleteReply.getPrevious());
      assertNotNull(deleteReply.getPrevious().getMessage());
      assertTrue(deleteReply.getPrevious().getMessage().toString().equals(message2));
    } else {
      assertNull(deleteReply.getPrevious());
    }
    final Pdu<ReadReply> pdu = awaitFuture(client.read(channel));
    assertThat(pdu.getBody().getMessageAsType(String.class), equalTo(null));
    client.stop();
  }


}
