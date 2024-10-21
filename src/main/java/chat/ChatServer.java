package chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final String FILE_SETTINGS = "src/main/resources/settings.txt";
    private static final String USERS_FILE = "src/main/resources/users.txt";
    private static final Map<String, String> users = new ConcurrentHashMap<>();
    private static final Map<String, PrintWriter> clientWriters = new ConcurrentHashMap<>();
    private static int PORT;

    public static Log logger = Log.getInstance(); // Логгер
    private static volatile boolean isRunning = true;

    // методы для тестов
    public static Map<String, String> getUsers() {
        return users;
    }

    public static Map<String, PrintWriter> getClientWriters() {
        return clientWriters;
    }

    public static void main(String[] args) {
        loadSettings(FILE_SETTINGS);
        loadUsersFromFile(USERS_FILE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер Сетевой чат запущен...");
            logger.log("Сервер Сетевой чат запущен...", Log.SERVER);
            while (isRunning) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            logger.logError("Ошибка запуска Сервера: " + e.getMessage(), Log.SERVER);
        }
    }

    // класс обработки соединений в потоках
    public static class ClientHandler extends Thread {
        private final Socket socket;
        private String clientName = "Гость";
        private BufferedReader in;
        private PrintWriter out;

        // Конструктор для использования в тестах
        public ClientHandler(Socket socket, BufferedReader in, PrintWriter out) {
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        // Конструктор для реального использования
        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                handleSocketException(e);
            }
        }

        public void run() {
            logger.log("Подключен новый клиент: " + socket.getRemoteSocketAddress(), Log.SERVER);
            try {
                handleClientConnection();
            } catch (IOException e) {
                handleSocketException(e);
            } finally {
                cleanupResources();
                logger.log("Соединение с клиентом " + clientName + " закрыто.", Log.SERVER);
            }
        }

        private void handleClientConnection() throws IOException {
            out.println("Добро пожаловать в чат! Введите '1' для входа или '2' для регистрации:");
            out.println("Введите '3' для выхода без регистрации:");
            String choice = in.readLine();
            clientWriters.put("Гость", out);

            switch (choice) {
                case "1":
                    clientName = handleLogin(in, out);
                    break;
                case "2":
                    clientName = handleRegistration(in, out);
                    break;
                case "3":
                    disconnectClient(this);
                    return;
                default:
                    out.println("Неверный выбор.");
                    disconnectClient(this);
                    return;
            }
            clientWriters.remove("Гость");

            if (clientName.equals("error") || clientName.equals("exit")) {
                disconnectClient(this);
                return;
            }

            broadcastMessage(clientName + " присоединяется к чату");
            clientWriters.put(clientName, out);

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("exit")) {
                    disconnectClient(this);
                    return;
                }
                broadcastMessage(clientName + ": " + message);
            }
        }

        // Обработка ошибок сокета
        private void handleSocketException(IOException e) {
            if (e instanceof SocketException) {
                System.out.println("Клиент " + clientName + " разорвал соединение.");
                broadcastMessage(clientName + " разорвал соединение.");
            } else {
                logger.logError("Ошибка во время работы с клиентом: " + e.getMessage(), Log.SERVER);
            }
        }

        // Очистка ресурсов
        private void cleanupResources() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                clientWriters.remove(clientName);
                broadcastMessage(clientName + " покинул чат.");
            } catch (IOException e) {
                logger.logError("Ошибка при закрытии ресурсов: " + e.getMessage(), Log.SERVER);
            }
        }

        // Рассылка сообщений всем клиентам
        private void broadcastMessage(String message) {
            for (PrintWriter writer : clientWriters.values()) {
                writer.println(message);
            }
            logger.log(message, Log.SERVER);
        }
    }

    // Загрузка настроек из файла
    private static void loadSettings(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    PORT = Integer.parseInt(parts[1].trim());
                }
            }
        } catch (IOException | NumberFormatException e) {
            logger.logError("Ошибка при загрузке настроек: " + e.getMessage(), Log.SERVER);
        }
    }

    // Загрузка пользователей из файла
    private static void loadUsersFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    users.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            logger.logError("Ошибка при загрузке пользователей: " + e.getMessage(), Log.SERVER);
        }
    }

    // Сохранение нового пользователя в файл
    private static void saveUserToFile(String login, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            writer.write(login + ":" + password);
            writer.newLine();
        } catch (IOException e) {
            logger.logError("Ошибка при сохранении пользователя: " + e.getMessage(), Log.SERVER);
        }
    }

    // Авторизация клиента
    private static String handleLogin(BufferedReader in, PrintWriter out) throws IOException {
        out.println("Введите логин:");
        String login = in.readLine();
        if (login.equals("exit")) {
            return "exit";
        }
        if (!users.containsKey(login)) {
            out.println("Логин не существует. Попробуйте снова или введите 'exit' для выхода.");
            return "Гость";
        }
        out.println("Введите пароль:");
        String password = in.readLine();
        if (authenticate(login, password)) {
            out.println("Вход успешен! Добро пожаловать, " + login);
            return login;
        } else {
            out.println("Неверный логин или пароль.");
            return "error";
        }
    }

    // Регистрация клиента
    private static String handleRegistration(BufferedReader in, PrintWriter out) throws IOException {
        out.println("Введите логин:");
        String login = in.readLine();
        if (users.containsKey(login)) {
            out.println("Логин уже существует. Попробуйте другой.");
            return "Гость";
        }
        out.println("Введите пароль:");
        String password = in.readLine();
        users.put(login, password);
        saveUserToFile(login, password);
        out.println("Регистрация успешна!");
        return login;
    }

    // Аутентификация клиента
    private static boolean authenticate(String login, String password) {
        return users.containsKey(login) && users.get(login).equals(password);
    }

    // Отключение клиента
    public static void disconnectClient(ClientHandler clientHandler) {
        try {
            if (clientHandler != null) {
                if (clientHandler.clientName != null && !clientHandler.clientName.equals("Гость")) {
                    PrintWriter writer = clientWriters.remove(clientHandler.clientName);
                    if (writer != null) {
                        writer.close();  // Закрываем PrintWriter, если он существует
                    }
                }
                if (clientHandler.socket != null && !clientHandler.socket.isClosed()) {
                    clientHandler.socket.close();
                }
                logger.log(clientHandler.clientName + " отключен!", Log.SERVER);
            } else {
                logger.log("Клиент отключен, но объект ClientHandler равен null.", Log.SERVER);
            }
        } catch (IOException e) {
            logger.logError("Ошибка при отключении клиента: " + e.getMessage(), Log.SERVER);
        }
    }
    // Метод для безопасного завершения работы сервера
    public static void stopServer() {
        isRunning = false;
        // Логирование или действия по остановке
    }
}