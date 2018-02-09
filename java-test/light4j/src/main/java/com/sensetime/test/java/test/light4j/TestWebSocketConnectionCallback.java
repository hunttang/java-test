package com.sensetime.test.java.test.light4j;

import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.joda.time.DateTime;

/**
 * Created by Hunt Tang <tangmingming@xiaomi.com> on 2/9/18.
 */
public class TestWebSocketConnectionCallback implements WebSocketConnectionCallback {
    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        channel.getReceiveSetter().set(new TestWebSocketListener());
        channel.resumeReceives();
    }

    private final class TestWebSocketListener extends AbstractReceiveListener {
        @Override
        protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
            String result = String.format("This is a test WebSocket handler response on %s", DateTime.now().toString());
            WebSockets.sendText(result, channel, null);

            try {
                Thread.sleep(3000);
            }
            catch (Exception e) {
                // Ignore it
            }

            result = String.format("This is another test WebSocket handler response on %s", DateTime.now().toString());
            WebSockets.sendText(result, channel, null);
        }
    }
}
