package com.satori.rtm;

import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.connection.Connection;
import com.satori.rtm.model.DeleteReply;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduException;
import com.satori.rtm.model.PublishReply;
import com.satori.rtm.model.PublishRequest;
import com.satori.rtm.model.ReadReply;
import com.satori.rtm.model.ReadRequest;
import com.satori.rtm.model.WriteReply;
import com.satori.rtm.model.WriteRequest;
import com.satori.rtm.model.DeleteRequest;
import com.satori.rtm.transport.TransportException;
import java.util.EnumSet;

/**
 * An {@code RtmClient} is the main entry point for accessing RTM.
 * <p>
 * To create an RTM client, use {@link RtmClientBuilder}. To connect a client to RTM,
 * you must call {@link #start}.
 * <p>
 * By default, the RTM SDK attempts to reconnect to RTM if the connection to RTM
 * fails for any reason. To disable this behavior, call
 * {@link RtmClientBuilder#setAutoReconnect(boolean) RtmClientBuilder.setAutoReconnect()}.
 * <p>
 * Operations performed when client is offline are queued and performed
 * when the client reconnects to RTM. To disable this behaviour or change the length of this queue, call
 * {@link RtmClientBuilder#setPendingActionQueueLength(int) RtmClientBuilder.setPendingActionQueueLength()}.
 * <p>
 * {@code RtmClient} instances are thread-safe so you can reuse them freely across multiple threads.
 * <p>
 * This is an example of to use an RTM client to subscribe to a channel:
 * <pre>
 * RtmClient client = new RtmClientBuilder(YOUR_ENDPOINT, YOUR_APPKEY)
 *   // Sets a listener for RTM lifecycle events
 *   .setListener(new RtmClientAdapter() {
 *      // When the client successfully connects to RTM
 *     &#64;Override
 *     public void onEnterConnected(RtmClient client) {
 *       System.out.println("Connected to Satori RTM!");
 *     }
 *   })
 *   // Builds the client instance
 *   .build();
 * // Subscribe to "my_channel" and listen for incoming messages
 * client.createSubscription("my_channel", SubscriptionMode.SIMPLE, new SubscriptionAdapter() {
 *   // Displays incoming messages
 *   &#64;Override
 *   public void onSubscriptionData(SubscriptionData data) {
 *     for (AnyJson json: data.getMessages()) {
 *       System.out.println("Got message: " + json);
 *     }
 *   }
 * });
 * // Connects the client to RTM
 * client.start();
 * </pre>
 */
public interface RtmClient {
  /**
   * Starts the client, which then tries to connect to RTM asynchronously.
   * <p>
   * By default, the RTM SDK attempts to reconnect to RTM if the connection to RTM
   * fails for any reason.
   * <p>
   * To provide callbacks for events in the client or the connection lifecycle,
   * call {@link RtmClientBuilder#setListener(RtmClientListener) RtmClientBuilder.setListener()}.
   * For example,
   * {@link RtmClientListener#onEnterConnected(RtmClient) RtmClientListener.onEnterConnected()} is
   * called when the RTM client starts
   * connecting to
   * RTM.
   */
  void start();

  /**
   * Stops the client.
   * <p>
   * The RTM SDK tries to close the WebSocket connection asynchronously and doesn't start it again unless you call
   * {@link #start}.
   * <p>
   * Use this method to explicitly stop all interaction with RTM.
   * {@link RtmClientListener#onEnterStopped(RtmClient) RtmClientListener.onEnterStopped()} is
   * called when the client enters the
   * stopped state.
   */
  void stop();

  /**
   * Restarts the client by calling {@link #stop} followed by {@link #start}.
   */
  void restart();

  /**
   * Stops the client, terminates threads, and cleans up all allocated resources.
   * <p>
   * After you call {@code shutdown()}, you can't use your RTM client.
   */
  void shutdown();

  /**
   * Returns {@code true} if the RTM client is completely connected.
   * <p>
   * The client is connected when:
   * <ul>
   * <li>Connection to RTM is established</li>
   * <li>Authentication (if requested) is complete</li>
   * </ul>
   *
   * @return {@code true} if the client is completely connected, otherwise {@code false}
   */
  boolean isConnected();

  /**
   * Creates a subscription to the specified channel.
   * <p>
   * To create a subscription that has a streamfilter, call
   * {@link #createSubscription(String, SubscriptionConfig) createSubscription()}.
   * <p>
   * You can subscribe at any time. The RTM SDK manages the subscription and sends a subscribe
   * request when the RTM client is connected. Use {@code modes} to tell the SDK how to resubscribe
   * after a dropped connection.
   * <p>
   * To provide callbacks for events in the subscription lifecycle, use {@link SubscriptionListener}.
   *
   * @param channel  name of the channel
   * @param modes    subscription modes
   * @param listener subscription listener
   * @see SubscriptionAdapter
   */
  void createSubscription(String channel, EnumSet<SubscriptionMode> modes,
                          SubscriptionListener listener);

