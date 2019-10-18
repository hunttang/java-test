package com.sensetime.test.java.test.light4j;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by Hunt on 9/1/15.
 */
public class TestHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestHandler.class);
    private static final Gson GSON = new Gson();

    private static final String CLIENT_APP_ID = "1585928312337326";
    private static final String ACCESS_TOKEN = "24.e8a694aa2381eca6d7031431e29dc5dd.7200.1518425555.282335-10487822";

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        try {
            Map<String, Deque<String>> queryParam = exchange.getQueryParameters();

            LOGGER.info(queryParam.toString());

            String filename = queryParam.get("filename").getFirst();
            try (FileInputStream fis = new FileInputStream(filename)) {
                URIBuilder uri = new URIBuilder().setScheme("https").setHost("openapi.baidu.com").setPath("/rest/2.0/mms/general/url");

                String imageStr = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
                List<NameValuePair> formList = new ArrayList<>();
                formList.add(new BasicNameValuePair("image", imageStr));
                formList.add(new BasicNameValuePair("client_app_id", CLIENT_APP_ID));
                formList.add(new BasicNameValuePair("access_token", ACCESS_TOKEN));
                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(formList);

                HttpPost httpRequest = new HttpPost(uri.build());
                httpRequest.setEntity(formEntity);

                try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                     CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode != 200) {
                        exchange.setStatusCode(statusCode);
                        exchange.endExchange();
                        return;
                    }

                    String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                    JsonObject jsonObject = GSON.fromJson(content, JsonObject.class);
                    String resultUrl = jsonObject.getAsJsonObject("result").get("url").getAsString();

                    exchange.getResponseSender().send(resultUrl);
                }
            }
            /*
            String result = String.format("This is a test handler response on %s", DateTime.now().toString());

            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; charset=utf-8");
            exchange.getResponseSender().send(result);
            */
        }
        catch (Throwable e) {
            exchange.setStatusCode(400);
            exchange.endExchange();
            LOGGER.warn("Exception occurred when processing query! Probably due to client bad request!", e);
        }
    }
}
