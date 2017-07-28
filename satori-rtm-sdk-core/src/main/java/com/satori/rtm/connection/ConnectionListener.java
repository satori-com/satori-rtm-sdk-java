package com.satori.rtm.connection;

import com.satori.rtm.model.PduRaw;
import com.satori.rtm.transport.TransportListener;
import com.satori.rtm.model.InvalidJsonException;
import com.satori.rtm.model.PduException;

/**
 * Defines listener methods for changes in connection state. For example, you can add code that executes when the
 * connection receives an unsolicited Protocol Data Unit (<strong>PDU</strong>) or when an error occurs for the
 * connection.
 */
public interface ConnectionListener extends TransportListener {
  /**
   * Called when the client receives an unsolicited Protocol Data Unit (PDU) from RTM. An unsolicited PDU does not
   * contain an {@code id} element.
   *
   * @param pdu the unsolicited PDU
   */
  void onUnsolicitedPDU(PduRaw pdu);

  /**
   * Called when an error occurs for the Protocol Data Unit (PDU) that the client receives.
   * <p>
   * This method is called when one of the following exceptions occurs:
   * <ul>
   * <li>{@link InvalidJsonException} - the PDU contains JSON that isn't well-formed
   * <li>{@link PduException} - the PDU has a format that the RTM SDK doesn't recognize
   * </ul>
   * This method is also called when RTM returns a negative response to a request.
   *
   * @param error the returned exception
   */
  void onError(Exception error);
}
