package com.satori.rtm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.SettableFuture;
import com.satori.rtm.connection.Connection;
import com.satori.rtm.model.AnyJson;
import com.satori.rtm.model.InvalidJsonException;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduException;
import com.satori.rtm.model.PduRaw;
import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionError;
import com.satori.rtm.model.UnsubscribeReply;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

@RunWith(JUnit4.class)
public class SubscriptionFromPositionTest {
  private SubscriptionListener mListener;
  private RtmService mService;
  private ChannelSubscription mFSM;
  private Connection mConnection;

  @Before
  public void setUp() {
    mService = RtmService.create(0, null);
    mService.setPubSub(spy(mService.getPubSub()));

    mConnection = mock(Connection.class);
    mListener = mock(SubscriptionListener.class);

    SubscriptionConfig config = new SubscriptionConfig(SubscriptionMode.RELIABLE, mListener)
        .setPosition("position");
    mFSM = new ChannelSubscription("channel", config, mService);
  }

  @Test
  public void startFSMWithoutConnection() {
    mService.onDisconnected();
    mFSM.enterStartState();
    assertThat(mFSM.getState(), is(ChannelSubscription.UNSUBSCRIBED));
  }

  @Test
  public void failedToSubscribe() throws InvalidJsonException {
        /*
         * We are not connected to the server
         * Receive onConnect
         * Failed to subscribe to the channel
         * Receive unsubscribe
         * Check that we are unsubscribed but still unlinked
         * Check that we delete subscription
        */
    mService.onDisconnected();

    mFSM.enterStartState();
    assertThat(mFSM.getState(), is(ChannelSubscription.UNSUBSCRIBED));

    ArgumentCaptor<SubscribeRequest> argument = ArgumentCaptor.forClass(SubscribeRequest.class);
    SettableFuture<Pdu<SubscribeReply>> reply = SettableFuture.create();

    when(mConnection
        .send(eq("rtm/subscribe"), argument.capture(), eq(SubscribeReply.class)))
        .thenReturn(reply);

    mService.onConnected(mConnection);
    mFSM.onConnected();

    assertThat(mFSM.getState(), is(ChannelSubscription.SUBSCRIBING));

    PduRaw reason = new PduRaw("rtm/subscribe/error", new AnyJson() {
      @Override
      @SuppressWarnings("unchecked")
      public <F> F convertToType(Class<F> clazz) {
        return (F) new SubscriptionError("channel", "error_code", "error_message");
      }
    });

    reply.setException(new PduException("error_outcome", reason));

    assertThat(mFSM.getState(), is(ChannelSubscription.FAILED));
    mFSM.unsubscribe();

    assertThat(mFSM.getState(), is(ChannelSubscription.UNSUBSCRIBED));
    assertThat(argument.getValue().getChannel(), equalTo("channel"));
    assertThat(argument.getValue().getPosition(), equalTo("position"));

    assertThat(mFSM.getMode(), is(ChannelSubscription.Mode.UNLINKED));

    verify(mService.getPubSub()).deleteSubscriptionFromRegistry("channel");
  }

  @Test
  public void resubscribeAfterDisconnectWithUpdatedNext() {
        /*
         * We are connected to the server
         * Successfully subscribe to the channel
         * Receive onDisconnect
         * Receive onConnect
         * Check that we are connecting with new position
        */

    mService.onConnected(mConnection);

    ArgumentCaptor<SubscribeRequest> argument = ArgumentCaptor.forClass(SubscribeRequest.class);
    SettableFuture<Pdu<SubscribeReply>> reply = SettableFuture.create();

    when(mConnection
        .send(eq("rtm/subscribe"), argument.capture(), eq(SubscribeReply.class)))
        .thenReturn(reply);

    mFSM.enterStartState();
    assertThat(mFSM.getState(), is(ChannelSubscription.SUBSCRIBING));
    reply.set(
        new Pdu<SubscribeReply>("rtm/subscribe/ok", new SubscribeReply("channel", "new_position")));

    assertThat(mFSM.getState(), is(ChannelSubscription.SUBSCRIBED));

    mService.onDisconnected();

    mFSM.onDisconnected();
    assertThat(mFSM.getState(), is(ChannelSubscription.UNSUBSCRIBED));

    SettableFuture<Pdu<SubscribeReply>> newReply = SettableFuture.create();

    when(mConnection
        .send(eq("rtm/subscribe"), argument.capture(), eq(SubscribeReply.class)))
        .thenReturn(newReply);

    mService.onConnected(mConnection);

    mFSM.onConnected();

    assertThat(mFSM.getState(), is(ChannelSubscription.SUBSCRIBING));
    newReply
        .set(new Pdu<SubscribeReply>("rtm/subscribe/ok", new SubscribeReply("channel", "foobar")));
    assertThat(mFSM.getState(), is(ChannelSubscription.SUBSCRIBED));
    assertThat(argument.getValue().getPosition(), equalTo("new_position"));
  }

