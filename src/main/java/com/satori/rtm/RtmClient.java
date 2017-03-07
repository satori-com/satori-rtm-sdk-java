package com.satori.rtm;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.connection.Connection;
import com.satori.rtm.model.DeleteReply;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduException;
import com.satori.rtm.model.PublishReply;
import com.satori.rtm.model.PublishRequest;
import com.satori.rtm.model.ReadReply;
import com.satori.rtm.model.ReadRequest;
import com.satori.rtm.model.SearchReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.UnsubscribeReply;
import com.satori.rtm.model.UnsubscribeRequest;
import com.satori.rtm.model.WriteReply;
import com.satori.rtm.model.WriteRequest;
import com.satori.rtm.model.DeleteRequest;
import com.satori.rtm.transport.TransportException;
import java.util.EnumSet;

/**
 * The RtmClient interface is the main entry point for accessing the RTM Service, including publish and subscribe
 * operations.
 * <p>
 * Create an instance of the client with {@link RtmClientBuilder#RtmClientBuilder} and use the RtmClient methods to start, stop,
 * and restart the client WebSocket connection, and publish and subscribe to channels.
 * <pre>
 * {@code
 * RtmClient client = new RtmClientBuilder(ENDPOINT, APP_KEY)
 *           .setListener(new RtmClientAdapter() {
 *                // overridden methods go here
 *            )}
 *           .build();
 *            //  other logic
 *
 * client.start();
 * client.createSubscription("test-channel", SubscriptionMode.SIMPLE,
 *     new SubscriptionAdapter() {
 *      // overridden methods go here
 *     });
 * client.publish("test-channel", "Hello, world");
 * }
 * </pre>
 */
public interface RtmClient {
  /**
   * Starts the client. Client tries to establish the WebSocket connection to the RTM Service asynchronously.
   * <p>
   * If you enable auto-reconnect mode, the SDK attempts to reconnect to the RTM Service if the
   * WebSocket connection fails for any reason.
   * <p>
   * If you make any publish or subscribe requests while the WebSocket connection is not active, the SDK queues the
   * requests and completes them when the connection is established or re-established.
   * <p>
   * You can use the {@link RtmClientListener} interface to define application functionality for when the application
   * enters or leaves the connecting state.
   *
   * @see RtmClientListener#onEnterConnecting(RtmClient)
   */
  void start();

  /**
   * Stops the client. The SDK tries to close the WebSocket connection asynchronously and does not start it again
   * unless you call {@link RtmClient#start()}.
   * <p>
   * Use this method to explicitly stop all interaction with the RTM Service.
   * <p>
   * You can use the {@link RtmClientListener} interface to define application functionality when the application
   * enters or leaves the {@code stopped} state.
   *
   * @see RtmClientListener#onEnterStopped(RtmClient)
   */
  void stop();

  /**
   * Restarts the client.
   *
   * @see RtmClient#start()
   * @see RtmClient#stop()
   */
  void restart();

  /**
   * Shuts down the client. Stops the client, terminates threads, and cleans up all allocated resources.
   * <p>
   * You cannot use the instance of the {@link RtmClient} when it is shut down.
   */
  void shutdown();

  /**
   * Returns {@code true} if the client is completely connected.
   * <p>
   * The client is connected when underlying WebSocket transport is established, any authentication (if necessary) is
   * completed and the client is in an active state and able to send and receive messages.
   *
   * @return {@code true} if the client is completely connected; {@code false} otherwise.
   */
  boolean isConnected();

  /**
   * Creates subscription with the specific channel.
   * <p>
   * You can create subscription at any time. The SDK manages the subscription and sends a subscribe
   * request when the WebSocket connection is established. Use the {@code subscriptionConfig} parameter to define
   * the behaviour that the SDK uses to handle dropped connections.
   * <p>
   * Subscribe and unsubscribe operations always produce either an OK response or an error response from the RTM
   * Service.
   *
   * @param channel  Name of the channel.
   * @param modes    Subscription modes.
   * @param listener Subscription listener.
   * @see SubscribeRequest
   */
  void createSubscription(String channel, EnumSet<SubscriptionMode> modes,
                          SubscriptionListener listener);

