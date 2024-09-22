package chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class ChatServerTests {
    private static final String FILE_SETTINGS = "src/main/resources/settings.txt";
    private static final String USERS_FILE = "src/main/resources/users.txt";
    private static final String FILE_LOG = "src/main/resources/log.txt";

    private Socket mockSocket;
    private BufferedReader mockReader;
    private PrintWriter mockWriter;

    @BeforeEach
    public void setup() {
        mockSocket = Mockito.mock(Socket.class);
        mockReader = Mockito.mock(BufferedReader.class);
        mockWriter = Mockito.mock(PrintWriter.class);
    }

    @Test
    public void testClientLogin() throws IOException {
        // Подготавливаем данные для регистрации
        when(mockReader.readLine())
                .thenReturn("1")  // Выбор регистрации
                .thenReturn("user")  // Логин
                .thenReturn("password123")  // Пароль
                .thenReturn("exit");  // Завершение соединения

        // Добавляем тестового пользователя в карту пользователей
        ChatServer.getUsers().put("user", "password123");

        ChatServer.ClientHandler clientHandler = new ChatServer.ClientHandler(mockSocket);
        clientHandler.run(mockReader, mockWriter);

        // Проверяем, что отправлялись ожидаемые сообщения клиенту
        verify(mockWriter).println("Добро пожаловать в чат! Введите '1' для входа или '2' для регистрации:");
        verify(mockWriter).println("Введите логин:");
        verify(mockWriter).println("Введите пароль:");
        verify(mockWriter).println("Вход успешен! Добро пожаловать, user");
        verify(mockWriter).println("Server: user присоединился к чату");
        verify(mockWriter).println("user: exit");

        assertFalse(ChatServer.getClientWriters().containsKey("user"));
    }

    @Test
    public void testClientRegistration() throws IOException {
        // Подготавливаем данные для регистрации
        when(mockReader.readLine())
                .thenReturn("2")  // Выбор регистрации
                .thenReturn("newuser")  // Логин
                .thenReturn("newpassword123")  // Пароль
                .thenReturn("exit");  // Завершение соединения


        ChatServer.ClientHandler clientHandler = new ChatServer.ClientHandler(mockSocket);
        clientHandler.run(mockReader, mockWriter);

        // Проверяем, что отправлялись ожидаемые сообщения клиенту
        verify(mockWriter).println("Добро пожаловать в чат! Введите '1' для входа или '2' для регистрации:");
        verify(mockWriter).println("Придумайте логин:");
        verify(mockWriter).println("Придумайте пароль:");
        verify(mockWriter).println("Регистрация успешна! Теперь вы можете войти.");
        verify(mockWriter).println("Server: newuser присоединился к чату");
        verify(mockWriter).println("newuser: exit");
    }

    @Test
    public void testLog() throws IOException {
        String message = "Test message";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String expectedLogMessage = "[" + dtf.format(now) + "] <" + message + ">\n";

        ChatServer.log(message);

        Path logFilePath = Paths.get(FILE_LOG);
        String fileContent = Files.readString(logFilePath);

        assertTrue(fileContent.contains(expectedLogMessage));
    }

    @Test
    public void testLogToFileWithException() throws IOException {

        FileWriter mockWriter = Mockito.mock(FileWriter.class);
        doThrow(new IOException("Test exception")).when(mockWriter).write(anyString());

        assertDoesNotThrow(() -> ChatServer.logToFile("Error message"));
    }
}
