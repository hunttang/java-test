package com.sensetime.test.java.test.common;

import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

/**
 * This is a common Http Request Sender which disregards any business logic
 *
 * @author Hunt
 */
public class HttpRequestSender {
    public enum ArgsKey {TYPE, SCHEME, HOST, PORT, PATH, PARAM, TIMEOUT, ENTITY_TYPE, ENTITY, CONTENT_TYPE}

    public enum Type {GET, POST, DELETE}

    /**
     * When ArgsKey.ENTITY_TYPE is set to EntityType.MULTIPART,
     * ArgsKey.ENTITY should be List<MultipartEntityTriplet>,
     * and ArgsKey.CONTENT_TYPE will be disregarded.
     */
    public enum EntityType {
        STRING, STREAM, BINARY, FILE, MULTIPART
    }

    private final CloseableHttpClient httpClient;

    public HttpRequestSender() {
        httpClient = HttpClientBuilder.create().build();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    public CloseableHttpResponse send(final HashMap<ArgsKey, Object> requestArgs) {
        try {
            URIBuilder uri = new URIBuilder();
            if (requestArgs.containsKey(ArgsKey.SCHEME)) {
                uri.setScheme((String)requestArgs.get(ArgsKey.SCHEME));
            }
            else {
                uri.setScheme("http");
            }
            uri.setHost((String)requestArgs.get(ArgsKey.HOST));
            uri.setPath((String)requestArgs.get(ArgsKey.PATH));
            if (requestArgs.containsKey(ArgsKey.PORT)) {
                uri.setPort((int)requestArgs.get(ArgsKey.PORT));
            }
            if (requestArgs.containsKey(ArgsKey.PARAM)) {
                uri.setParameters((List<NameValuePair>)requestArgs.get(ArgsKey.PARAM));
            }

            Type type = (Type)requestArgs.get(ArgsKey.TYPE);

            Integer timeout = 0;
            if (requestArgs.containsKey(ArgsKey.TIMEOUT)) {
                timeout = (Integer)requestArgs.get(ArgsKey.TIMEOUT);
            }
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setConnectionRequestTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .build();

            HttpUriRequest httpRequest;
            if (type == Type.GET) {
                HttpGet httpGet = new HttpGet(uri.build());
                httpGet.setConfig(config);
                httpRequest = httpGet;
            }
            else if (type == Type.DELETE) {
                HttpDelete httpDelete = new HttpDelete(uri.build());
                httpDelete.setConfig(config);
                httpRequest = httpDelete;
            }
            else if (type == Type.POST) {
                HttpPost httpPost = new HttpPost(uri.build());
                httpPost.setConfig(config);
                httpRequest = httpPost;
                if (!setEntityByType((HttpPost)httpRequest, (EntityType)requestArgs.get(ArgsKey.ENTITY_TYPE),
                        requestArgs.get(ArgsKey.ENTITY), (ContentType)requestArgs.get(ArgsKey.CONTENT_TYPE))) {
                    return null;
                }
            }
            else {
                return null;
            }

            CloseableHttpResponse response = httpClient.execute(httpRequest);

            return response;
        }
        catch (URISyntaxException | IOException e) {
        }

        return null;
    }

    private boolean setEntityByType(final HttpEntityEnclosingRequestBase httpRequest, final EntityType entityType, final Object entity,
                                    final ContentType contentType) {
        // If none of these 3 variables is set, consider it as blank entity
        if (entityType == null && entity == null && contentType == null) {
            return true;
        }

        if (entityType == null || entity == null) {
            return false;
        }

        if (entityType == EntityType.STRING) {
            EntityBuilder entityBuilder = EntityBuilder.create().setContentType(contentType).setText((String)entity);
            httpRequest.setEntity(entityBuilder.build());
        }
        else if (entityType == EntityType.STREAM) {
            EntityBuilder entityBuilder = EntityBuilder.create().setContentType(contentType).setStream((InputStream)entity);
            httpRequest.setEntity(entityBuilder.build());
        }
        else if (entityType == EntityType.BINARY) {
            EntityBuilder entityBuilder = EntityBuilder.create().setContentType(contentType).setBinary((byte[])entity);
            httpRequest.setEntity(entityBuilder.build());
        }
        else if (entityType == EntityType.MULTIPART) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            for (MultipartEntityTriplet triplet : (List<MultipartEntityTriplet>)entity) {
                if (triplet.getType() == EntityType.STRING) {
                    entityBuilder.addTextBody(triplet.getName(), (String)triplet.getValue());
                }
                else if (triplet.getType() == EntityType.STREAM) {
                    entityBuilder.addBinaryBody(triplet.getName(), (InputStream)triplet.getValue());
                }
                else if (triplet.getType() == EntityType.BINARY) {
                    entityBuilder.addBinaryBody(triplet.getName(), (byte[])triplet.getValue());
                }
                else if (triplet.getType() == EntityType.FILE) {
                    entityBuilder.addBinaryBody(triplet.getName(), (File)triplet.getValue());
                }
                else {
                    return false;
                }
            }
            httpRequest.setEntity(entityBuilder.build());
        }
        else {
            return false;
        }

        return true;
    }

    @Override
    protected void finalize() throws Throwable {
        httpClient.close();
        super.finalize();
    }

    public static class MultipartEntityTriplet {
        private final String name;
        private final EntityType type;
        private final Object value;

        public MultipartEntityTriplet(final String name, final EntityType type, final Object value) {
            if (name == null || type == null) {
                throw new IllegalArgumentException(name + " may not be null");
            }
            this.name = name;
            this.type = type;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public EntityType getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }
    }

    private class ShutdownHook extends Thread {
        @Override
        public void run() {
            try {
                httpClient.close();
            }
            catch (IOException e) {
            }
        }
    }
}
