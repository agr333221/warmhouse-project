import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

/**
 * Device Service — MVP микросервис управления устройствами (Java).
 * Интегрируется с монолитом smart_home по HTTP: читает состояние
 * устройств (сенсоров) и отправляет им команды через API монолита.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(env("PORT", "8082"));
        String monolithUrl = env("SMARTHOME_API_URL", "http://app:8080");

        MonolithClient monolithClient = new MonolithClient(monolithUrl);
        DeviceController controller = new DeviceController(monolithClient);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", controller::handleHealth);
        server.createContext("/api/v1/devices", controller::handleDevices);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("Device Service started on :" + port + ", monolith at " + monolithUrl);
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