  @Test
  public void subscribeUnsubscribe() {
        /*
         * We are connected to the server
         * Successfully subscribe to the channel
         * Successfully unsubscribe from the channel
         * Check that are unlinked and subscription is deleted
        */

    mService.onConnected(mConnection);
    SettableFuture<Pdu<SubscribeReply>> subscribeRequest = SettableFuture.create();
    when(mConnection
        .send(eq("rtm/subscribe"), any(), eq(SubscribeReply.class)))
        .thenReturn(subscribeRequest);
    mFSM.enterStartState();
    assertThat(mFSM.getState(), is(ChannelSubscription.SUBSCRIBING));
    subscribeRequest.set(
        new Pdu<SubscribeReply>("rtm/subscribe/ok", new SubscribeReply("channel", "new_position")));
    assertThat(mFSM.getState(), is(ChannelSubscription.SUBSCRIBED));

    SettableFuture<Pdu<UnsubscribeReply>> unsubscribeRequest = SettableFuture.create();

    when(mConnection
        .send(eq("rtm/unsubscribe"), any(), eq(UnsubscribeReply.class)))
        .thenReturn(unsubscribeRequest);

    mFSM.unsubscribe();
    assertThat(mFSM.getState(), is(ChannelSubscription.UNSUBSCRIBING));
    unsubscribeRequest.set(
        new Pdu<UnsubscribeReply>("rtm/unsubscribe/ok",
            new UnsubscribeReply("unsubscribe_position")));

    assertThat(mFSM.getState(), is(ChannelSubscription.UNSUBSCRIBED));

    assertThat(mFSM.getSubscriptionConfig().getPosition(), equalTo("unsubscribe_position"));
    assertThat(mFSM.getMode(), is(ChannelSubscription.Mode.UNLINKED));
    verify(mService.getPubSub()).deleteSubscriptionFromRegistry("channel");
  }

  @Test
  public void resubscribeDuringSubscribing() {
        /*
         * We are connected to the server
         * Successfully subscribe to the channel
         * Receive unsubscribe and subscribe calls during subscribing
         * Check that we unsubscribed from prev channel and subscribe to the new channel
        */
    mService.onConnected(mConnection);
    SettableFuture<Pdu<SubscribeReply>> subscribeRequest = SettableFuture.create();
    ArgumentCaptor<SubscribeRequest> argument = ArgumentCaptor.forClass(SubscribeRequest.class);

    when(mConnection
        .send(eq("rtm/subscribe"), argument.capture(), eq(SubscribeReply.class)))
        .thenReturn(subscribeRequest);

    SettableFuture<Pdu<UnsubscribeReply>> unsubscribeRequest = SettableFuture.create();

    when(mConnection
        .send(eq("rtm/unsubscribe"), any(), eq(UnsubscribeReply.class)))
        .thenReturn(unsubscribeRequest);


    mFSM.enterStartState();
    verify(mListener).onCreated();

    assertThat(mFSM.getState(), is(ChannelSubscription.SUBSCRIBING));
    mFSM.unsubscribe();

    SubscriptionAdapter newUserListener = mock(SubscriptionAdapter.class);
    mFSM.updateSubscriptionConfig(
        new SubscriptionConfig(SubscriptionMode.RELIABLE, newUserListener)
            .setPosition("position"));
    assertThat(mFSM.getMode(), is(ChannelSubscription.Mode.CYCLE));
    verify(newUserListener).onCreated();

    subscribeRequest.set(
        new Pdu<SubscribeReply>("rtm/subscribe/ok", new SubscribeReply("channel", "new_position")));
    assertThat(mFSM.getState(), is(ChannelSubscription.UNSUBSCRIBING));
    unsubscribeRequest.set(
        new Pdu<UnsubscribeReply>("rtm/unsubscribe/ok",
            new UnsubscribeReply("unsubscribe_position")));

    assertThat(mFSM.getState(), is(ChannelSubscription.SUBSCRIBED));
    assertThat(argument.getValue().getPosition(), equalTo("position"));
    assertThat(mFSM.getSubscriptionConfig().getPosition(), equalTo("new_position"));
    assertThat(mFSM.getMode(), is(ChannelSubscription.Mode.LINKED));

    verify(mListener).onDeleted();
    verify(mService.getPubSub(), never()).deleteSubscriptionFromRegistry("channel");
  }

