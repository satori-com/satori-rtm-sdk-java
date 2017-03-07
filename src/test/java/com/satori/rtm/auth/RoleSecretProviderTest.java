package com.satori.rtm.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RoleSecretProviderTest {
  @Test
  public void hashGeneration() {
    String hash = RoleSecretAuthProvider.calculateHash("B37Ab888CAB4343434bAE98AAAAAABC1", "nonce");
    assertThat(hash, equalTo("B510MG+AsMpvUDlm7oFsRg=="));
  }
}
