package com.satori.rtm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;

import com.satori.rtm.utils.TrampolineExecutorService;
import org.junit.Before;
import org.junit.Test;
import java.util.concurrent.ExecutorService;

public class ConnectionTest {
  private ExecutorService mDispatcher;
  private RtmClientStateMachine mConnectionFSM;

  @Before
  public void setUp() {
    mDispatcher = new TrampolineExecutorService();
    RtmClientListener listener = mock(RtmClientListener.class);
    mConnectionFSM = new RtmClientStateMachine(null, listener, null, true, 10, 100, mDispatcher);
  }

  @Test
  public void reconnectIntervalTest() {
    assertThat(mConnectionFSM.getNextAwaitInterval(),
        allOf(greaterThanOrEqualTo(10L), lessThan(20L)));
    assertThat(mConnectionFSM.getNextAwaitInterval(),
        allOf(greaterThanOrEqualTo(20L), lessThan(30L)));
    assertThat(mConnectionFSM.getNextAwaitInterval(),
        allOf(greaterThanOrEqualTo(40L), lessThan(50L)));
    assertThat(mConnectionFSM.getNextAwaitInterval(),
        allOf(greaterThanOrEqualTo(80L), lessThan(90L)));

    for (int i = 0; i < 5; i++) {
      mConnectionFSM.getNextAwaitInterval();
    }

    assertThat(mConnectionFSM.getNextAwaitInterval(), equalTo(100L));
  }
}
