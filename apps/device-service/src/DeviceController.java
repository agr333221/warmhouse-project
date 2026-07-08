import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST-контроллер Device Service.
 *
 * Эндпоинты:
 *   GET  /health                        — проверка живости
 *   GET  /api/v1/devices/{id}           — информация об устройстве (проксирует монолит)
 *   POST /api/v1/devices/{id}/commands  — команда устройству: {"command": "turn_on" | "turn_off"}
 */
public class DeviceController {
    private static final Pattern DEVICE_PATH = Pattern.compile("^/api/v1/devices/(\\d+)(/commands)?$");
    private static final Pattern COMMAND_FIELD = Pattern.compile("\"command\"\\s*:\\s*\"([a-zA-Z_]+)\"");

    private final MonolithClient monolith;

    public DeviceController(MonolithClient monolith) {
        this.monolith = monolith;
    }

    public void handleHealth(HttpExchange exchange) throws IOException {
        respond(exchange, 200, "{\"status\": \"ok\", \"service\": \"device-service\"}");
    }

    public void handleDevices(HttpExchange exchange) throws IOException {
        Matcher m = DEVICE_PATH.matcher(exchange.getRequestURI().getPath());
        if (!m.matches()) {
            respond(exchange, 404, "{\"error\": \"not found\"}");
            return;
        }
        String deviceId = m.group(1);
        boolean isCommand = m.group(2) != null;

        try {
            if (isCommand && "POST".equals(exchange.getRequestMethod())) {
                sendCommand(exchange, deviceId);
            } else if (!isCommand && "GET".equals(exchange.getRequestMethod())) {
                getDevice(exchange, deviceId);
            } else {
                respond(exchange, 405, "{\"error\": \"method not allowed\"}");
            }
        } catch (Exception e) {
            respond(exchange, 502, "{\"error\": \"monolith unavailable: " + e.getMessage() + "\"}");
        }
    }

    private void getDevice(HttpExchange exchange, String deviceId) throws Exception {
        MonolithClient.ApiResponse resp = monolith.getSensor(deviceId);
        respond(exchange, resp.status(), resp.body());
    }

    private void sendCommand(HttpExchange exchange, String deviceId) throws Exception {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Matcher m = COMMAND_FIELD.matcher(body);
        if (!m.find()) {
            respond(exchange, 400, "{\"error\": \"field 'command' is required\"}");
            return;
        }
        String command = m.group(1);

        // MVP: команда включения/выключения транслируется в вызов API монолита
        double value;
        String status;
        switch (command) {
            case "turn_on" -> { value = 1.0; status = "active"; }
            case "turn_off" -> { value = 0.0; status = "inactive"; }
            default -> {
                respond(exchange, 400, "{\"error\": \"unknown command: " + command + "\"}");
                return;
            }
        }

        MonolithClient.ApiResponse resp = monolith.updateSensorValue(deviceId, value, status);
        if (resp.status() >= 200 && resp.status() < 300) {
            String commandId = UUID.randomUUID().toString();
            respond(exchange, 202, "{\"command_id\": \"" + commandId + "\", \"device_id\": \""
                    + deviceId + "\", \"command\": \"" + command + "\", \"status\": \"done\"}");
        } else {
            respond(exchange, resp.status(), resp.body());
        }
    }

    private void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
