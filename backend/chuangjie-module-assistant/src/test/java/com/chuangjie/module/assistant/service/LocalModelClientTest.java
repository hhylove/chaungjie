package com.chuangjie.module.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalModelClientTest {

    private final LocalModelClient client = new LocalModelClient(new ObjectMapper());

    @Test
    void acceptsPrivateModelAddresses() {
        assertDoesNotThrow(() -> client.validateEndpoint("http://127.0.0.1:8000/v1"));
        assertDoesNotThrow(() -> client.validateEndpoint("http://192.168.0.10:8000/v1"));
    }

    @Test
    void rejectsPublicAndRedirectableAddresses() {
        assertThrows(IllegalArgumentException.class, () -> client.validateEndpoint("https://127.0.0.1/v1"));
        assertThrows(IllegalArgumentException.class, () -> client.validateEndpoint("http://8.8.8.8/v1"));
        assertThrows(IllegalArgumentException.class, () -> client.validateEndpoint("http://example.com/v1"));
        assertThrows(IllegalArgumentException.class, () -> client.validateEndpoint("http://127.0.0.1:8000@evil.com/v1"));
    }

    @Test
    void readsBatchEmbeddingsFromLocalEndpoint() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/embeddings", exchange -> {
            byte[] body = "{\"data\":[{\"embedding\":[1,0]},{\"embedding\":[0,1]}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            List<double[]> vectors = client.embedBatch("http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                    "local-embedding", List.of("第一段", "第二段"));
            assertArrayEquals(new double[]{1, 0}, vectors.get(0));
            assertArrayEquals(new double[]{0, 1}, vectors.get(1));
        } finally { server.stop(0); }
    }
}