  /**
   * Creates subscription to the specified subscription id.
   * <p>
   * You can create subscription at any time. The SDK manages the subscription and sends a subscribe
   * request when the WebSocket connection is established. Use the {@code subscriptionConfig} parameter to define
   * the behaviour that the SDK uses to handle dropped connections.
   * <p>
   * Subscribe and unsubscribe operations always produce either an OK response or an error response from the RTM
   * Service.
   * <p>
   * If filter is not specified then {@code channelOrSubId} is used as a channel name.
   *
   * @param channelOrSubId     Name of the channel or subscription id.
   * @param subscriptionConfig Subscription configuration.
   * @see SubscribeRequest
   */
  void createSubscription(String channelOrSubId, SubscriptionConfig subscriptionConfig);

  /**
   * Removes the subscription with the specific subscription id.
   * <p>
   * The RTM Service continues to send messages for the channel until the unsubscribe operation completes.
   * <p>
   * You can use the {@link SubscriptionListener} interface to define application
   * functionality for when the application enters or leaves the {@code unsubscribing}, {@code unsubscribed}, or
   * {@code deleted} state.
   *
   * @param subscriptionId Name of the channel to unsubscribe from.
   * @see SubscriptionListener#onEnterUnsubscribing(UnsubscribeRequest)
   * @see SubscriptionListener#onEnterUnsubscribed(UnsubscribeRequest, UnsubscribeReply)
   * @see SubscriptionListener#onDeleted()
   */
  void removeSubscription(String subscriptionId);

  /**
   * Publishes the message to the channel.
   * <p>
   * If client is not connected to the RTM Service, the publish request is queued. The SDK sends the message when the
   * connection is established. The length of this queue is limited by
   * {@link RtmClientBuilder#setPendingActionQueueLength(int)}.
   * <p>
   * Use the ListenableFuture object, for example, with the {@link java.util.concurrent.Future#get}
   * method or use the {@link com.google.common.util.concurrent.Futures#addCallback(ListenableFuture, FutureCallback)}
   * method to set a callback and process the response from the RTM Service.
   * <p>
   * The {@code ListenableFuture} can fail due to one of the following errors during the publish operation:
   * <ul>
   * <li>{@link IllegalStateException}: The message cannot be added to the pending queue because the queue is
   * full.</li>
   * <li>{@link PduException}: The reply from the RTM Service is not a positive confirmation.</li>
   * <li>{@link TransportException}: An error occurred in the WebSocket transport
   * layer.</li>
   * </ul>
   *
   * @param channel Name of the channel to publish to.
   * @param message Serializable JSON value.
   * @param ack     Acknowledge mode.
   * @param <T>     Type of serializable message.
   * @return Asynchronous result of the publish request.
   * @see PublishRequest
   * @see PublishReply
   * @see RtmClientBuilder#setPendingActionQueueLength(int)
   */
  <T> ListenableFuture<Pdu<PublishReply>> publish(String channel, T message, Ack ack);

  /**
   * Returns the current connection or {@code null} if the client is not connected.
   *
   * @return Connection, or null if client is not connected.
   */
  Connection getConnection();

  /**
   * Reads the value of the specified key from a key-value store.
   *
   * @param key Key name.
   * @return Asynchronous result of the read request.
   * @see #read(ReadRequest)
   */
  ListenableFuture<Pdu<ReadReply>> read(String key);

