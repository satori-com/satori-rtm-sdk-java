/**
 * Classes that represent JSON data models and exceptions as Protocol Data Units (PDU).
 * <p>
 * The models specify how the SDK parses and serializes JSON data.
 * <p>
 * The following PDU shows a sample subscribe request:
 * <pre>{@literal
 * {
 *     "action": "rtm/subscribe",
 *     "body": {
 *         "channel":"my-channel",
 *         "position": "0123456789"
 *     },
 *     "id":"0"
 * }}
 * </pre>
 * The Java SDK represents JSON with the following Plain Old Java Object (POJO):
 * <pre>
 * new Pdu&lt;SubscribeRequest&gt;("rtm/subscribe", new SubscribeRequest("my-channel", "0123456789"), "0")
 * </pre>
 */
package com.satori.rtm.model;
