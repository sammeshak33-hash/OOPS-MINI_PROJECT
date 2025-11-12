import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main application class.
 * This is the new entry point for the GUI-based Digital File Locker.
 */
public class Main {

    // Directory constants from the original console app
    public static final String DATA_DIR = "locker_data";
    public static final String FILES_DIR = DATA_DIR + "/files";
    public static final String DOWNLOADS_DIR = "locker_downloads";

    public static void main(String[] args) {
        // 1. Initialize directories (same as before)
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            Files.createDirectories(Paths.get(FILES_DIR));
            Files.createDirectories(Paths.get(DOWNLOADS_DIR));
        } catch (IOException e) {
            System.err.println("Fatal Error: Could not create necessary directories. Exiting.");
            e.printStackTrace();
            return;
        }

        // 2. Launch the GUI on the Event Dispatch Thread (EDT)
        // This is the standard, thread-safe way to start a Swing application.
        SwingUtilities.invokeLater(() -> {
            LoginWindow loginWindow = new LoginWindow();
            loginWindow.setVisible(true);
        });
    }
}
