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

    public static Log logger = Log.getInstance(); //Логгер

    // создан для тестов
    public static Map<String, String> getUsers() {
        return users;
    }

    // создан для тестов
    public static Map<String, PrintWriter> getClientWriters() {
        return clientWriters;
    }

    public static void main(String[] args) {
        loadSettings(FILE_SETTINGS);
        loadUsersFromFile(USERS_FILE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер Сетевой чат запущен...");
            logger.log("Сервер Сетевой чат запущен...", Log.SERVER);
            while (true) {
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

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                // Попытка открыть потоки
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                logger.logError("Ошибка открытия потоков: " + e.getMessage(), Log.SERVER);
                System.out.println("Ошибка открытия потоков!");
                return;  // Выход из метода, так как не удается работать без потоков
            }
            try {
                run(in, out); // Метод для обработки общения с клиентом
            } catch (IOException e) {
                // Проверяем, разорвал ли клиент соединение
                if (e instanceof SocketException) {
                    System.out.println("Клиент " + clientName + " разорвал(а) соединение.");
                    logger.logError("Клиент " + clientName + " разорвал(а) соединение. " + e.getMessage(), Log.SERVER);
                    broadcastMessage(clientName + " самостоятельно разорвал(а) соединение.");
                } else {
                    logger.logError("Ошибка во время работы с клиентом: " + e.getMessage(), Log.SERVER);
                    System.out.println("Ошибка работы с клиентом!");
                }
            } finally {
                // Выполняем очистку и закрытие ресурсов
                clientWriters.remove(clientName);

                // Если клиент не разорвал соединение на этапе открытия потоков (или не возникла другая ошибка)
                if (!clientName.equals("error")) {
                    broadcastMessage(clientName + " покинул(a) чат");
                }

                try {
                    socket.close();
                } catch (IOException e) {
                    logger.logError("Ошибка закрытия сокета: " + e.getMessage(), Log.SERVER);
                }
            }
        }

        // Основной поток сервера  Аргументы добавил для Junit тестов
        public void run(BufferedReader in, PrintWriter out) throws IOException {
            out.println("Добро пожаловать в чат! Введите '1' для входа или '2' для регистрации:");
            out.println("Введите '3' для выхода без регистрации:");
            String choice = in.readLine();
            clientWriters.put("Гость", out);

            switch (choice) {
                case "1":
                    do {
                        clientName = handleLogin(in, out);
                    } while (clientName.equals("Гость"));
                    break;
                case "2":
                    do {
                        clientName = handleRegistration(in, out);
                    } while (clientName.equals("Гость"));
                    break;
                case "3":
                    out.println("Отключение...");
                    ChatServer.disconnectClient(this);
                    return;
                default:
                    out.println("Неверный выбор.");
                    ChatServer.disconnectClient(this);
                    return;
            }
            if (clientName.equalsIgnoreCase("error") || clientName.equals("exit")) {
                clientName = "Гость";
                ChatServer.disconnectClient(this);
                return;
            }
            System.out.println("Подключён новый посетитель: " + clientName);
            broadcastMessage(clientName + " присоединяется к чату");
            clientWriters.remove("Гость");
            clientWriters.put(clientName, out);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Получено сообщение: " + clientName + " : " + message);
                broadcastMessage(clientName + ": " + message);

                if (message.equalsIgnoreCase("exit")) {
                    ChatServer.disconnectClient(this);
                    return;
                }
            }
        }

        // Метод для рассылки сообщений всем клиентам
        private void broadcastMessage(String message) {
            for (PrintWriter writer : clientWriters.values()) {
                writer.println(message);
            }
            logger.log(message, Log.SERVER); // логирование
        }
    }

    // Загрузка настроек из файла
    private static void loadSettings(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    PORT = Integer.parseInt(parts[1]);
                } else {
                    System.out.println("Данные в файле повреждены");
                }
            }
        } catch (IOException e) {
            logger.logError("Ошибка при загрузке настроек из файла: " + e.getMessage(), Log.SERVER);
        }
    }

    // Загрузка пользователей из файла
    private static void loadUsersFromFile(String filePat) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePat))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    users.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            logger.logError("Ошибка при загрузке пользователей из файла: " + e.getMessage(), Log.SERVER);
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

    //Метод для обработки авторизации
    private static String handleLogin(BufferedReader in, PrintWriter out) throws IOException {

        out.println("Введите логин:");
        System.out.println("Ожидается ввод логина от клиента...");
        String login = in.readLine();

        if (login.equals("exit")) {
            return "exit";
        }
        if (!users.containsKey(login)) {
            out.println("Такого логина не существует. Попробуйте другой либо наберите exit для выхода:");
            return "Гость";
        }
        out.println("Введите пароль:");
        System.out.println("Ожидается ввод пароля от клиента...");
        String password = in.readLine();
        if (authenticate(login, password)) {
            out.println("Вход успешен! Добро пожаловать, " + login);
            return login;
        } else {
            out.println("Неверный логин или пароль.");
            return "error";
        }
    }

    // Метод для обработки регистрации
    private static String handleRegistration(BufferedReader in, PrintWriter out) throws IOException {
        System.out.println("Регистрация нового клиента...");
        out.println("Придумайте логин:");
        String login = in.readLine();

        if (users.containsKey(login)) {
            out.println("Логин уже существует. Попробуйте другой.");
            return "Гость";
        }

        out.println("Придумайте пароль:");
        String password = in.readLine();
        users.put(login, password);
        saveUserToFile(login, password);
        out.println("Регистрация успешна! Теперь вы можете войти.");
        return login;
    }

    private static boolean authenticate(String login, String password) {
        return users.containsKey(login) && users.get(login).equals(password);
    }

    // Метод для отключения клиента со стороны сервера
    public static void disconnectClient(ClientHandler clientHandler) {
        try {
            if (clientHandler != null && clientHandler.clientName != null && !clientHandler.clientName.equals("Гость")) {
                clientWriters.remove(clientHandler.clientName); // Удаляем по имени клиента
            }
            if (clientHandler.socket != null && !clientHandler.socket.isClosed()) {
                clientHandler.socket.close();
            }
            String msg;
            if (clientHandler.clientName.equals("Гость")) {
                msg = "Неавторизованный Гость отключен!";
                System.out.println(msg);
            } else {
                msg = "Клиент " + clientHandler.clientName + " отключен!";
                System.out.println(msg);
            }
            logger.log(msg, Log.SERVER);
        } catch (IOException e) {
            logger.logError("Ошибка при отключении клиента: " + e.getMessage(), Log.SERVER);
        }
    }
}