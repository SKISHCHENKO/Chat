package chat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static chat.ChatClient.loadSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatClientTests {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 8080;

    @Test
    public void testLoadSettings_success() throws IOException {
        Path tempFile = Files.createTempFile("settings", ".txt");
        Files.write(tempFile, "localhost:8080".getBytes());

        loadSettings(tempFile.toString());

        // Проверяем, что настройки загружены правильно
        assertEquals("localhost", SERVER_ADDRESS);
        assertEquals(8080, PORT);

        Files.delete(tempFile);
    }

    @Test
    public void testLoadSettings_invalidData() throws IOException {
        Path tempFile = Files.createTempFile("settings", ".txt");
        Files.write(tempFile, "invalid_data".getBytes());

        loadSettings(tempFile.toString());

        // Проверяем, что серверный адрес и порт не изменились
        assertEquals("localhost", SERVER_ADDRESS);
        assertEquals(8080, PORT);

        Files.delete(tempFile);
    }

    @Test
    public void testLoadSettings_fileNotFound() {
        // Загружаем несуществующий файл
        loadSettings("non_existent_file.txt");

        // Проверяем, что серверный адрес и порт не изменились
        assertEquals("localhost", SERVER_ADDRESS);
        assertEquals(8080, PORT);
    }
}