  /**
   * Creates a subscription to the specified subscription id.
   * <p>
   * This form of {@code createSubscription} lets you specify a streamfilter.
   * See {@link SubscriptionConfig#setFilter(String) SubscriptionConfig.setFilter()}. You set a
   * {@code SubscriptionListener} and subscription modes
   * in the {@link SubscriptionConfig#SubscriptionConfig(EnumSet, SubscriptionListener) SubscriptionConfig} constructor.
   * <p>
   * You can subscribe at any time. The RTM SDK manages the subscription and sends a subscribe
   * request when the RtmClient is connected. Use the {@code subscriptionConfig} parameter to set
   * various subscription options such as streamview, history, position, and so forth.
   * <p>
   * If you don't specify a streamview in the configuration, {@code channelOrSubId} is treated as a channel name.
   *
   * @param channelOrSubId     name of the channel or subscription id
   * @param subscriptionConfig subscription configuration
   */
  void createSubscription(String channelOrSubId, SubscriptionConfig subscriptionConfig);

  /**
   * Removes the subscription with the specific subscription id.
   * <p>
   * Removing a subscription triggers related callbacks in {@link SubscriptionListener}. To set this listener,
   * call {@link #createSubscription(String, EnumSet, SubscriptionListener) createSubscription()}.
   *
   * @param subscriptionId subscription id
   */
  void removeSubscription(String subscriptionId);

  /**
   * Publishes a message to a channel asynchronously.
   * <p>
   * To get the response returned by RTM, call {@link ListenableFuture#get}, or pass
   * {@code ListenableFuture} to {@link com.google.common.util.concurrent.Futures#addCallback} to set up a callback.
   * <p>
   * If the publish operation fails, then the exception is passed to the {@link ListenableFuture}
   * object:
   * <ul>
   * <li>
   * If you call {@link ListenableFuture#get() ListenableFuture.get()} to get the result,
   * it throws an
   * {@link java.util.concurrent.ExecutionException} and passes the original exception to it.
   * </li>
   * <li>
   * If you call
   * {@link com.google.common.util.concurrent.Futures#addCallback Futures.addCallback()} to
   * get the result,
   * the exception is passed to
   * {@link com.google.common.util.concurrent.FutureCallback#onFailure FutureCallback.onFailure()}
   * unaltered.
   * </li>
   * </ul>
   * <p>
   * The publish operation can fail with the following execution exceptions:
   * <ul>
   * <li>{@link IllegalStateException}: RTM SDK failed to add the message to the pending queue
   * because the queue is full.</li>
   * <li>{@link PduException}: an RTM error occured. For example, this exception is thrown if the
   * client isn't authorized to publish to the specified channel.</li>
   * <li>{@link TransportException}: a WebSocket transport error occurred.</li>
   * </ul>

   * For example:
   * <pre>
   *  String myMessage = "{\"message\": \"Hello\"}";
   *  ListenableFuture<Pdu<PublishReply>> publish("my_channel", myMessage, Ack.YES);
   *  Futures.addCallback(reply, new FutureCallback<Pdu<PublishReply>>() {
   *      public void onSuccess(Pdu<PublishReply> publishReplyPdu) {
   *         System.out.println("Animal is published: " + animal);
   *      }
   *      public void onFailure(Throwable t) {
   *         System.out.println("Publish request failed: " + t.getMessage());
   *      }
   *  });
   * </pre>
   *
   * @param channel name of the channel
   * @param message message to publish
   * @param ack     determines if RTM should acknowledge the publish operation
   * @param <T>     type of the {@code message} parameter
   * @return a {@link PublishReply}
   */
  <T> ListenableFuture<Pdu<PublishReply>> publish(final String channel, final T message, final Ack ack);

