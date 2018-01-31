package com.sensetime.test.java.test.common;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * This is a common Http Request Sender which disregards any business logic
 *
 * @author Hunt
 */
public class HttpRequestSender {
    public enum ArgsKey {TYPE, SCHEME, HOST, PORT, PATH, PARAM, HEADER, TIMEOUT, ENTITY_TYPE, ENTITY, CONTENT_TYPE}

    public enum Type {GET, POST, DELETE}

    /**
     * When ArgsKey.ENTITY_TYPE is set to EntityType.MULTIPART,
     * ArgsKey.ENTITY should be List<MultipartEntityTriplet>,
     * while ArgsKey.ENTITY_TYPE is set to EntityType.URL_ENCODED_FORM,
     * ArgsKey.ENTITY should be List<NameValuePair>.
     * And both of the above, the ArgsKey.CONTENT_TYPE will be disregarded.
     */
    public enum EntityType {
        STRING, STREAM, BINARY, FILE, MULTIPART, URL_ENCODED_FORM
    }

    private CloseableHttpClient httpClient;

    public HttpRequestSender() {
        httpClient = HttpClientBuilder.create().build();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    public CloseableHttpResponse send(final Map<ArgsKey, Object> requestArgs) {
        return send(requestArgs, null);
    }

    public CloseableHttpResponse send(final Map<ArgsKey, Object> requestArgs, HttpContext context) {
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

            HttpRequestBase httpRequest;
            switch ((Type)requestArgs.get(ArgsKey.TYPE)) {
                case GET:
                    httpRequest = new HttpGet(uri.build());
                    break;
                case DELETE:
                    httpRequest = new HttpDelete(uri.build());
                    break;
                case POST:
                    httpRequest = new HttpPost(uri.build());
                    if (!setEntityByType((HttpPost)httpRequest, (EntityType)requestArgs.get(ArgsKey.ENTITY_TYPE),
                            requestArgs.get(ArgsKey.ENTITY), (ContentType)requestArgs.get(ArgsKey.CONTENT_TYPE))) {
                        return null;
                    }
                    break;
                default:
                    return null;
            }

            if (requestArgs.containsKey(ArgsKey.HEADER)) {
                httpRequest.setHeaders(((List<Header>)requestArgs.get(ArgsKey.HEADER)).toArray(new Header[0]));
            }

            Integer timeout = 0;
            if (requestArgs.containsKey(ArgsKey.TIMEOUT)) {
                timeout = (Integer)requestArgs.get(ArgsKey.TIMEOUT);
            }
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setConnectionRequestTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .build();
            httpRequest.setConfig(config);

            CloseableHttpResponse response = httpClient.execute(httpRequest, context);

            return response;
        }
        catch (URISyntaxException | IOException e) {
            // Do nothing
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

        HttpEntity httpEntity;
        switch (entityType) {
            case STRING:
                httpEntity = EntityBuilder.create().setContentType(contentType).setText((String)entity).build();
                break;
            case STREAM:
                httpEntity = EntityBuilder.create().setContentType(contentType).setStream((InputStream)entity).build();
                break;
            case BINARY:
                httpEntity = EntityBuilder.create().setContentType(contentType).setBinary((byte[])entity).build();
                break;
            case MULTIPART:
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
                httpEntity = entityBuilder.build();
                break;
            case URL_ENCODED_FORM:
                try {
                    httpEntity = new UrlEncodedFormEntity((List<NameValuePair>)entity);
                }
                catch (UnsupportedEncodingException e) {
                    return false;
                }
                break;
            default:
                return false;
        }

        httpRequest.setEntity(httpEntity);
        return true;
    }

    @Override
    protected void finalize() throws Throwable {
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
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
                if (httpClient != null) {
                    httpClient.close();
                    httpClient = null;
                }
            }
            catch (IOException e) {
                // Do nothing
            }
        }
    }
}
