package com.satori.rtm;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import com.satori.rtm.model.SubscriptionError;
import com.satori.rtm.model.SubscriptionInfo;
import com.satori.rtm.model.UnsubscribeReply;
import com.satori.rtm.model.UnsubscribeRequest;
import org.junit.After;
import org.junit.Before;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractRealTest {
  private static final String DEFAULT_RTM_CONFIG_PATH = "../credentials.json";
  final BlockingQueue<String> dispatcher = new LinkedBlockingQueue<String>();
  Config config = null;
  String channel = null;
  private final List<RtmClient> allClients = new ArrayList<RtmClient>();

  @Before
  public void loadConfigurationFile() throws IOException {
    String path = System.getProperty("RTM_CONFIG", DEFAULT_RTM_CONFIG_PATH);
    if (Strings.isNullOrEmpty(path)) {
      path = DEFAULT_RTM_CONFIG_PATH;
    }
    config = Config.loadFromPath(path);
  }

  @Before
  public void setUp() {
    dispatcher.clear();
    channel = generateChannel("ch");
  }

  String generateChannel(String name) {
    return name + "-" + UUID.randomUUID().toString().substring(0, 5);
  }

  void addEvent(String text) {
    dispatcher.add(text);
  }

  String getEvent() throws InterruptedException {
    return dispatcher.poll(15, TimeUnit.SECONDS);
  }

  <T> T awaitFuture(ListenableFuture<T> future) throws ExecutionException {
    try {
      return future.get(15, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  @After
  public void shutdownClients() {
    for (RtmClient client : allClients) {
      client.stop();
      client.shutdown();
    }
  }

  protected RtmClientBuilder clientBuilder() {
    return new RtmClientBuilder(config.endpoint, config.appKey) {
      @Override
      public RtmClient build() {
        RtmClient client = super.build();
        allClients.add(client);
        return client;
      }
    };
  }

  <T> void logFuture(final ListenableFuture<T> future) {
    logFuture(future, "future");
  }

  <T> void logFuture(final ListenableFuture<T> future, final String prefix) {
    Futures.addCallback(future, new FutureCallback<T>() {
      @Override
      public void onSuccess(T result) {
        dispatcher.add(prefix + "-success");
      }

      @Override
      public void onFailure(Throwable t) {
        dispatcher.add(prefix + "-failure-" + t.getMessage());
      }
    });
  }

  SubscriptionListener logSubscriptionListener(SubscriptionListenerType... modes) {
    final Set<SubscriptionListenerType> detectModes = Sets.newHashSet(modes);
    return new SubscriptionListener() {
      @Override
      public void onEnterUnsubscribed(UnsubscribeRequest request, UnsubscribeReply reply) {
        if (detectModes.contains(SubscriptionListenerType.UNSUBSCRIBED)) {
          dispatcher.add("on-enter-unsubscribed");
        }
      }

      @Override
      public void onLeaveUnsubscribed(UnsubscribeRequest request, UnsubscribeReply reply) {
        if (detectModes.contains(SubscriptionListenerType.UNSUBSCRIBED)) {
          dispatcher.add("on-leave-unsubscribed");
        }
      }

      @Override
      public void onEnterSubscribing(SubscribeRequest request) {
        if (detectModes.contains(SubscriptionListenerType.SUBSCRIBING)) {
          dispatcher.add("on-enter-subscribing");
        }
      }

      @Override
      public void onLeaveSubscribing(SubscribeRequest request) {
        if (detectModes.contains(SubscriptionListenerType.SUBSCRIBING)) {
          dispatcher.add("on-leave-subscribing");
        }
      }

      @Override
      public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
        if (detectModes.contains(SubscriptionListenerType.SUBSCRIBED)) {
          dispatcher.add("on-enter-subscribed");
        }
      }

      @Override
      public void onLeaveSubscribed(SubscribeRequest request, SubscribeReply reply) {
        if (detectModes.contains(SubscriptionListenerType.SUBSCRIBED)) {
          dispatcher.add("on-leave-subscribed");
        }
      }

      @Override
      public void onEnterUnsubscribing(UnsubscribeRequest request) {
        if (detectModes.contains(SubscriptionListenerType.UNSUBSCRIBING)) {
          dispatcher.add("on-enter-unsubscribing");
        }
      }

      @Override
      public void onLeaveUnsubscribing(UnsubscribeRequest request) {
        if (detectModes.contains(SubscriptionListenerType.UNSUBSCRIBING)) {
          dispatcher.add("on-leave-unsubscribing");
        }
      }

      @Override
      public void onEnterFailed() {
        if (detectModes.contains(SubscriptionListenerType.FAILED)) {
          dispatcher.add("on-enter-failed");
        }
      }

      @Override
      public void onLeaveFailed() {
        if (detectModes.contains(SubscriptionListenerType.FAILED)) {
          dispatcher.add("on-leave-failed");
        }
      }

      @Override
      public void onCreated() {
        if (detectModes.contains(SubscriptionListenerType.CREATED)) {
          dispatcher.add("on-created");
        }
      }

      @Override
      public void onDeleted() {
        if (detectModes.contains(SubscriptionListenerType.DELETED)) {
          dispatcher.add("on-deleted");
        }
      }

      @Override
      public void onSubscriptionData(SubscriptionData data) {
        if (detectModes.contains(SubscriptionListenerType.SUBSCRIPTION_DATA)) {
          for (String message : data.getMessagesAsStrings()) {
            dispatcher.add(message);
          }
        }
      }

      @Override
      public void onSubscriptionError(SubscriptionError error) {
        if (detectModes.contains(SubscriptionListenerType.SUBSCRIPTION_ERROR)) {
          dispatcher.add(error.getError());
        }
      }

      @Override
      public void onSubscriptionInfo(SubscriptionInfo info) {
        if (detectModes.contains(SubscriptionListenerType.SUBSCRIPTION_INFO)) {
          dispatcher.add(info.getInfo());
        }
      }
    };
  }

  RtmClientListener logClientListener(ClientListenerType... modes) {
    final Set<ClientListenerType> detectModes = Sets.newHashSet(modes);
    return new RtmClientListener() {
      @Override
      public void onTransportError(RtmClient client, Exception ex) {
        if (detectModes.contains(ClientListenerType.TRANSPORT_ERROR)) {
          String event = "on-transport-error-" + ex.getMessage();
          dispatcher.add(event);
        }
      }

      @Override
      public void onConnectingError(RtmClient client, Exception ex) {
        if (detectModes.contains(ClientListenerType.CONNECTING_ERROR)) {
          String event = "on-connecting-error-" + ex.getMessage();
          dispatcher.add(event);
        }
      }

      @Override
      public void onError(RtmClient client, Exception ex) {
        if (detectModes.contains(ClientListenerType.ERROR)) {
          String event = "on-error-" + ex.getMessage();
          dispatcher.add(event);
        }
      }

      @Override
      public void onEnterStopped(RtmClient client) {
        if (detectModes.contains(ClientListenerType.STOPPED)) {
          dispatcher.add("on-enter-stopped");
        }
      }

      @Override
      public void onLeaveStopped(RtmClient client) {
        if (detectModes.contains(ClientListenerType.STOPPED)) {
          dispatcher.add("on-leave-stopped");
        }
      }

      @Override
      public void onEnterConnecting(RtmClient client) {
        if (detectModes.contains(ClientListenerType.CONNECTING)) {
          dispatcher.add("on-enter-connecting");
        }
      }

      @Override
      public void onLeaveConnecting(RtmClient client) {
        if (detectModes.contains(ClientListenerType.CONNECTING)) {
          dispatcher.add("on-leave-connecting");
        }
      }

      @Override
      public void onEnterConnected(RtmClient client) {
        if (detectModes.contains(ClientListenerType.CONNECTED)) {
          dispatcher.add("on-enter-connected");
        }
      }

      @Override
      public void onLeaveConnected(RtmClient client) {
        if (detectModes.contains(ClientListenerType.CONNECTED)) {
          dispatcher.add("on-leave-connected");
        }
      }

      @Override
      public void onEnterAwaiting(RtmClient client) {
        if (detectModes.contains(ClientListenerType.AWAITING)) {
          dispatcher.add("on-enter-awaiting");
        }
      }

      @Override
      public void onLeaveAwaiting(RtmClient client) {
        if (detectModes.contains(ClientListenerType.AWAITING)) {
          dispatcher.add("on-leave-awaiting");
        }
      }
    };
  }

  enum ClientListenerType {
    TRANSPORT_ERROR,
    CONNECTING_ERROR,
    ERROR,
    STOPPED,
    CONNECTING,
    CONNECTED,
    AWAITING
  }

  enum SubscriptionListenerType {
    UNSUBSCRIBED,
    UNSUBSCRIBING,
    SUBSCRIBED,
    SUBSCRIBING,
    FAILED,
    CREATED,
    DELETED,
    SUBSCRIPTION_DATA,
    SUBSCRIPTION_INFO,
    SUBSCRIPTION_ERROR
  }

  static class Config {
    String endpoint;

    @SerializedName("appkey")
    String appKey;

    @SerializedName("auth_role_name")
    String roleName;

    @SerializedName("auth_role_secret_key")
    String roleSecretKey;

    @SerializedName("auth_restricted_channel")
    String restrictedChannelName;

    static Config loadFromPath(String path) throws IOException {
      Reader reader = null;
      try {
        File file = new File(path);
        reader = new InputStreamReader(new FileInputStream(file.getCanonicalPath()), "UTF-8");
        Config config = new Gson().fromJson(reader, Config.class);

        if (Strings.isNullOrEmpty(config.endpoint)) {
          throw new IllegalArgumentException("'endpoint' is not specified: " + path);
        }

        if (Strings.isNullOrEmpty(config.appKey)) {
          throw new IllegalArgumentException("'appkey' is not specified: " + path);
        }

        if (Strings.isNullOrEmpty(config.roleName)) {
          throw new IllegalArgumentException("'auth_role_name' is not specified: " + path);
        }

        if (Strings.isNullOrEmpty(config.roleSecretKey)) {
          throw new IllegalArgumentException("'auth_role_secret_key' is not specified: " + path);
        }

        if (Strings.isNullOrEmpty(config.restrictedChannelName)) {
          throw new IllegalArgumentException("'auth_restricted_channel' is not specified: " + path);
        }

        return config;
      } catch (FileNotFoundException ex) {
        throw new IOException(
            "Can't find the credentials.json file for the test environment.\n" +
                "See: https://github.com/satori-com/satori-rtm-sdk-java/tree/master#running-tests",
            ex);
      } finally {
        if (null != reader) {
          reader.close();
        }
      }
    }
  }
}

