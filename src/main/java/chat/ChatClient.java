package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            // Запрашиваем у пользователя его имя
            System.out.println();
            System.out.println("Enter your name: ");
            String clientName = scanner.nextLine();
            out.println(clientName);

            // Создаем поток для чтения сообщений от сервера
            Thread incomingMessages = new Thread(() -> {
                String message;
                try {
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            incomingMessages.start();

            // Создаем поток для отправки сообщений на сервер
            Thread outgoingMessages = new Thread(() -> {
                String message;
                while (true) {
                    System.out.print(clientName + ": ");
                    message = scanner.nextLine();
                    out.println(message);
                }
            });
            outgoingMessages.start();

            // Ожидание завершения потоков (опционально)
            outgoingMessages.join();
            incomingMessages.join();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}