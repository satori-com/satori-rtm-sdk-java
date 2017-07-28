package com.satori.rtm;

import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.connection.Connection;
import com.satori.rtm.model.DeleteReply;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduException;
import com.satori.rtm.model.PublishReply;
import com.satori.rtm.model.ReadReply;
import com.satori.rtm.model.ReadRequest;
import com.satori.rtm.model.SearchReply;
import com.satori.rtm.model.WriteReply;
import com.satori.rtm.model.WriteRequest;
import com.satori.rtm.transport.TransportException;
import java.util.EnumSet;

/**
 * The RtmClient interface is the main entry point for accessing the RTM Service. RtmClient
 * instances are thread-safe so you can reuse them freely across multiple threads.
 * <p>
 * RtmClient instance is built with {@link RtmClientBuilder}. RtmClient must be started
 * explicitly with {@link #start()} method to establish connection to RTM.
 * <p>
 * By default, the SDK attempts to reconnect to the RTM Service if the connection to RTM Service
 * fails for any reason. To disable this use {@link RtmClientBuilder#setAutoReconnect(boolean)}
 * method.
 * <p>
 * Any operation performed when client is in offline is queued and performed
 * when client reconnects to RTM. To disable this behaviour or change the length of this queue use
 * {@link RtmClientBuilder#setPendingActionQueueLength(int)} method.
 *
 * <p>Here is an example of how RtmClient is used to subscribe for a channel:
 *
 * <pre>
 * RtmClient client = new RtmClientBuilder(YOUR_ENDPOINT, YOUR_APPKEY)
 *   .setListener(new RtmClientAdapter() {
 *     &#64;Override
 *     public void onEnterConnected(RtmClient client) {
 *       System.out.println("Connected to Satori RTM!");
 *     }
 *   })
 *   .build();
 * client.createSubscription("my_channel", SubscriptionMode.SIMPLE, new SubscriptionAdapter() {
 *   &#64;Override
 *   public void onSubscriptionData(SubscriptionData data) {
 *     for (AnyJson json: data.getMessages()) {
 *       System.out.println("Got message: " + json);
 *     }
 *   }
 * });
 * client.start();
 * </pre>
 */
public interface RtmClient {
  /**
   * Starts the client, which then tries to connect to RTM asynchronously.
   * <p>
   * By default, the SDK attempts to reconnect to the RTM Service if the connection to RTM Service
   * fails for any reason.
   * <p>
   * To provide callbacks that respond to the events in the RtmClient or the connection lifestyle,
   * use {@link RtmClientBuilder#setListener(RtmClientListener)} method.
   *
   * @see RtmClientListener#onEnterConnecting(RtmClient)
   */
  void start();

  /**
   * Stops the client. The RTM SDK tries to close the WebSocket connection asynchronously and does
   * not start it again unless you call {@link RtmClient#start()}.
   * <p>
   * Use this method to explicitly stop all interaction with RTM.
   *
   * @see RtmClientListener#onEnterStopped(RtmClient)
   */
  void stop();

  /**
   * Restarts the client by calling {@link RtmClient#stop} followed by {@link RtmClient#start}.
   */
  void restart();

  /**
   * Stops the client, terminates threads, and cleans up all allocated resources.
   * <p>
   * You cannot use the instance of the {@link RtmClient} when it is shut down.
   */
  void shutdown();

  /**
   * Returns {@code true} if the client is completely connected.
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
   * You can subscribe at any time. The RTM SDK manages the subscription and sends a subscribe
   * request when the RtmClient is connected. Use {@code modes} to tell the SDK how to resubscribe
   * after a dropped connection.
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
   * You can subscribe at any time. The RTM SDK manages the subscription and sends a subscribe
   * request when the RtmClient is connected. Use the {@code subscriptionConfig} parameter to set
   * various subscription options such as filter, history, position etc.
   * <p>
   * If filter is not specified then subscription id is used as a channel name.
   *
   * @param channelOrSubId     name of the channel or subscription id
   * @param subscriptionConfig subscription configuration
   */
  void createSubscription(String channelOrSubId, SubscriptionConfig subscriptionConfig);