  @Test
  public void getErrorWhenSubscribed() {
        /*
         * We are connected to the server
         * Successfully subscribed to the channel
         * Got channel error while subscribed
         * Reconnected with empty position
        */

    mService.onConnected(mConnection);

    ArgumentCaptor<SubscribeRequest> argument = ArgumentCaptor.forClass(SubscribeRequest.class);
    SettableFuture<Pdu<SubscribeReply>> reply = SettableFuture.create();
    reply.set(
        new Pdu<SubscribeReply>("rtm/subscribe/ok", new SubscribeReply("channel", "new_position")));

    when(mConnection
        .send(eq("rtm/subscribe"), argument.capture(), eq(SubscribeReply.class)))
        .thenReturn(reply);

    mFSM.enterStartState();
    assertThat(mFSM.getState(), is(ChannelSubscription.SUBSCRIBED));

    SubscriptionError error = new SubscriptionError();
    mFSM.onSubscriptionError(new Pdu<SubscriptionError>("rtm/subscription/error", error));

    assertThat(mFSM.getState(), is(ChannelSubscription.FAILED));

    mService.onDisconnected();
    mFSM.onDisconnected();

    assertThat(mFSM.getState(), is(ChannelSubscription.UNSUBSCRIBED));
    assertThat(mFSM.getMode(), is(ChannelSubscription.Mode.LINKED));
    verify(mListener).onSubscriptionError(error);
    verify(mService.getPubSub(), never()).deleteSubscriptionFromRegistry("channel");
  }

  @Test
  public void closeConnectionWhenGetUnsubscribeError() throws InvalidJsonException {
        /*
         * We are connected to the server
         * Successfully subscribed to the channel
         * Got error while unsubscribing from the channel
         * Close/open connection
        */
    mService.onConnected(mConnection);

    SettableFuture<Pdu<SubscribeReply>> reply = SettableFuture.create();
    reply.set(
        new Pdu<SubscribeReply>("rtm/subscribe/ok", new SubscribeReply("channel", "new_position")));

    when(mConnection
        .send(eq("rtm/subscribe"), any(), eq(SubscribeReply.class)))
        .thenReturn(reply);

    SettableFuture<Pdu<UnsubscribeReply>> unsubscribeRequest = SettableFuture.create();
    when(mConnection
        .send(eq("rtm/unsubscribe"), any(), eq(UnsubscribeReply.class)))
        .thenReturn(unsubscribeRequest);


    PduRaw reason = new PduRaw("rtm/unsubscribe/error", new AnyJson() {
      @Override
      @SuppressWarnings("unchecked")
      public <F> F convertToType(Class<F> clazz) {
        return (F) new SubscriptionError("channel", "error_code", "error_message");
      }
    });

    unsubscribeRequest.setException(new PduException("unsubscribe_error", reason));

    mFSM.enterStartState();
    mFSM.unsubscribe();
    verify(mConnection).close();

    ArgumentCaptor<SubscriptionError> argument = ArgumentCaptor.forClass(SubscriptionError.class);
    verify(mListener).onSubscriptionError(argument.capture());

    assertThat(argument.getValue().getSubscriptionId(), equalTo("channel"));
    assertThat(argument.getValue().getError(), equalTo("error_code"));
    assertThat(argument.getValue().getReason(), equalTo("error_message"));

    mFSM.onDisconnected();
    assertThat(mFSM.getState(), is(ChannelSubscription.UNSUBSCRIBED));
    assertThat(mFSM.getMode(), is(ChannelSubscription.Mode.UNLINKED));
    verify(mService.getPubSub()).deleteSubscriptionFromRegistry("channel");
  }
}
