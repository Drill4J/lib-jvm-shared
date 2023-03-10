/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.epam.drill.websocket.gen.*

@SharedImmutable
val lwsEventsDescription = mapOf(
    LWS_CALLBACK_PROTOCOL_INIT to "One-time call per protocol, per-vhost using it, so it can do initial setup / allocations etc",
    LWS_CALLBACK_PROTOCOL_DESTROY to "One-time call per protocol, per-vhost using it, indicating this protocol won't get used at all after this callback, the vhost is getting destroyed",
    LWS_CALLBACK_WSI_CREATE to " Outermost (earliest) wsi create notification to protocols [0]",
    LWS_CALLBACK_WSI_DESTROY to "Outermost(latest) wsi destroy notification to protocols [0]",
    LWS_CALLBACK_WSI_TX_CREDIT_GET to "Manually-managed connection received TX credit (len is int32)",
    LWS_CALLBACK_OPENSSL_LOAD_EXTRA_CLIENT_VERIFY_CERTS to "This callback allows your user direct OpenSSL where to find certificates the client can use to confirm the remote server identity",
    LWS_CALLBACK_OPENSSL_LOAD_EXTRA_SERVER_VERIFY_CERTS to "This callback allows your user code to load extra certificates into the server which allow it to verify the validity of certificates returned by clients",
    LWS_CALLBACK_OPENSSL_PERFORM_CLIENT_CERT_VERIFICATION to "This callback is generated during OpenSSL verification of the cert sent from the client",
    LWS_CALLBACK_OPENSSL_CONTEXT_REQUIRES_PRIVATE_KEY to "If configured for including OpenSSL support but no private key file has been specified, this is called to allow the user to set the private key directly via libopenssl and perform further operations if required",
    LWS_CALLBACK_SSL_INFO to "SSL connections only. An event you registered an interest in at the vhost has occurred on a connection using the vhost",
    LWS_CALLBACK_OPENSSL_PERFORM_SERVER_CERT_VERIFICATION to "This callback is called during OpenSSL verification of the cert sent from the server to the client",
    LWS_CALLBACK_SERVER_NEW_CLIENT_INSTANTIATED to "A new client has been accepted by the ws server. This callback allows setting any relevant property to it",
    LWS_CALLBACK_HTTP to "An http request has come from a client that is not asking to upgrade the connection to a websocket one. This is a chance to serve http content",
    LWS_CALLBACK_HTTP_BODY to "The next len bytes data from the http request body HTTP connection is now available in in",
    LWS_CALLBACK_HTTP_BODY_COMPLETION to "The expected amount of http request body has been delivered",
    LWS_CALLBACK_HTTP_FILE_COMPLETION to "A file requested to be sent down http link has completed",
    LWS_CALLBACK_HTTP_WRITEABLE to "You can write more down the http protocol link now",
    LWS_CALLBACK_CLOSED_HTTP to "When a HTTP(non - websocket) session ends",
    LWS_CALLBACK_FILTER_HTTP_CONNECTION to "Called when the request has been received and parsed from the client, but the response is not sent yet",
    LWS_CALLBACK_ADD_HEADERS to "This gives your user code a chance to add headers to a server transaction bound to your protocol",
    LWS_CALLBACK_VERIFY_BASIC_AUTHORIZATION to "This gives the user code a chance to accept or reject credentials provided HTTP to basic authorization",
    LWS_CALLBACK_CHECK_ACCESS_RIGHTS to "This gives the user code a chance to forbid an http access",
    LWS_CALLBACK_PROCESS_HTML to "This gives your user code a chance to mangle outgoing HTML",
    LWS_CALLBACK_HTTP_BIND_PROTOCOL to "You can bind different protocols (by name) to different parts of the URL space using callback mounts. This callback occurs in the new protocol when a wsi is bound to that protocol",
    LWS_CALLBACK_HTTP_DROP_PROTOCOL to "This is called when a transaction is unbound from a protocol. It indicates the connection completed its transaction and may do something different now. Any protocol allocation related to the http transaction processing should be destroyed",
    LWS_CALLBACK_HTTP_CONFIRM_UPGRADE to "This is your chance to reject an HTTP upgrade action",
    LWS_CALLBACK_ESTABLISHED_CLIENT_HTTP to "The HTTP client connection has succeeded, and is now connected to the server",
    LWS_CALLBACK_CLOSED_CLIENT_HTTP to "The HTTP client connection is closing",
    LWS_CALLBACK_RECEIVE_CLIENT_HTTP_READ to "Used to drain incoming data. In the case the incoming data was chunked, it will be split into multiple smaller callbacks for each chunk block, removing the chunk headers. If not chunked, it will appear all in one callback",
    LWS_CALLBACK_RECEIVE_CLIENT_HTTP to "This indicates data was received on the HTTP client connection. It does NOT actually drain or provide the data",
    LWS_CALLBACK_COMPLETED_CLIENT_HTTP to "The client transaction completed... at the moment this is the same as closing since transaction pipelining on client side is not yet supported",
    LWS_CALLBACK_CLIENT_HTTP_WRITEABLE to "When doing an HTTP type client connection, you can call lws_client_http_body_pending(wsi, 1) from LWS_CALLBACK_CLIENT_APPEND_HANDSHAKE_HEADER to get these callbacks sending the HTTP headers",
    LWS_CALLBACK_CLIENT_HTTP_REDIRECT to "We're handling a 3xx redirect... return nonzero to hang up",
    LWS_CALLBACK_ESTABLISHED to "After the server completes a handshake with an incoming client. If you built the library with ssl support, in is a pointer to the ssl struct associated with the connection or NULL",
    LWS_CALLBACK_CLOSED to "When the websocket session ends",
    LWS_CALLBACK_SERVER_WRITEABLE to "You will get one of these callbacks coming when the connection socket is able to accept another write packet without blocking. If it already was able to take another packet without blocking, you'll get this callback at the next call to the service loop function",
    LWS_CALLBACK_RECEIVE to "Data has appeared for this server endpoint from a remote client",
    LWS_CALLBACK_RECEIVE_PONG to "Servers receive PONG packets with this callback reason",
    LWS_CALLBACK_WS_PEER_INITIATED_CLOSE to "The peer has sent an unsolicited Close WS packet",
    LWS_CALLBACK_FILTER_PROTOCOL_CONNECTION to "Called when the handshake has been received and parsed from the client, but the response is not sent yet",
    LWS_CALLBACK_CONFIRM_EXTENSION_OKAY to "When the server handshake code sees that it does support a requested extension, before accepting the extension by additing to the list sent back to the client it gives this callback just to check that it's okay to use that extension",
    LWS_CALLBACK_CLIENT_CONNECTION_ERROR to "The request client connection has been unable to complete a handshake with the remote server",
    LWS_CALLBACK_CLIENT_FILTER_PRE_ESTABLISH to "This is the last chance for the client user code to examine the http headers and decide to reject the connection",
    LWS_CALLBACK_CLIENT_ESTABLISHED to "After your client connection completed the websocket upgrade handshake with the remote server",
    LWS_CALLBACK_CLIENT_CLOSED to "When a client websocket session ends",
    LWS_CALLBACK_CLIENT_APPEND_HANDSHAKE_HEADER to "This callback happens when a client handshake is being compiled",
    LWS_CALLBACK_CLIENT_RECEIVE to "Data has appeared from the server for the client connection",
    LWS_CALLBACK_CLIENT_RECEIVE_PONG to "Clients receive PONG packets with this callback reason",
    LWS_CALLBACK_CLIENT_WRITEABLE to "You will get one of these callbacks coming when the connection socket is able to accept another write packet without blocking. If it already was able to take another packet without blocking, you'll get this callback at the next call to the service loop function",
    LWS_CALLBACK_CLIENT_CONFIRM_EXTENSION_SUPPORTED to "When a ws client connection is being prepared to start a handshake to a server, each supported extension is checked with protocols[0] callback with this reason, giving the user code a chance to suppress the claim to support that extension by returning non-zero",
    LWS_CALLBACK_WS_EXT_DEFAULTS to "Gives client connections an opportunity to adjust negotiated extension defaults",
    LWS_CALLBACK_FILTER_NETWORK_CONNECTION to "Called when a client connects to the server at network level; the connection is accepted but then passed to this callback to decide whether to hang up immediately or not, based on the client IP",
    LWS_CALLBACK_GET_THREAD_ID to "Lws can accept callback when writable requests from other threads, if you implement this callback and return an opaque current thread ID integer",
    LWS_CALLBACK_ADD_POLL_FD to "Lws normally deals with its poll() or other event loop internally, but in the case you are integrating with another server you will need to have lws sockets share a polling array with the other server",
    LWS_CALLBACK_DEL_POLL_FD to "This callback happens when a socket descriptor needs to be removed from an external polling array",
    LWS_CALLBACK_CHANGE_MODE_POLL_FD to "This callback happens when lws wants to modify the events for a connection",
    LWS_CALLBACK_LOCK_POLL to "These allow the external poll changes driven by lws to participate in an external thread locking scheme around the changes, so the whole thing is threadsafe",
    LWS_CALLBACK_UNLOCK_POLL to "These allow the external poll changes driven by lws to participate in an external thread locking scheme around the changes, so the whole thing is threadsafe",
    LWS_CALLBACK_CGI to "CGI: CGI IO events on stdin / out / err are sent here on protocols[0]",
    LWS_CALLBACK_CGI_TERMINATED to "CGI: The related CGI process ended, this is called before the wsi is closed",
    LWS_CALLBACK_CGI_STDIN_DATA to "CGI: Data is, to be sent to the CGI process stdin, eg from a POST body",
    LWS_CALLBACK_CGI_STDIN_COMPLETED to "CGI: no more stdin is coming",
    LWS_CALLBACK_CGI_PROCESS_ATTACH to "CGI: Sent when the CGI process is spawned for the wsi",
    LWS_CALLBACK_SESSION_INFO to "This is only generated by user code using generic sessions.It's used to get a struct lws_session_info filled in by generic sessions with information about the logged-in user",
    LWS_CALLBACK_GS_EVENT to "Indicates an event happened to the Generic Sessions session",
    LWS_CALLBACK_HTTP_PMO to "Per-mount options for this connection, called before the normal",
    LWS_CALLBACK_HTTP to "When the mount has per-mount options",
    LWS_CALLBACK_RAW_PROXY_CLI_RX to "RAW mode client(outgoing) RX",
    LWS_CALLBACK_RAW_PROXY_SRV_RX to "RAW mode server(listening) RX",
    LWS_CALLBACK_RAW_PROXY_CLI_CLOSE to "RAW mode client (outgoing) is closing",
    LWS_CALLBACK_RAW_PROXY_SRV_CLOSE to "RAW mode server (listening) is closing",
    LWS_CALLBACK_RAW_PROXY_CLI_WRITEABLE to "RAW mode client (outgoing) may be written",
    LWS_CALLBACK_RAW_PROXY_SRV_WRITEABLE to "RAW mode server (listening) may be written",
    LWS_CALLBACK_RAW_PROXY_CLI_ADOPT to "RAW mode client (onward) accepted socket was adopted(equivalent to 'wsi created')",
    LWS_CALLBACK_RAW_PROXY_SRV_ADOPT to " RAW mode server(listening) accepted socket was adopted(equivalent to 'wsi created')",
    LWS_CALLBACK_RAW_RX to "RAW mode connection RX",
    LWS_CALLBACK_RAW_CLOSE to "RAW mode connection is closing",
    LWS_CALLBACK_RAW_WRITEABLE to "RAW mode connection may be written",
    LWS_CALLBACK_RAW_ADOPT to " RAW mode connection was adopted(equivalent to 'wsi created')",
    LWS_CALLBACK_RAW_CONNECTED to "outgoing client RAW mode connection was connected",
    LWS_CALLBACK_RAW_ADOPT_FILE to "RAW mode file was adopted(equivalent to 'wsi created')",
    LWS_CALLBACK_RAW_RX_FILE to "This is the indication the RAW mode file has something to read",
    LWS_CALLBACK_RAW_WRITEABLE_FILE to "RAW mode file is writeable",
    LWS_CALLBACK_RAW_CLOSE_FILE to "RAW mode wsi that adopted a file is closing",
    LWS_CALLBACK_TIMER to "When the time elapsed after a call to lws_set_timer_usecs(wsi, usecs) is up, the wsi will get one of these callbacks",
    LWS_CALLBACK_EVENT_WAIT_CANCELLED to "This is sent to every protocol of every vhost in response to lws_cancel_service() or lws_cancel_service_pt()",
    LWS_CALLBACK_CHILD_CLOSING to "Sent to parent to notify them a child is closing / being destroyed",
    LWS_CALLBACK_VHOST_CERT_AGING to "When a vhost TLS cert has its expiry checked, this callback is broadcast to every protocol of every vhost in case the protocol wants to take some action with this information",
    LWS_CALLBACK_VHOST_CERT_UPDATE to "When a vhost TLS cert is being updated, progress is reported to the vhost in question here, including completion and failure",
    LWS_CALLBACK_MQTT_ACK to "When a message is fully sent, if QoS0 this callback is generated to locally \"acknowledge\" it",
    LWS_CALLBACK_MQTT_RESEND to "In QoS1 or QoS2, this callback is generated instead of the _ACK one if we timed out waiting for a PUBACK or a PUBREC, and we must resend the message",
    LWS_CALLBACK_USER to "User code can use any including above without fear of clashes"
)
