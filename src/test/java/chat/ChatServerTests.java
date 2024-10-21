package chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class ChatServerTests {

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
        // Подготавливаем данные для входа
        when(mockReader.readLine())
                .thenReturn("1")  // Выбор входа
                .thenReturn("user")  // Логин
                .thenReturn("password123")  // Пароль
                .thenReturn("exit");  // Завершение соединения

        // Добавляем тестового пользователя в карту пользователей
        ChatServer.getUsers().put("user", "password123");

        // Передаем моки в конструктор ClientHandler
        ChatServer.ClientHandler clientHandler = new ChatServer.ClientHandler(mockSocket, mockReader, mockWriter);
        clientHandler.start(); // Запускаем поток
        // Ждем завершения потока, чтобы убедиться, что все сообщения были отправлены
        try {
            clientHandler.join();  // Ожидаем завершения работы потока
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Проверяем, что отправлялись ожидаемые сообщения клиенту
        verify(mockWriter).println("Добро пожаловать в чат! Введите '1' для входа или '2' для регистрации:");
        verify(mockWriter).println("Введите логин:");
        verify(mockWriter).println("Введите пароль:");
        verify(mockWriter).println("Вход успешен! Добро пожаловать, user");
        verify(mockWriter).println("Server: user присоединился к чату");
        verify(mockWriter).println("user: exit");

        assertTrue(ChatServer.getClientWriters().containsKey("user"));
    }

    @Test
    public void testClientRegistration() throws IOException {
        // Подготавливаем данные для регистрации
        when(mockReader.readLine())
                .thenReturn("2")  // Выбор регистрации
                .thenReturn("newuser")  // Логин
                .thenReturn("newpassword123")  // Пароль
                .thenReturn("exit");  // Завершение соединения

        // Передаем моки в конструктор ClientHandler
        ChatServer.ClientHandler clientHandler = new ChatServer.ClientHandler(mockSocket, mockReader, mockWriter);
        clientHandler.start(); // Запускаем поток
        // Ждем завершения потока, чтобы убедиться, что все сообщения были отправлены
        try {
            clientHandler.join();  // Ожидаем завершения работы потока
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Проверяем, что отправлялись ожидаемые сообщения клиенту
        verify(mockWriter).println("Добро пожаловать в чат! Введите '1' для входа или '2' для регистрации:");
        verify(mockWriter).println("Введите логин:");
        verify(mockWriter).println("Введите пароль:");
        verify(mockWriter).println("Регистрация успешна! Теперь вы можете войти.");
        verify(mockWriter).println("Server: newuser присоединился к чату");
        verify(mockWriter).println("newuser: exit");

        assertTrue(ChatServer.getUsers().containsKey("newuser"));
    }
}