  /**
   * Publishes a {@link PublishRequest} asynchronously. This form of {@code publish} lets you
   * specify "time to live" (<strong>ttl</strong>) parameters. See the
   * {@link PublishRequest#PublishRequest(String, Object, long, Object) PublishRequest.PublishRequest(channel, message, ttl, ttl_message)}
   * constructor.
   * <p>
   * For example:
   * <pre>
   *  String myMessage = "{\"message\": \"Hello\"}";
   *  PublishRequest<String> pRequest =
   *      new PublishRequest<String>("my_channel", myMessage, 15, "Dead");
   *  ListenableFuture<Pdu<PublishReply>> publish(pRequest, Ack.YES);
   *  Futures.addCallback(reply, new FutureCallback<Pdu<PublishReply>>() {
   *      public void onSuccess(Pdu<PublishReply> publishReplyPdu) {
   *         System.out.println("Animal is published: " + animal);
   *      }
   *      public void onFailure(Throwable t) {
   *         System.out.println("Publish request failed: " + t.getMessage());
   *      }
   *  });
   * </pre>
   * To get the response returned by RTM, call {@link ListenableFuture#get}, or pass
   * {@code ListenableFuture} to {@link com.google.common.util.concurrent.Futures#addCallback} to
   * set up a callback.
   * <p>
   * If the publish operation fails, then the exception is passed to the {@link ListenableFuture}
   * object:
   * <ul>
   * <li>
   * If you call {@link ListenableFuture#get() ListenableFuture.get()} to get the result,
   * it throws an
   * {@link java.util.concurrent.ExecutionException} and passes the original exception to it.
   * </li>
   * <li>
   * If you call
   * {@link com.google.common.util.concurrent.Futures#addCallback Futures.addCallback()} to
   * get the result,
   * the exception is passed to
   * {@link com.google.common.util.concurrent.FutureCallback#onFailure FutureCallback.onFailure()}
   * unaltered.
   * </li>
   * </ul>
   * <p>
   * The publish operation can fail with the following execution exceptions:
   * <ul>
   * <li>{@link IllegalStateException}: RTM SDK failed to add the message to the pending queue
   * because the queue is full.</li>
   * <li>{@link PduException}: an RTM error occured. For example, this exception is thrown if the
   * client isn't authorized to publish to the specified channel.</li>
   * <li>{@link TransportException}: a WebSocket transport error occurred.</li>
   * </ul>
   *
   * @param request     publish request
   * @param ack         determines if RTM should acknowledge the publish operation
   * @param <T>         type of the message parameters
   * @return a {@link PublishReply}
   */
  <T> ListenableFuture<Pdu<PublishReply>> publish(PublishRequest<T> request, Ack ack);

  /**
   * Gets the current {@link Connection}.
   * <p>
   * If the client isn't connected, this method returns {@code null}.
   *
   * @return {@link Connection} or null if client isn't connected.
   */
  Connection getConnection();

  /**
   * Reads the value of the specified key from a key-value store. The operation is asynchronous.
   * <p>
   * The documentation for {@link #publish(String, Object, Ack)} publish()} describes how to get
   * the method response, and how to handle errors.
   *
   * @param key key name
   * @return result of the read operation, returned asynchronously
   * @see #publish(String, Object, Ack)
   */
  ListenableFuture<Pdu<ReadReply>> read(String key);

  /**
   * Reads the value of the specified key from a key-value store. The operation is asynchronous.
   * <p>
   * The documentation for {@link #publish(String, Object, Ack) publish()} describes how to get
   * the method response, and how to handle errors.
   *
   * @param request read request
   * @return asynchronous result of the read request
   */
  ListenableFuture<Pdu<ReadReply>> read(ReadRequest request);

  /**
   * Writes the specified key-value pair to a key-value store. The operation is asynchronous.
   * <p>
   * The documentation for {@link #publish(String, Object, Ack) publish()} describes how to get
   * the method response, and how to handle errors.
   *
   * @param key   key name
   * @param value value to store
   * @param ack   determines if RTM should acknowledge the write operation
   * @param <T>   type of serializable message
   * @return asynchronous result of the write request
   * @see #write(WriteRequest, Ack)
   */
  <T> ListenableFuture<Pdu<WriteReply>> write(String key, T value, Ack ack);

  /**
   * Writes the specified key-value pair to a key-value store. The operation is asynchronous.
   * <p>
   * The documentation for {@link #publish(String, Object, Ack) publish()} describes how to get
   * the method response, and how to handle errors.
   *
   * @param writeRequest write request
   * @param ack          determines if RTM should acknowledge the write operation
   * @param <T>          type of serializable message
   * @return asynchronous result of the write request
   */
  <T> ListenableFuture<Pdu<WriteReply>> write(WriteRequest<T> writeRequest, Ack ack);


  /**
   * Deletes the value of the specified key from the key-value store. The operation is asynchronous.
   * <p>
   * The documentation for {@link #publish(String, Object, Ack) publish()} describes how to get
   * the method response, and how to handle errors.
   *
   * @param key key name
   * @param ack determines if RTM should acknowledge the delete operation
   * @return asynchronous result of the delete request
   */
  ListenableFuture<Pdu<DeleteReply>> delete(String key, Ack ack);

  /**
   * Writes the specified key-value pair to a key-value store. The operation is asynchronous.
   * <p>
   * The documentation for {@link #publish(String, Object, Ack) publish()} describes how to get
   * the method response, and how to handle errors.
   *
   * @param deleteRequest write request
   * @param ack          determines if RTM should acknowledge the write operation
   * @param <T>          type of serializable message
   * @return asynchronous result of the write request
   */
  <T> ListenableFuture<Pdu<DeleteReply>> delete(DeleteRequest deleteRequest, Ack ack);
}
