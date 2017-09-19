package com.satori.rtm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.satori.rtm.auth.AuthException;
import com.satori.rtm.auth.RoleSecretAuthProvider;
import com.satori.rtm.model.CommonError;
import com.satori.rtm.model.PduException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.concurrent.ExecutionException;

@RunWith(JUnit4.class)
public class AuthTest extends AbstractRealTest {
  @Test
  public void successfulAuthentication() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setAuthProvider(new RoleSecretAuthProvider(config.roleName, config.roleSecretKey))
        .setListener(createAuthConnectionListener())
        .build();
    client.start();

    assertThat(getEvent(), equalTo("on-enter-connected"));

    client.stop();

    assertThat(getEvent(), equalTo("on-leave-connected"));
  }

  @Test
  public void unsuccessfulAuthentication() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setAuthProvider(new RoleSecretAuthProvider(config.roleName, "bad_key"))
        .setListener(createAuthConnectionListener())
        .build();

    client.start();

    assertThat(getEvent(), equalTo("authentication_failed"));
    assertThat(getEvent(), equalTo("on-enter-awaiting"));

    client.stop();
  }

  @Test
  public void tryingToSubscribeToChannelWhenAuthorized() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setAuthProvider(new RoleSecretAuthProvider(config.roleName, config.roleSecretKey))
        .build();

    client.start();
    client.createSubscription(config.restrictedChannelName, SubscriptionMode.SIMPLE,
        logSubscriptionListener(
            SubscriptionListenerType.SUBSCRIBING,
            SubscriptionListenerType.SUBSCRIBED,
            SubscriptionListenerType.FAILED
        ));

    assertThat(getEvent(), equalTo("on-enter-subscribing"));
    assertThat(getEvent(), equalTo("on-leave-subscribing"));
    assertThat(getEvent(), equalTo("on-enter-subscribed"));
    assertThat(dispatcher.poll(), nullValue());

    client.stop();

    assertThat(getEvent(), equalTo("on-leave-subscribed"));
  }

  @Test
  public void tryingToPublishToRestrictedChannelWhenNotAuthorized() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .build();

    client.start();
    try {
      awaitFuture(client.publish("channel", "authentication_required", Ack.YES));
    } catch (ExecutionException executionException) {
      Throwable cause = executionException.getCause();
      if (cause instanceof PduException) {
        PduException ex = (PduException) cause;
        CommonError error = ex.getReply();
        dispatcher.add(error.getError());
      }
    }

    assertThat(getEvent(), equalTo("authentication_required"));
    assertThat(dispatcher.poll(), nullValue());

    client.stop();
  }

  @Test
  public void roleBasedAuthenticationWithHandshakeError() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setTransportFactory(new SandboxWebSocketFactory())
        .setAuthProvider(new RoleSecretAuthProvider("handshake_error", "anything"))
        .setListener(createAuthConnectionListener())
        .build();
    client.start();

    assertThat(getEvent(), equalTo("handshake_failed"));
    assertThat(getEvent(), equalTo("on-enter-awaiting"));

    client.stop();
  }

  @Test
  public void roleBasedAuthenticationWithBadRole() throws InterruptedException {
    RtmClient client = clientBuilder()
        .setAuthProvider(new RoleSecretAuthProvider("foo", "bad_key"))
        .setListener(createAuthConnectionListener())
        .build();

    client.start();
    assertThat(getEvent(), equalTo("authentication_failed"));
    assertThat(getEvent(), equalTo("on-enter-awaiting"));
    client.stop();
  }

  private RtmClientAdapter createAuthConnectionListener() {
    return new RtmClientAdapter() {
      @Override
      public void onEnterConnected(RtmClient client) {
        dispatcher.add("on-enter-connected");
      }

      @Override
      public void onLeaveConnected(RtmClient client) {
        dispatcher.add("on-leave-connected");
      }

      @Override
      public void onEnterAwaiting(RtmClient client) {
        dispatcher.add("on-enter-awaiting");
      }

      @Override
      public void onLeaveAwaiting(RtmClient client) {
        dispatcher.add("on-leave-awaiting");
      }

      @Override
      public void onError(RtmClient client, Exception exception) {
        try {
          throw exception;
        } catch (AuthException ex) {
          PduException errorOutcomeException = (PduException) ex.getCause();
          CommonError error = errorOutcomeException.getReply();
          dispatcher.add(error.getError());
        } catch (Throwable t) {
          // doesn't match
        }
      }
    };
  }
}
