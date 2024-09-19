package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static final Map<String, PrintWriter> clientWriters = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Chat server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Запрашиваем у клиента его имя
                clientName = in.readLine();
                System.out.println("Подключён новый клиент: " + clientName);

                // Отправляем сообщение всем, что новый клиент подключился
                broadcastMessage("Server: " + clientName + " has joined the chat");

                // Добавляем клиента в карту клиентов
                clientWriters.put(clientName, out);

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message + " from " + clientName);
                    // Рассылаем сообщение всем пользователям
                    broadcastMessage(clientName + ": " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Удаляем клиента из карты клиентов и сообщаем остальным
                clientWriters.remove(clientName);
                broadcastMessage("Server: " + clientName + " has left the chat");
            }
        }

        // Метод для рассылки сообщений всем клиентам
        private void broadcastMessage(String message) {
            for (PrintWriter writer : clientWriters.values()) {
                writer.println(message);
            }
        }
    }
}