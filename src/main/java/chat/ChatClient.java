package chat;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static final String FILE_SETTINGS = "src/main/resources/settings.txt";
    private static String SERVER_ADDRESS;
    private static int PORT;
    private static boolean isRunning = true;  // Флаг для управления завершением работы

    public static void main(String[] args) {
        loadSettings(FILE_SETTINGS);

        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in);
             BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in))) {

            String login = handleLoginOrRegistration(in, out, consoleIn, socket);

            if (login == null) {
                System.out.println("Завершение работы...");
                System.exit(0);
            }

            // Поток для чтения сообщений от сервера
            Thread incomingMessages = new Thread(() -> {
                try {
                    String message;
                    while (isRunning && (message = in.readLine()) != null) {
                        System.out.println(message);
                        if (message.equals("Отключение...")) {
                            isRunning = false;  // Устанавливаем флаг, чтобы завершить другие потоки
                            break;
                        }
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        System.out.println("Соединение с сервером прервано.");
                    }
                }
            });
            incomingMessages.start();

            // Поток для отправки сообщений на сервер
            Thread outgoingMessages = new Thread(() -> {
                try {
                    String message;
                    while (isRunning) {
                        message = scanner.nextLine();
                        if (message.equalsIgnoreCase("exit")) {
                            out.println(login + " покидает чат.");
                            out.println("exit");  // Отправляем команду "exit" на сервер
                            isRunning = false;  // Останавливаем цикл отправки сообщений
                            break;
                        }
                        out.println(message);
                        System.out.println(login + ": " + message);
                    }
                } catch (Exception e) {
                    if (isRunning) {
                        System.out.println("Ошибка при отправке сообщения.");
                    }
                } finally {
                    closeConnection(out, in, socket);
                }
            });
            outgoingMessages.start();

            outgoingMessages.join();
            incomingMessages.join();

        } catch (IOException | InterruptedException e) {
            System.out.println("Ошибка подключения к серверу: " + e.getMessage());
        }
    }

    private static String handleLoginOrRegistration(BufferedReader in, PrintWriter out, BufferedReader consoleIn, Socket socket) throws IOException {
        System.out.println(in.readLine());
        System.out.println(in.readLine());
        String choice = consoleIn.readLine();
        out.println(choice);

        String login = "";
        String result;
        String password;

        switch (choice) {
            case "1":
                do {
                    System.out.println(in.readLine());
                    login = consoleIn.readLine(); // Логин
                    if (login.equals("exit")) {
                        out.println(login);
                        closeConnection(out, in, socket);
                    }
                    out.println(login);
                    result = in.readLine();
                    System.out.println(result);
                } while (result.contains("Попробуйте другой"));
                password = consoleIn.readLine(); // Пароль
                out.println(password);

                result = in.readLine();
                System.out.println(result);
                if (!result.contains("успешен")) {
                    closeConnection(out, in, socket);
                }
                break;

            case "2":
                do {
                    System.out.println(in.readLine());
                    login = consoleIn.readLine(); // Логин
                    out.println(login);
                    result = in.readLine();
                    System.out.println(result);
                } while (result.contains("Попробуйте другой"));

                password = consoleIn.readLine();   // Пароль
                out.println(password);

                result = in.readLine();
                System.out.println(result);
                if (!result.contains("успешна")) {
                    closeConnection(out, in, socket);
                }
                break;

            case "3":
                closeConnection(out, in, socket);
                break;
            default:
                System.out.println("Неверный выбор!");
                closeConnection(out, in, socket);
        }

        return login;
    }

    // Загрузка настроек из файла
    static void loadSettings(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    SERVER_ADDRESS = parts[0];
                    PORT = Integer.parseInt(parts[1]);
                } else {
                    System.out.println("Данные в файле повреждены");
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка при загрузке настроек из файла: " + e.getMessage());
        }
    }

    // Закрытие соединения и потоков
    private static void closeConnection(PrintWriter out, BufferedReader in, Socket socket) {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Отключение...");
        } catch (IOException e) {
            System.out.println("Ошибка при закрытии соединения: " + e.getMessage());
        }
        System.exit(0);
    }
}