  /**
   * Removes the subscription with the specific subscription id.
   * <p>
   * Use the callback methods in {@link SubscriptionListener} to respond to the events in the
   * subscription lifecycle.
   *
   * @param subscriptionId subscription id
   */
  void removeSubscription(String subscriptionId);

  /**
   * Publishes a message to a channel asynchronously.
   * <p>
   * To get the response returned by RTM, call {@link ListenableFuture#get}, or pass
   * {@code ListenableFuture} to {@link com.google.common.util.concurrent.Futures#addCallback} to
   * set up a callback.
   * <p>
   * {@link ListenableFuture} can complete with the following execution exceptions:
   * <ul>
   * <li>{@link IllegalStateException}: RTM SDK fails to add the message to the pending queue
   * because the queue is full.</li>
   * <li>{@link PduException}: an RTM error occurs. For example, this exception is thrown if the
   * client is not authorized to publish to the specified channel.</li>
   * <li>{@link TransportException}: a WebSocket transport error occurred.</li>
   * </ul>
   *
   * @param channel name of the channel
   * @param message message to publish
   * @param ack     determines if RTM should acknowledge the publish operation
   * @param <T>     type of the {@code message} parameter
   * @return result of the publish operation, returned asynchronously
   */
  <T> ListenableFuture<Pdu<PublishReply>> publish(String channel, T message, Ack ack);

  /**
   * Returns the current connection or {@code null} if the client is not connected.
   *
   * @return Connection, or null if client is not connected.
   */
  Connection getConnection();

  /**
   * Reads the value of the specified key from a key-value store asynchronously.
   * <p>
   * The return value described in the {@link #publish(String, Object, Ack)} method.
   *
   * @param key key name
   * @return result of the read operation, returned asynchronously
   * @see #publish(String, Object, Ack)
   */
  ListenableFuture<Pdu<ReadReply>> read(String key);

  /**
   * Reads the value from a key-value store asynchronously.
   * <p>
   * The return value described in the {@link #publish(String, Object, Ack)} method.
   *
   * @param request read request
   * @return asynchronous result of the read request
   */
  ListenableFuture<Pdu<ReadReply>> read(ReadRequest request);

  /**
   * Writes the specified key-value pair to the key-value store asynchronously.
   * <p>
   * The return value described in the {@link #publish(String, Object, Ack)} method.
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
   * Writes the value to a key-value store asynchronously.
   * <p>
   * The return value described in the {@link #publish(String, Object, Ack)} method.
   *
   * @param writeRequest write request
   * @param ack          determines if RTM should acknowledge the write operation
   * @param <T>          type of serializable message
   * @return asynchronous result of the write request
   */
  <T> ListenableFuture<Pdu<WriteReply>> write(WriteRequest<T> writeRequest, Ack ack);


  /**
   * Deletes the value of the specified key from the key-value store asynchronously.
   * <p>
   * The return value described in the {@link #publish(String, Object, Ack)} method.
   *
   * @param key key name
   * @param ack determines if RTM should acknowledge the delete operation
   * @return asynchronous result of the delete request
   */
  ListenableFuture<Pdu<DeleteReply>> delete(String key, Ack ack);


  /**
   * Returns all channels that have a name that starts with {@code prefix} asynchronously.
   * <p>
   * This method passes RTM replies to the callback. RTM may send multiple
   * responses to the same search request: zero or more search result PDUs with
   * an action of `rtm/search/data` (depending on the results of the search). After the search
   * result PDUs, RTM follows with PDU with an action of `rtm/search/ok`.
   *
   * @param prefix   channels prefix.
   * @param callback callback that's invoked when the SDK receives a response from RTM
   */
  void search(String prefix, Callback<Pdu<SearchReply>> callback);
}
