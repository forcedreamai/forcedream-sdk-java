package ai.forcedream.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Thin wrapper over Java's built-in HttpClient (since Java 11) -- no external HTTP dependency needed. */
final class Http {
    static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private Http() {}

    record Result(int status, JsonNode json) {}

    static Result get(String url, String apiKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
        if (apiKey != null) builder.header("Authorization", "Bearer " + apiKey);
        HttpResponse<String> res = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json;
        try {
            json = MAPPER.readTree(res.body());
        } catch (Exception e) {
            json = MAPPER.nullNode();
        }
        return new Result(res.statusCode(), json);
    }

    static Result post(String url, Object body, String apiKey) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (apiKey != null) builder.header("Authorization", "Bearer " + apiKey);
        HttpResponse<String> res = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode responseJson;
        try {
            responseJson = MAPPER.readTree(res.body());
        } catch (Exception e) {
            responseJson = MAPPER.nullNode();
        }
        return new Result(res.statusCode(), responseJson);
    }
}
