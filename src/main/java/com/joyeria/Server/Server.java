package com.joyeria.Server;
import com.joyeria.config.DatabaseConfig;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;

public class Server {

    public static void main(String[] args) throws Exception {
        // Crear servidor en puerto 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Definir endpoints
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/test-connection", new TestConnectionHandler());

        server.setExecutor(null); // Usa el executor por defecto
        server.start();

        System.out.println("✅ Servidor iniciado en http://localhost:8080");
    }

    // Handler para servir archivos estáticos (HTML, CSS, JS)
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/")) {
                path = "/index.html";
            }

            // Buscar archivo en src/main/resources/static
            File file = new File("src/main/resources/static" + path);

            if (file.exists() && !file.isDirectory()) {
                String contentType = getContentType(path);
                byte[] content = Files.readAllBytes(file.toPath());

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, content.length);

                OutputStream os = exchange.getResponseBody();
                os.write(content);
                os.close();
            } else {
                String response = "404 - Archivo no encontrado";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".json")) return "application/json";
            return "text/plain";
        }
    }

    // Handler para probar la conexión a la base de datos
    static class TestConnectionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Permitir CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            try {
                Connection conn = DatabaseConfig.getConnection();

                String response;
                if (conn != null && !conn.isClosed()) {
                    response = "{\"status\":\"success\",\"message\":\"✅ Conexión exitosa a la base de datos\"}";
                    conn.close();
                } else {
                    response = "{\"status\":\"error\",\"message\":\"❌ No se pudo conectar\"}";
                }

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);

                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();

            } catch (Exception e) {
                String errorResponse = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
                exchange.sendResponseHeaders(500, errorResponse.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(errorResponse.getBytes());
                os.close();
            }
        }
    }
}