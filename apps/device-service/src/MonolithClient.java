import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP-клиент к монолиту smart_home.
 * Инкапсулирует интеграцию: пока функциональность не вынесена из монолита,
 * Device Service работает через его REST API (паттерн Strangler Fig).
 */
public class MonolithClient {
    private final HttpClient client;
    private final String baseUrl;

    public MonolithClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /** Ответ монолита: код и тело. */
    public record ApiResponse(int status, String body) {}

    public ApiResponse getSensor(String id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/sensors/" + id))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(resp.statusCode(), resp.body());
    }

    public ApiResponse updateSensorValue(String id, double value, String status) throws Exception {
        String json = String.format(java.util.Locale.US,
                "{\"value\": %.2f, \"status\": \"%s\"}", value, status);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/sensors/" + id + "/value"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(resp.statusCode(), resp.body());
    }
}
