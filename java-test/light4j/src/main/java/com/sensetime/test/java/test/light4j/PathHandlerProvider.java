package com.sensetime.test.java.test.light4j;

import com.networknt.server.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.util.Methods;

/**
 * Created by Hunt Tang <tangmingming@xiaomi.com> on 2/6/18.
 */
public class PathHandlerProvider implements HandlerProvider {
    @Override
    public HttpHandler getHandler() {
        return Handlers.path()
                .addExactPath("/ws", Handlers.websocket(new TestWebSocketConnectionCallback()))
                .addPrefixPath("/", Handlers.routing().add(Methods.GET, "/test", new TestHandler()));
    }
}
