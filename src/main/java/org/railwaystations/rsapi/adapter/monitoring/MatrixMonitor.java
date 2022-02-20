package org.railwaystations.rsapi.adapter.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.railwaystations.rsapi.core.ports.Monitor;
import org.railwaystations.rsapi.utils.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
@Profile("prod")
public class MatrixMonitor implements Monitor {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(MatrixMonitor.class);

    private final CloseableHttpClient httpclient;
    private final MatrixMonitorConfig config;

    public MatrixMonitor(final MatrixMonitorConfig config) {
        super();
        this.config = config;
        this.httpclient = HttpClients.custom().setDefaultRequestConfig(
                RequestConfig.custom()
                        .setSocketTimeout(5000)
                        .setConnectTimeout(5000)
                        .setConnectionRequestTimeout(10000).build()
        ).build();
    }

    @Override
    @Async
    public void sendMessage(final String message) {
        sendMessage(message, null);
    }

    @Override
    @Async
    public void sendMessage(final String message, final Path photo) {
        LOG.info("Sending message: {}", message);
        if (StringUtils.isBlank(config.getRoomUrl())) {
            LOG.warn("Skipping message, missing Matrix Room URL config");
            return;
        }
        try {
            final CloseableHttpResponse response = sendRoomMessage(new MatrixTextMessage(message));
            final int status = response.getStatusLine().getStatusCode();
            final String content = EntityUtils.toString(response.getEntity());
            if (status >= 200 && status < 300) {
                LOG.info("Got json response: {}", content);
            } else {
                LOG.error("Error reading json, status {}: {}", status, content);
            }

            if (photo != null) {
                sendPhoto(photo);
            }
        } catch (final RuntimeException | IOException e) {
            LOG.warn("Error sending MatrixMonitor message", e);
        }
    }

    private CloseableHttpResponse sendRoomMessage(final Object message) throws IOException {
        final String json = MAPPER.writeValueAsString(message);
        final HttpPost httpPost = new HttpPost(config.getRoomUrl() + "?access_token=" + config.getAccessToken());
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON.withCharset("UTF-8")));
        return httpclient.execute(httpPost);
    }

    private void sendPhoto(final Path photo) throws IOException {
        final HttpPost httpPost = new HttpPost(config.getUploadUrl() + "?filename" + photo.getFileName() + "&access_token=" + config.getAccessToken());
        httpPost.setEntity(new ByteArrayEntity(ImageUtil.scalePhoto(photo, 300), ContentType.getByMimeType(ImageUtil.extensionToMimeType(ImageUtil.getExtension(photo.getFileName().toString())))));
        final CloseableHttpResponse responseUpload = httpclient.execute(httpPost);
        final int statusUpload = responseUpload.getStatusLine().getStatusCode();
        final String contentUpload = EntityUtils.toString(responseUpload.getEntity());
        if (statusUpload >= 200 && statusUpload < 300) {
            LOG.info("Got json response: {}", contentUpload);
        } else {
            LOG.error("Error reading json, statusUpload {}: {}", statusUpload, contentUpload);
            return;
        }

        final MatrixUploadResponse matrixUploadResponse = MAPPER.readValue(contentUpload, MatrixUploadResponse.class);

        final CloseableHttpResponse responseImage = sendRoomMessage(new MatrixImageMessage(photo.getFileName().toString(), matrixUploadResponse.contentUri));
        final int statusImage = responseImage.getStatusLine().getStatusCode();
        final String contentImage = EntityUtils.toString(responseImage.getEntity());
        if (statusImage >= 200 && statusImage < 300) {
            LOG.info("Got json response: {}", contentImage);
        } else {
            LOG.error("Error reading json, statusUpload {}: {}", statusImage, contentImage);
        }
    }

    private static class MatrixTextMessage {

        private static final String MSGTYPE = "m.text";
        private final String body;

        private MatrixTextMessage(final String body) {
            this.body = body;
        }

        public String getBody() {
            return body;
        }

        public String getMsgtype() {
            return MSGTYPE;
        }

    }

    private static class MatrixImageMessage {

        private static final String MSGTYPE = "m.image";
        private final String body;
        private final String url;

        private MatrixImageMessage(final String body, final String url) {
            this.body = body;
            this.url = url;
        }

        public String getBody() {
            return body;
        }

        public String getMsgtype() {
            return MSGTYPE;
        }

        public String getUrl() {
            return url;
        }
    }

    private static class MatrixUploadResponse {
        @JsonProperty("content_uri")
        private String contentUri;
    }

}
