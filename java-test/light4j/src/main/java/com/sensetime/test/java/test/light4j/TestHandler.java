package com.sensetime.test.java.test.light4j;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.Map;

/**
 * Created by Hunt on 9/1/15.
 */
public class TestHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        try {
            Map<String, Deque<String>> queryParam = exchange.getQueryParameters();

            String result = String.format("This is a test handler response on %s", DateTime.now().toString());

            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; charset=utf-8");
            exchange.getResponseSender().send(result);
        }
        catch (Throwable e) {
            exchange.setStatusCode(400);
            exchange.endExchange();
            LOGGER.warn("Exception occurred when processing query! Probably due to client bad request!", e);
        }
    }
}
