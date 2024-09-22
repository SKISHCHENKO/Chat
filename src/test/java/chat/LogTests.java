package chat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

public class LogTests {
    private static final String FILE_LOG = "src/main/resources/log.txt";
    @Test
    public void testLog() throws IOException {
        String message = "Test message";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String expectedLogMessage = "[" + dtf.format(now) + "] <" + message + ">\n";

        Log logger = Log.getInstance();
        logger.log(message);

        Path logFilePath = Paths.get(FILE_LOG);
        String fileContent = Files.readString(logFilePath);

        assertTrue(fileContent.contains(expectedLogMessage));
    }

    @Test
    public void testLogToFileWithException() throws IOException {
        Log logger = Log.getInstance();

        FileWriter mockWriter = Mockito.mock(FileWriter.class);
        doThrow(new IOException("Test exception")).when(mockWriter).write(anyString());

        assertDoesNotThrow(() -> logger.logToFile("Error message"));
    }
}
