package backend;

import com.google.gson.Gson;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainServer {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        server.createContext("/add-expense", new AddExpenseHandler());
        server.createContext("/balances", new BalanceHandler());
        server.createContext("/settlements", new SettlementHandler());

        server.setExecutor(null); // Use default executor
        System.out.println("✅ Server started at http://localhost:8000");
        server.start();
    }

    // ✅ CORS Preflight
    private static boolean handleCorsPreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1); // No Content
            return true;
        }
        return false;
    }

    // ✅ POST /add-expense
    static class AddExpenseHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflight(exchange)) return;
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            AddExpenseRequest req = new Gson().fromJson(reader, AddExpenseRequest.class);

            ExpenseManager.addExpense(req.group, req.payer, req.amount, req.participants); // assuming you removed `description` param

            String response = "Expense added successfully";
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }

        static class AddExpenseRequest {
            String group;
            String payer;
            double amount;
            List<String> participants;
            // String description; // If you plan to support this, uncomment it and handle in ExpenseManager
        }
    }

    // ✅ GET /balances?group=Trip
    static class BalanceHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflight(exchange)) return;
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            String query = exchange.getRequestURI().getQuery(); // e.g. group=Trip
            if (query == null || !query.startsWith("group=")) {
                String error = "{\"error\": \"Missing or invalid group parameter\"}";
                exchange.sendResponseHeaders(400, error.length());
                exchange.getResponseBody().write(error.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().close();
                return;
            }

            String group = query.split("=")[1];
            List<String> balances = ExpenseManager.getBalances(group);

            String json = new Gson().toJson(balances);
            byte[] response = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    // ✅ GET /settlements?group=Trip
    static class SettlementHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCorsPreflight(exchange)) return;
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            String query = exchange.getRequestURI().getQuery(); // e.g. group=Trip
            if (query == null || !query.startsWith("group=")) {
                String error = "{\"error\": \"Missing or invalid group parameter\"}";
                exchange.sendResponseHeaders(400, error.length());
                exchange.getResponseBody().write(error.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().close();
                return;
            }

            String group = query.split("=")[1];
            List<String> settlements = ExpenseManager.getSettlements(group);

            String json = new Gson().toJson(settlements);
            byte[] response = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }
}
