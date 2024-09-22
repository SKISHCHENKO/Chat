package chat;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private final String logFilePath;
    private final String logFilePathError;
    private final Object lock = new Object();
    private final Object lockError = new Object();

    private static final String FILE_LOG = "src/main/resources/log.txt";
    private static final String FILE_LOG_ERROR = "src/main/resources/error_log.txt";

    private static Log INSTANCE = null;


    private Log(String logFilePath, String logFilePathError) {
        this.logFilePath = logFilePath;
        this.logFilePathError = logFilePathError;
    }

    public static Log getInstance() {
        if (INSTANCE == null) {
            synchronized (Log.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Log(FILE_LOG, FILE_LOG_ERROR);
                }
            }
        }
        return INSTANCE;
    }

    //Логирование
    public void log(String msg) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String logMessage = "[" + dtf.format(now) + "] <" + msg + ">";
        logToFile(logMessage);
    }

    // Запись лога в файл
    public void logToFile(String msg) {
        synchronized (lock) { // Синхронизация для обеспечения потокобезопасности
            try (PrintWriter out = new PrintWriter(new FileWriter(logFilePath, true))) {
                out.println(msg);
            } catch (IOException e) {
                logError("Ошибка при записи лога: " + e.getMessage());
            }
        }
    }

    // Логирование ошибок в отдельный файл
    public void logError(String errorMsg) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String errorLogMessage = "[" + dtf.format(now) + "] <ERROR> " + errorMsg + "\n";
        synchronized (lockError) { // Синхронизация для обеспечения потокобезопасности
            try (FileWriter errorWriter = new FileWriter(logFilePathError, true)) {
                errorWriter.write(errorLogMessage);
            } catch (IOException e) {
                System.err.println("Ошибка при записи ошибки в лог: " + e.getMessage());
            }
        }
    }
}
