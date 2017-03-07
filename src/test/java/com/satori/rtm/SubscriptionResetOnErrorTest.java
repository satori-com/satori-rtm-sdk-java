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
import com.google.gson.JsonObject;
import com.satori.rtm.connection.Connection;
import com.satori.rtm.connection.GsonSerializer;
import com.satori.rtm.model.InvalidJsonException;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduException;
import com.satori.rtm.model.PduRaw;
import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionError;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SubscriptionResetOnErrorTest {
  private SubscriptionConfig mConfig;
  private SubscriptionListener mListener;
  private RtmService mService;
  private ChannelSubscription mFSM;
  private GsonSerializer mSerializer;
  private Connection mConnection;

  @Before
  public void setUp() {
    mService = RtmService.create(0, null);
    mService.setPubSub(spy(mService.getPubSub()));

    mConnection = mock(Connection.class);
    mListener = mock(SubscriptionListener.class);

    mConfig = new SubscriptionConfig(SubscriptionMode.ADVANCED, mListener);
    mSerializer = new GsonSerializer();
    mFSM = new ChannelSubscription("channel", mConfig, mService);
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
    MatcherAssert.assertThat(mFSM.getState(), is(ChannelSubscription.SUBSCRIBED));

    reply = SettableFuture.create();
    JsonObject errorBody = new JsonObject();
    errorBody.addProperty("channel", "channel");
    errorBody.addProperty("error", "error_code");
    errorBody.addProperty("error_text", "error_message");
    PduRaw reason =
        mSerializer.parsePdu("{'action':'rtm/subscribe/error','body':{'channel':'channel'}}");
    reply.setException(new PduException("subscribe_error", reason));
    when(mConnection
        .send(eq("rtm/subscribe"), argument.capture(), eq(SubscribeReply.class)))
        .thenReturn(reply);

    SubscriptionError error = new SubscriptionError();
    mFSM.onSubscriptionError(new Pdu<SubscriptionError>("rtm/subscription/error", error));

    MatcherAssert.assertThat(mFSM.getState(), is(ChannelSubscription.FAILED));
    assertThat(argument.getValue().getPosition(), nullValue());
    assertThat(argument.getValue().getHistory(), nullValue());
    assertThat(mFSM.getMode(), is(ChannelSubscription.Mode.LINKED));
    verify(mListener).onSubscriptionError(error);
    verify(mService.getPubSub(), never()).deleteSubscriptionFromRegistry("channel");
  }
}
