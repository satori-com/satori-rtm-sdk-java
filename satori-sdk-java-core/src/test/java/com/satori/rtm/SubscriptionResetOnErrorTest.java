package com.satori.rtm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SubscriptionResetOnErrorTest {
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

    SubscriptionConfig config = new SubscriptionConfig(SubscriptionMode.ADVANCED, mListener);
    mFSM = new ChannelSubscription("channel", config, mService);
  }

  @Test
  public void getSubscriptionErrorWhenSubscribedAndGotErrorWhenSubscribing()
      throws InvalidJsonException {
        /*
         * We are connected to the server
         * Successfully subscribed to the channel
         * Got channel error while subscribed
         * Got subscribe error
         * Check that we are in failed state
        */

    mService.onConnected(mConnection);

    ArgumentCaptor<SubscribeRequest> argument = ArgumentCaptor.forClass(SubscribeRequest.class);
    SettableFuture<Pdu<SubscribeReply>> reply = SettableFuture.create();
    reply.set(
        new Pdu<SubscribeReply>("rtm/subscribe/ok", new SubscribeReply("channel", "new_next")));
    when(mConnection
        .send(eq("rtm/subscribe"), argument.capture(), eq(SubscribeReply.class)))
        .thenReturn(reply);

    mFSM.enterStartState();
    assertThat(mFSM.getState(), is(ChannelSubscription.SUBSCRIBED));

    reply = SettableFuture.create();

    PduRaw reason = new PduRaw("rtm/subscribe/error", new AnyJson() {
      @Override
      @SuppressWarnings("unchecked")
      public <F> F convertToType(Class<F> clazz) {
        return (F) new SubscriptionError("channel", "error_code", "error_message");
      }
    });

    reply.setException(new PduException("subscribe_error", reason));
    when(mConnection
        .send(eq("rtm/subscribe"), argument.capture(), eq(SubscribeReply.class)))
        .thenReturn(reply);

    SubscriptionError error = new SubscriptionError();
    mFSM.onSubscriptionError(new Pdu<SubscriptionError>("rtm/subscription/error", error));

    assertThat(mFSM.getState(), is(ChannelSubscription.FAILED));
    assertThat(argument.getValue().getPosition(), nullValue());
    assertThat(argument.getValue().getHistory(), nullValue());
    assertThat(mFSM.getMode(), is(ChannelSubscription.Mode.LINKED));
    verify(mListener).onSubscriptionError(error);
    verify(mService.getPubSub(), never()).deleteSubscriptionFromRegistry("channel");
  }
}
