package chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final String FILE_SETTINGS = "src/main/resources/settings.txt";
    private static final String USERS_FILE = "src/main/resources/users.txt";
    private static final String FILE_LOG = "src/main/resources/log.txt";
    private static final Map<String, String> users = new ConcurrentHashMap<>();
    private static final Map<String, PrintWriter> clientWriters = new ConcurrentHashMap<>();
    private static  int PORT;
    static StringBuilder log = new StringBuilder();

    public static void main(String[] args) {
        loadSettings();
        loadUsersFromFile();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat server started...");
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

                // Запрашиваем у пользователя его имя
                out.println("Добро пожаловать в чат! Введите '1' для входа или '2' для регистрации:");
                out.println("Введите '3' выхода и закрытия соединения:");
                String choice = in.readLine();

                switch(choice){
                    case "1":
                       clientName = handleLogin(in, out,this);
                        break;
                    case "2":
                        clientName = handleRegistration(in, out);
                        break;
                    case "3":
                        out.println("Отключение...");
                        ChatServer.disconnectClient(this);
                    default:
                        out.println("Неверный выбор.");
                        ChatServer.disconnectClient(this);
                }

                System.out.println("Подключён новый клиент: " + clientName);

                broadcastMessage("Server: " + clientName + " присоединился к чату");
                clientWriters.put(clientName, out);
                log("Server: " + clientName + " присоединился к чату");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("exit")) {
                        out.println("Отключение...");
                        break; // Остановка цикла при получении команды "exit"
                    }
                    System.out.println("Получено сообщение: " + message + " от " + clientName);
                    broadcastMessage(clientName + ": " + message);
                    log(clientName + ": " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clientWriters.remove(clientName);
                broadcastMessage("Server: " + clientName + " покинул чат");
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Метод для рассылки сообщений всем клиентам
        private void broadcastMessage(String message) {
            for (PrintWriter writer : clientWriters.values()) {
                writer.println(message);
            }
        }
    }
    // Загрузка настроек из файла
    private static void loadSettings() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_SETTINGS))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    PORT = Integer.parseInt(parts[1]);
                }
                else {
                    System.out.println("Данные в файле повреждены");
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка при загрузке настроек из файла: " + e.getMessage());
        }
    }
    // Загрузка пользователей из файла
    private static void loadUsersFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    users.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка при загрузке пользователей из файла: " + e.getMessage());
        }
    }
    // Сохранение нового пользователя в файл
    private static void saveUserToFile(String login, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            writer.write(login + ":" + password);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении пользователя: " + e.getMessage());
        }
    }
    private static String handleLogin(BufferedReader in, PrintWriter out, ClientHandler clientHandler) throws IOException {
        out.println("Введите логин:");
        String login = in.readLine();

        out.println("Введите пароль:");
        String password = in.readLine();

        if (authenticate(login, password)) {
            out.println("Вход успешен! Добро пожаловать, " + login);
            return login;  // Возвращаем логин, если аутентификация успешна
        } else {
            out.println("Неверный логин или пароль.");
            out.println("Отсоединение...");
            ChatServer.disconnectClient(clientHandler);
            return null;  // Возвращаем null при неудачной попытке входа
        }
    }

    // Метод для обработки регистрации
    private static String handleRegistration(BufferedReader in, PrintWriter out) throws IOException {
        out.println("Придумайте логин:");
        String login = in.readLine();

        if (users.containsKey(login)) {
            out.println("Логин уже существует. Попробуйте другой.");
        }

        out.println("Придумайте пароль:");
        String password = in.readLine();
        users.put(login, password);
        saveUserToFile(login, password);
        out.println("Регистрация успешна! Теперь вы можете войти.");

        return login;
    }

    // Аутентификация пользователя
    private static boolean authenticate(String login, String password) {
        return users.containsKey(login) && users.get(login).equals(password);
    }
    public static void disconnectClient(ClientHandler clientHandler) {
        try {
            if (clientHandler != null && clientHandler.clientName != null) {
                clientWriters.remove(clientHandler.clientName); // Удаляем по имени клиента
            }
            clientHandler.socket.close(); // Закрываем сокет
            System.out.println("Клиент " + clientHandler.clientName + " отключен.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void logToFile(StringBuilder msg) {
        try (FileWriter logWriter = new FileWriter(FILE_LOG, true)) {
            logWriter.write(msg.toString());
        } catch (IOException e) {
            System.err.println("Ошибка при записи лога: " + e.getMessage());
        }
    }
    public static void log(String msg) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        log.append("[").append(dtf.format(now)).append("] <").append(msg).append(">").append("\n");
        logToFile(log);
    }

}