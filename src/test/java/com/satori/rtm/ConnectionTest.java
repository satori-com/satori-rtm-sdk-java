package com.satori.rtm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;

import com.satori.rtm.connection.GsonSerializer;
import com.satori.rtm.model.InvalidJsonException;
import com.satori.rtm.model.Pdu;
import com.satori.rtm.model.PduRaw;
import com.satori.rtm.model.SubscriptionError;
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

  @Test
  public void deserializePduIntoDifferentClasses() throws InvalidJsonException {
    GsonSerializer serializer = new GsonSerializer();
    PduRaw raw = serializer.parsePdu("{'action':'rtm/subscription/error'," +
        "'body':{'subscription_id':'channel','error':'error','field': 10}}");
    Pdu<SubscriptionError> pdu = raw.convertBodyTo(SubscriptionError.class);
    assertThat(pdu.getBody().getSubscriptionId(), equalTo("channel"));
    assertThat(pdu.getBody().getError(), equalTo("error"));
  }
}