  /**
   * Reads the value of the key specified in a {@link ReadRequest} instance from a key-value store.
   * <p>
   * If client is not connected to the RTM Service, the read request is queued. The SDK reads the message when the
   * connection is established. The length of this queue is limited by
   * {@link RtmClientBuilder#setPendingActionQueueLength(int)}.
   * <p>
   * The <a href="http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/ListenableFuture.html">ListenableFuture</a>
   * returned by this method is complete when the SDK receives the value from the RTM Service.
   * <p>
   * The {@code ListenableFuture} can fail due to one of the following errors during the read operation:
   * <ul>
   * <li>{@link IllegalStateException}: The request cannot be added to the pending queue because the queue is
   * full.</li>
   * <li>{@link PduException}: The reply from the RTM Service is not a positive confirmation.</li>
   * <li>{@link TransportException}: An error occurred in the WebSocket transport
   * layer.</li>
   * </ul>
   *
   * @param request The read request.
   * @return Asynchronous result of the read request.
   */
  ListenableFuture<Pdu<ReadReply>> read(ReadRequest request);

  /**
   * Writes the value for the specified key to a key-value store.
   *
   * @param key   Key name.
   * @param value Serializable JSON value.
   * @param ack   Acknowledge mode.
   * @param <T>   Type of serializable message.
   * @return Asynchronous result of the write request.
   * @see #write(WriteRequest, Ack)
   */
  <T> ListenableFuture<Pdu<WriteReply>> write(String key, T value, Ack ack);

  /**
   * Writes the value of the key in a {@link WriteRequest} instance to a key-value store.
   * <p>
   * If client is not connected to the RTM Service, the write request is queued. The SDK sends the request when the
   * connection is established. The length of this queue is limited by {@link RtmClientBuilder#setPendingActionQueueLength(int)}.
   * <p>
   * The <a href="http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/ListenableFuture.html">ListenableFuture</a>
   * returned by this method is complete when the SDK receives the acknowledgement from the RTM Service.
   * <p>
   * The {@code ListenableFuture} can fail due to one of the following errors during the write operation:
   * <ul>
   * <li>{@link IllegalStateException}: The request cannot be added to the pending queue because the queue is
   * full.</li>
   * <li>{@link PduException}: The reply from the RTM Service is not a positive confirmation.</li>
   * <li>{@link TransportException}: An error occurred in the WebSocket transport
   * layer.</li>
   * </ul>
   *
   * @param writeRequest Write request.
   * @param ack          Acknowledge mode.
   * @param <T>          Type of serializable message.
   * @return Asynchronous result of the write request.
   * @see WriteRequest
   * @see WriteReply
   */
  <T> ListenableFuture<Pdu<WriteReply>> write(WriteRequest<T> writeRequest, Ack ack);


  /**
   * Deletes the value of the specified key from a key-value store.
   * <p>
   * If client is not connected to the RTM Service, the delete request is queued. The SDK sends the request when the
   * connection is established. The length of this queue is limited by {@link RtmClientBuilder#setPendingActionQueueLength(int)}.
   * <p>
   * The <a href="http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/ListenableFuture.html">ListenableFuture</a>
   * returned by this method is complete when the SDK receives the acknowledgement from the RTM Service.
   * <p>
   * The {@code ListenableFuture} can fail due to one of the following errors during the delete operation:
   * <ul>
   * <li>{@link IllegalStateException}: The request cannot be added to the pending queue because the queue is
   * full.</li>
   * <li>{@link PduException}: The reply from the RTM Service is not a positive confirmation.</li>
   * <li>{@link TransportException}: An error occurred in the WebSocket transport
   * layer.</li>
   * </ul>
   *
   * @param key Key name.
   * @param ack Acknowledge mode.
   * @return Asynchronous result of the write request.
   * @see DeleteRequest
   * @see DeleteReply
   */
  ListenableFuture<Pdu<DeleteReply>> delete(String key, Ack ack);


  /**
   * Returns all channels with a given prefix. Server could send several responses.
   *
   * @param prefix   The channels prefix.
   * @param callback A user-supplied callback to execute when the response has been received
   *                 from the RTM service.
   */
  void search(String prefix, Callback<Pdu<SearchReply>> callback);
}
