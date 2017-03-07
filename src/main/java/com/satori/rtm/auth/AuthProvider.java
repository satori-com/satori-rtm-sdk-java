package com.satori.rtm.auth;

import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.RtmClientBuilder;
import com.satori.rtm.connection.Connection;

/**
 * A tagging interface that all authentication providers must extend.
 * <p>
 * The AuthProvider interface provides authentication for applications users of the RTM Service.
 * <p>
 * Use an implementation of the AuthProvider interface with {@link RtmClientBuilder#setAuthProvider(AuthProvider)}
 * to perform authentication when the client connects to the RTM Service.
 * <p>
 * Optionally, you can call the {@code authenticate} method directly to authenticate an application user.
 */
public interface AuthProvider {
  /**
   * Asynchronously authenticates a user for a previously established connection.
   * <p>
   * The <a href="http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/ListenableFuture.html">ListenableFuture</a>
   * returned by this method is complete when an authentication response is received from the RTM Service.
   *
   * @param connection Previously established connection.
   * @return Result of an asynchronous authentication.
   * @see Connection
   */
  ListenableFuture<Void> authenticate(final Connection connection);
}
