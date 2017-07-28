package com.satori.rtm.auth;

import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.RtmClientBuilder;
import com.satori.rtm.connection.Connection;

/**
 * Defines the authentication interface for RTM clients.
 * <p>
 * {@link RoleSecretAuthProvider} is an implementation of {@code AuthProvider} that provides role-based authentication.
 * <p>
 * To set an authenticator for the client, pass it to {@link RtmClientBuilder#setAuthProvider(AuthProvider) RtmClientBuilder.setAuthProvider()}.
 */
public interface AuthProvider {
  /**
   * Asynchronously authenticates a user for a previously established connection.
   * <p>
   * This method returns a <a href="http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/ListenableFuture.html">ListenableFuture</a>
   * that completes when the authentication is complete.
   *
   * @param connection Previously established connection.
   * @return Result of an asynchronous authentication.
   * @see Connection
   */
  ListenableFuture<Void> authenticate(final Connection connection);
}
