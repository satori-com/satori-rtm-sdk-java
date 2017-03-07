package com.satori.rtm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;

public class RtmClientBuilderTest {
  @Test
  public void appendVersionTest() {
    RtmClientBuilder builder = new RtmClientBuilder("foo", "bar");
    // legacy format
    assertThat(
        builder.createUri("ws://xxx.api.com/v1", "appkey").toString(),
        equalTo("ws://xxx.api.com/v1?appkey=appkey")
    );

    // new format with slash
    assertThat(
        builder.createUri("ws://xxx.api.com/", "appkey").toString(),
        equalTo("ws://xxx.api.com/v2?appkey=appkey")
    );

    // new format without slash
    assertThat(
        builder.createUri("ws://xxx.api.com", "appkey").toString(),
        equalTo("ws://xxx.api.com/v2?appkey=appkey")
    );

  }
}
