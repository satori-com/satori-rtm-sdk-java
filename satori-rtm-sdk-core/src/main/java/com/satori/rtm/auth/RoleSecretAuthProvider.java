package com.satori.rtm.auth;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.satori.rtm.connection.Connection;
import com.satori.rtm.model.Pdu;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Provides authentication and authorization using a role-based authentication algorithm. The algorithm itself
 * provides the authentication. Because the algorithm is based on user roles, you can use different roles to control
 * authorization.
 * <p>
 * {@code RoleSecretAuthProvider} is based on the <strong>HMAC</strong> process, using the <strong>MD5</strong> hashing
 * routine:
 * <ul>
 * <li>The client obtains a nonce from the server in a handshake request.</li>
 * <li>The client then sends an authorization request with its role secret key hashed with the received nonce.</li>
 * </ul>
 * <p>
 * Obtain the role secret key for your application from the Dev Portal.
 */
public class RoleSecretAuthProvider implements AuthProvider {
  private final static String METHOD = "role_secret";
  private final String mRole;
  private final String mRoleKey;

  /**
   * Creates a {@code RoleSecretAuthProvider} to perform authentication for a specific role and role secret
   * key.
   *
   * @param role    user role to authenticate
   * @param roleKey role secret key for the role (get from Dev Portal)
   */
  public RoleSecretAuthProvider(String role, String roleKey) {
    this.mRole = role;
    this.mRoleKey = roleKey;
  }

  static String calculateHash(String roleKey, String nonce) {
    try {
      final String hashAlgorithm = "HMACMD5";
      SecretKeySpec keySpec = new SecretKeySpec(roleKey.getBytes(Charsets.UTF_8), hashAlgorithm);
      Mac mac = Mac.getInstance(hashAlgorithm);
      mac.init(keySpec);
      byte[] result = mac.doFinal(nonce.getBytes(Charsets.UTF_8));
      BaseEncoding base64 = BaseEncoding.base64();
      return base64.encode(result);
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    } catch (InvalidKeyException ex) {
      throw new RuntimeException(ex);
    }
  }

  public ListenableFuture<Void> authenticate(final Connection connection) {
    ListenableFuture<Pdu<HandshakeResponse>> handshakeResponse = handshake(connection, this.mRole);

    ListenableFuture<Pdu<AuthenticateResponse>> authenticate = Futures.transformAsync(
        handshakeResponse,
        new AsyncFunction<Pdu<HandshakeResponse>, Pdu<AuthenticateResponse>>() {
          @Override
          public ListenableFuture<Pdu<AuthenticateResponse>> apply(Pdu<HandshakeResponse> input)
              throws Exception {
            String nonce = input.getBody().data.nonce;
            return authenticate(connection, RoleSecretAuthProvider.this.mRoleKey, nonce);
          }
        }
    );

    return Futures.transform(
        authenticate,
        new Function<Pdu<AuthenticateResponse>, Void>() {
          @Override
          public Void apply(Pdu<AuthenticateResponse> input) {
            return null;
          }
        }
    );
  }

  private ListenableFuture<Pdu<HandshakeResponse>> handshake(Connection connection, String role) {
    HandshakeRequest<HandshakePayload> payload =
        new HandshakeRequest<HandshakePayload>(METHOD, new HandshakePayload(role));
    return connection.send("auth/handshake", payload, HandshakeResponse.class);
  }

  private ListenableFuture<Pdu<AuthenticateResponse>> authenticate(Connection connection,
                                                                   String roleKey, String nonce) {
    String hash = calculateHash(roleKey, nonce);
    AuthenticatedRequest<RoleBaseCredentials> payload =
        new AuthenticatedRequest<RoleBaseCredentials>(METHOD, new RoleBaseCredentials(hash));
    return connection.send("auth/authenticate", payload, AuthenticateResponse.class);
  }

  private static class HandshakeRequest<T> {
    private String method;
    private T data;

    HandshakeRequest(String method, T data) {
      this.method = method;
      this.data = data;
    }
  }

  private static class HandshakePayload {
    private String role;

    HandshakePayload(String role) {
      this.role = role;
    }
  }

  private static class HandshakeResponse {
    private HandshakeNonce data;

    public HandshakeResponse() { }
  }

  private static class HandshakeNonce {
    private String nonce;

    public HandshakeNonce() { }
  }

  private static class AuthenticatedRequest<T> {
    private String method;
    private T credentials;

    AuthenticatedRequest(String method, T credentials) {
      this.method = method;
      this.credentials = credentials;
    }
  }

  private static class AuthenticateResponse {}

  private static class RoleBaseCredentials {
    private String hash;

    RoleBaseCredentials(String hash) {
      this.hash = hash;
    }
  }
}
