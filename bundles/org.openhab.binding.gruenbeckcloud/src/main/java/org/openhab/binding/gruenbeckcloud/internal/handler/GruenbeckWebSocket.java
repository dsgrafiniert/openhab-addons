/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.gruenbeckcloud.internal.handler;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.openhab.binding.gruenbeckcloud.internal.api.model.Device;
import org.openhab.binding.gruenbeckcloud.internal.listener.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link GruenbeckWebSocket} is the class to manage GruenbeckCloud
 * Websocket communication for softeners
 * 
 * @author Dominik Schön - Initial contribution
 */
@NonNullByDefault
@WebSocket
public class GruenbeckWebSocket {

    private final Logger logger = LoggerFactory.getLogger(GruenbeckWebSocket.class);

    private final char record_separator = 0x1e;

    private static final long maxIdleTimeout = 60000;
    private final Device device;
    private final String websocketAuthToken;
    private final String websocketId;
    private final EventListener eventListener;

    private @Nullable Session session;
    private @Nullable WebSocketClient client;
    private boolean closing;

    public GruenbeckWebSocket(final EventListener evListener, final String wsConnectionId, final String wsAccessToken,
            final Device device2) {
        websocketAuthToken = wsAccessToken;
        websocketId = wsConnectionId;
        device = device2;
        eventListener = evListener;
    }

    /**
     * Starts the {@link GruenbeckWebSocket}.
     *
     * @throws Exception
     */
    public synchronized void start() throws Exception {
        final SslContextFactory sslContextFactory = new SslContextFactory();

        if (client == null || client.isStopped()) {
            client = new WebSocketClient(sslContextFactory);
            client.setMaxIdleTimeout(this.maxIdleTimeout);
            client.setMaxTextMessageBufferSize(64 * 1024);
            client.start();
        }

        if (session != null) {
            session.close();
        }

        logger.info("Connecting to Gruenbeck WebSocket...");
        session = client.connect(this,
                URI.create("wss://prod-eu-gruenbeck-signalr.service.signalr.net/client/?hub=gruenbeck&id=" + websocketId
                        + "&access_token=" + websocketAuthToken))
                .get();
    }

    /**
     * Stops the {@link InnogyWebSocket}.
     */
    public synchronized void stop() {
        this.closing = true;
        if (isRunning()) {
            logger.info("Closing session...");
            session.close();
            session = null;
        } else {
            session = null;
            logger.trace("Stopping websocket ignored - was not running.");
        }
    }

    /**
     * Return true, if the websocket is running.
     *
     * @return
     */
    public synchronized boolean isRunning() {
        return session != null && session.isOpen();
    }

    @OnWebSocketConnect
    public void onConnect(final Session session) {
        this.closing = false;
        logger.info("Connected to Gruenbeck Webservice.");
        logger.debug("Gruenbeck Websocket session: {}", session);
        final String initialMessage = "{\"protocol\":\"json\", \"version\":1 }" + record_separator;
        logger.debug(initialMessage);
        try {
            session.getRemote().sendString(initialMessage, new WriteCallback() {

                @Override
                public void writeSuccess() {
                    // TODO Auto-generated method stub
                    logger.debug("write success");

                }

                @Override
                public void writeFailed(@Nullable Throwable xt) {
                    logger.debug("write failes {}", xt.getLocalizedMessage());

                }
            });
            Future<Void> future = session.getRemote().sendStringByFuture(initialMessage);
            future.get(2, TimeUnit.SECONDS); // wait for send to complete.

            while (!future.isDone()) {
                logger.debug("still sending");

            }
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            logger.error("error during sendString - {}", e.getLocalizedMessage());
        }
    }

    @OnWebSocketClose
    public void onClose(final int statusCode, final String reason) {
        if (statusCode == StatusCode.NORMAL) {
            logger.info("Connection to Gruenbeck Webservice was closed normally. (code: {}). Reason: {}", statusCode,
                    reason);
        } else {
            logger.info("Connection to Gruenbeck Webservice was closed abnormally (code: {}). Reason: {}", statusCode,
                    reason);
            // eventListener.connectionClosed();
        }
    }

    @OnWebSocketError
    public void onError(final Throwable cause) {
        logger.error("Gruenbeck WebSocket onError() - {}", cause.getMessage());
        eventListener.onError(cause);
        
    }

    @OnWebSocketMessage
    public void onMessage(final String msg) {
        logger.debug("Gruenbeck WebSocket onMessage() - {}", msg);
        if (closing) {
            logger.debug("Gruenbeck WebSocket onMessage() - ignored, WebSocket is closing...");
        } else {
            try{
               String msg_stripped = msg.trim().replaceAll("\r\n", "\n").replaceAll("\r", "\n").replaceAll("[\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}&&[^\\s]]", "");
          eventListener.onEvent(msg_stripped, device);
            } catch (Exception e) {
                logger.debug("Gruenbeck WebSocket onMessage() error - {}", e.getMessage());

            }
        }
    }

    @OnWebSocketFrame
    public void onFrame(final Frame frame){
        logger.debug("onFrame: {}", frame);
    }


}
