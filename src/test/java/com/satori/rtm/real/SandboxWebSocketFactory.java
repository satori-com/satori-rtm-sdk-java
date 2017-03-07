package com.satori.rtm.real;

import com.satori.rtm.connection.GsonSerializer;
import com.satori.rtm.connection.Serializer;
import com.satori.rtm.model.AnyJson;
import com.satori.rtm.model.CommonError;
import com.satori.rtm.model.InvalidJsonException;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduRaw;
import com.satori.rtm.model.PublishReply;
import com.satori.rtm.model.PublishRequest;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import com.satori.rtm.model.SubscriptionError;
import com.satori.rtm.model.SubscriptionInfo;
import com.satori.rtm.model.UnsubscribeRequest;
import com.satori.rtm.transport.Transport;
import com.satori.rtm.transport.TransportException;
import com.satori.rtm.transport.TransportFactory;
import com.satori.rtm.transport.WebSocketTransport;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;

class SandboxWebSocketFactory implements TransportFactory {
  private final Serializer mSerializer;
  private WebSocketFactory mWebSocketFactory;

  SandboxWebSocketFactory() {
    mSerializer = new GsonSerializer();
    mWebSocketFactory = new WebSocketFactory();
  }

  @Override
  public Transport create(URI uri) throws IOException {
    WebSocket webSocket = mWebSocketFactory.createSocket(uri);
    return new WebSocketTransport(webSocket, false) {
      @Override
      public void send(String rawData) throws TransportException, InterruptedException {
        PduRaw raw = parsePdu(rawData);

        if (null == raw) {
          return;
        }

        if ("rtm/unsubscribe".equals(raw.getAction())) {
          String id = raw.convertBodyTo(UnsubscribeRequest.class).getBody().getSubscriptionId();
          if (id.startsWith("unsubscribe_error")) {
            SubscriptionError subscriptionError =
                new SubscriptionError(id, "not_subscribed", "No such subscription");
            String response = mSerializer
                .toJson(new Pdu<SubscriptionError>("rtm/unsubscribe/error", subscriptionError,
                    raw.getId()));
            mTransportListener.onMessage(response);
            return;
          }

          if (id.startsWith("unsubscribe_nack")) {
            return;
          }
        }

        if ("rtm/subscribe".equals(raw.getAction())) {
          SubscribeRequest body = raw.convertBodyTo(SubscribeRequest.class).getBody();
          String channel = body.getChannel();
          if (null == channel) {
            channel = body.getSubscriptionId();
          }
          if (channel.startsWith("subscribe_error")) {
            SubscriptionError
                subscriptionError = new SubscriptionError(channel, "already_subscribed",
                "Already subscribed to this channel");
            String response = mSerializer
                .toJson(new Pdu<SubscriptionError>("rtm/subscribe/error", subscriptionError,
                    raw.getId()));
            mTransportListener.onMessage(response);
            return;
          }

          if (channel.startsWith("subscribe_nack")) {
            return;
          }
        }

        if ("auth/handshake".equals(raw.getAction())) {
          if (rawData.contains("handshake_error")) {
            CommonError commonError = new CommonError("handshake_failed", "Handshake failed");
            String response = mSerializer
                .toJson(new Pdu<CommonError>("auth/handshake/error", commonError, raw.getId()));
            mTransportListener.onMessage(response);
            return;
          }
        }

        if (rawData.contains("authentication_required")) {
          String channel = raw.convertBodyTo(PublishRequest.class).getBody().getChannel();
          SubscriptionError subscriptionError =
              new SubscriptionError(channel, "authentication_required", "Authentication Required!");
          String response = mSerializer
              .toJson(
                  new Pdu<SubscriptionError>("rtm/publish/error", subscriptionError, raw.getId()));
          mTransportListener.onMessage(response);
          return;
        }

        if (rawData.contains("out_of_sync")) {
          String channel = raw.convertBodyTo(PublishRequest.class).getBody().getChannel();
          SubscriptionError
              subscriptionError = new SubscriptionError(channel, "out_of_sync", "Too much traffic");

          Pdu<SubscriptionError> pdu = new Pdu<SubscriptionError>("rtm/subscription/error",
              subscriptionError);
          String response = mSerializer.toJson(pdu);
          mTransportListener.onMessage(response);
          return;
        }

        if (rawData.contains("create_alien")) {
          SubscriptionData subscriptionData =
              new SubscriptionData("channel", "next", Collections.<AnyJson>emptyList());
          Pdu<SubscriptionData> pdu = new Pdu<SubscriptionData>("rtm/subscription/data",
              subscriptionData, "rid");
          mTransportListener.onMessage(mSerializer.toJson(pdu));
          return;
        }

        if (rawData.contains("channel_info")) {
          String channel = raw.convertBodyTo(PublishRequest.class).getBody().getChannel();
          SubscriptionInfo subscriptionInfo =
              new SubscriptionInfo(channel, "fast_forward", "Too much traffic", "next");
          String response =
              mSerializer
                  .toJson(new Pdu<SubscriptionInfo>("rtm/subscription/info", subscriptionInfo));
          mTransportListener.onMessage(response);
          return;
        }

        if (rawData.contains("publish_reply_without_body")) {
          String response = mSerializer
              .toJson(new Pdu<PublishReply>("rtm/publish/ok", null, raw.getId()));
          mTransportListener.onMessage(response);
          return;
        }

        if (rawData.contains("force_disconnect")) {
          super.close();
          return;
        }

        if (rawData.contains("packet_without_response")) {
          return;
        }

        if (rawData.contains("bad_json")) {
          mTransportListener.onMessage("}{ bad_json_from_server }{");
          return;
        }

        if (rawData.contains("system_wide_error")) {
          CommonError error = new CommonError("system_wide_error", "system_wide_desc");
          String response = mSerializer.toJson(new Pdu<CommonError>("/error", error));
          mTransportListener.onMessage(response);
          if (rawData.contains("system_wide_error_with_server_disconnect")) {
            super.close();
          }
          return;
        }

        if (rawData.contains("unknown_outcome")) {
          CommonError error = new CommonError("unknown_outcome", "Unknown outcome");
          String response =
              mSerializer.toJson(new Pdu<CommonError>("rtm/publish/unknown", error, raw.getId()));
          mTransportListener.onMessage(response);
          return;
        }

        super.send(rawData);
      }

      PduRaw parsePdu(String json) {
        try {
          return mSerializer.parsePdu(json);
        } catch (InvalidJsonException e) {
          return null;
        }
      }
    };
  }
}
