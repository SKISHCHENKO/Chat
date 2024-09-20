package chat;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static final String FILE_SETTINGS = "src/main/resources/settings.txt";
    private static String SERVER_ADDRESS;
    private static int PORT;

    public static void main(String[] args) {
        loadSettings();

        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in);
             BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in))) {

            String login = handleLoginOrRegistration(in, out, consoleIn);

            if (login == null) {
                System.out.println("Завершение работы...");
                System.exit(0);
            }

            // Создаем поток для чтения сообщений от сервера
            Thread incomingMessages = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                        if(message.equals("Отсоединение...")){
                            System.exit(0);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Соединение с сервером прервано.");
                }
            });
            incomingMessages.start();

            // Поток для отправки сообщений на сервер
            String finalLogin = login;
            Thread outgoingMessages = new Thread(() -> {
                try {
                    String message;
                    while (true) {
                        message = scanner.nextLine();
                        if (message.equalsIgnoreCase("exit")) {
                            out.println("Client " + finalLogin + " покидает чат.");
                            break;
                        }
                        out.println(message);
                    }
                } catch (Exception e) {
                    System.out.println("Ошибка при отправке сообщения.");
                }
            });
            outgoingMessages.start();

            // Ожидание завершения потоков
            outgoingMessages.join();
            incomingMessages.join();

        } catch (IOException | InterruptedException e) {
            System.out.println("Ошибка подключения к серверу: " + e.getMessage());
        }
    }

    // Метод для обработки входа или регистрации
    private static String handleLoginOrRegistration(BufferedReader in, PrintWriter out, BufferedReader consoleIn) throws IOException {
        System.out.println(in.readLine());
        System.out.println(in.readLine());
        String choice = consoleIn.readLine();
        out.println(choice);

        String login = null;

        switch (choice) {
            case "1":  // Вход
                System.out.println(in.readLine()); // Введите логин
                login = consoleIn.readLine();
                out.println(login);

                System.out.println(in.readLine()); // Введите пароль
                String password = consoleIn.readLine();
                out.println(password);

                // Чтение результата аутентификации
                System.out.println(in.readLine());
                break;

            case "2":  // Регистрация
                System.out.println(in.readLine()); // Придумайте логин
                login = consoleIn.readLine();
                out.println(login);

                System.out.println(in.readLine()); // Придумайте пароль
                password = consoleIn.readLine();
                out.println(password);

                // Чтение результата регистрации
                System.out.println(in.readLine());
                break;

            case "3":  // Завершение работы
                System.out.println("Завершение работы...");
                return null;

            default:
                System.out.println("Неверный выбор!");
                return null;
        }

        return login;
    }

    // Загрузка настроек из файла
    private static void loadSettings() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_SETTINGS))) {
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
}