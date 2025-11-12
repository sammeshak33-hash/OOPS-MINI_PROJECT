import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Set;

/**
 * Digital File Locker (Main Application)
 * This class contains the main method and the console menu logic.
 * It coordinates the AuthManager and LockerManager to run the application.
 */
public class DigitalFileLocker {

    // --- Directory Constants ---
    // These directories will be created in the same folder where the app is run.
    public static final String DATA_DIR = "locker_data";
    public static final String FILES_DIR = DATA_DIR + "/files";
    public static final String DOWNLOADS_DIR = "locker_downloads";

    private final AuthManager authManager;
    private final LockerManager lockerManager;
    private final Scanner scanner;
    private String currentUsername = null;

    /**
     * Main entry point of the application.
     */
    public static void main(String[] args) {
        // Create necessary directories
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            Files.createDirectories(Paths.get(FILES_DIR));
            Files.createDirectories(Paths.get(DOWNLOADS_DIR));
            System.out.println("Directories initialized.");
        } catch (IOException e) {
            System.err.println("Fatal Error: Could not create necessary directories. Exiting.");
            e.printStackTrace();
            return;
        }

        // Start the application
        DigitalFileLocker app = new DigitalFileLocker();
        app.runMainMenu();
    }

    /**
     * Constructor for the main application.
     * Initializes managers and the scanner.
     */
    public DigitalFileLocker() {
        this.authManager = new AuthManager();
        this.lockerManager = new LockerManager();
        this.scanner = new Scanner(System.in);
    }

    /**
     * Runs the main (logged-out) menu loop.
     */
    public void runMainMenu() {
        while (true) {
            System.out.println("\n--- 🔐 Digital File Locker ---");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    handleLogin();
                    break;
                case "2":
                    handleRegister();
                    break;
                case "3":
                    System.out.println("Thank you for using the Digital File Locker. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    /**
     * Handles the user registration process.
     */
    private void handleRegister() {
        System.out.print("Enter new username: ");
        String username = scanner.nextLine();
        System.out.print("Enter new password: ");
        String password = readPassword();

        try {
            if (authManager.register(username, password)) {
                System.out.println("✅ Registration successful! Please login.");
            } else {
                System.out.println("❌ Error: Username already exists.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error during registration: " + e.getMessage());
        }
    }

    /**
     * Handles the user login process.
     * If successful, sets the current user and shows the user menu.
     */
    private void handleLogin() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = readPassword();

        try {
            if (authManager.login(username, password)) {
                this.currentUsername = username;
                System.out.println("\n✅ Login successful. Welcome, " + username + "!");
                // Pass password to user menu for encryption/decryption
                runUserMenu(password);
            } else {
                System.out.println("❌ Login failed. Invalid username or password.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error during login: " + e.getMessage());
        }
    }

    /**
     * Runs the logged-in user menu loop.
     * @param password The user's plain-text password, used to derive the
     * encryption key.
     */
    private void runUserMenu(String password) {
        // Note: The password string lives in memory while the user is logged in.
        // In a high-security app, you'd use char[] and clear it,
        // or re-prompt the user for it for each operation.
        try {
            while (true) {
                System.out.println("\n--- 🗄️ [" + currentUsername + "'s Locker] ---");
                System.out.println("1. Upload File");
                System.out.println("2. Download File");
                System.out.println("3. List My Files");
                System.out.println("4. Delete File");
                System.out.println("5. Logout");
                System.out.print("Choose an option: ");
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        handleUpload(password);
                        break;
                    case "2":
                        handleDownload(password);
                        break;
                    case "3":
                        handleListFiles();
                        break;
                    case "4":
                        handleDelete();
                        break;
                    case "5":
                        System.out.println("Logging out...");
                        this.currentUsername = null;
                        return; // Exit user menu, go back to main menu
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }
        } finally {
            // Nullify reference to the password when user logs out
            password = null;
        }
    }

    /**
     * Handles uploading a file to the locker.
     * @param password The user's password for encryption.
     */
    private void handleUpload(String password) {
        System.out.print("Enter the full path of the file to upload: ");
        String sourcePath = scanner.nextLine();
        File file = new File(sourcePath);

        if (!file.exists() || file.isDirectory()) {
            System.out.println("❌ Error: File does not exist or is a directory.");
            return;
        }

        try {
            lockerManager.uploadFile(currentUsername, password, sourcePath);
            System.out.println("✅ File '" + file.getName() + "' uploaded successfully.");
        } catch (Exception e) {
            System.err.println("❌ Error uploading file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles downloading a file from the locker.
     * @param password The user's password for decryption.
     */
    private void handleDownload(String password) {
        System.out.print("Enter the name of the file to download: ");
        String originalName = scanner.nextLine();

        try {
            // All downloads go to the 'locker_downloads' folder
            String destPath = DOWNLOADS_DIR + "/" + originalName;
            if (lockerManager.downloadFile(currentUsername, password, originalName, destPath)) {
                System.out.println("✅ File '" + originalName + "' downloaded successfully to '" + destPath + "'.");
            } else {
                System.out.println("❌ Error: File not found in your locker.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error downloading file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles listing all files for the current user.
     */
    private void handleListFiles() {
        try {
            Set<String> files = lockerManager.listFiles(currentUsername);
            if (files.isEmpty()) {
                System.out.println("Your locker is empty.");
            } else {
                System.out.println("Your files:");
                files.forEach(file -> System.out.println("- " + file));
            }
        } catch (Exception e) {
            System.err.println("❌ Error listing files: " + e.getMessage());
        }
    }

    /**
     * Handles deleting a file from the locker.
     */
    private void handleDelete() {
        System.out.print("Enter the name of the file to delete: ");
        String originalName = scanner.nextLine();

        try {
            if (lockerManager.deleteFile(currentUsername, originalName)) {
                System.out.println("✅ File '" + originalName + "' deleted successfully.");
            } else {
                System.out.println("❌ Error: File not found in your locker.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error deleting file: " + e.getMessage());
        }
    }

    /**
     * Reads password from console without echoing.
     * Tries to use Console, falls back to standard input for IDEs.
     * @return The password as a String.
     */
    private String readPassword() {
        Console console = System.console();
        if (console != null) {
            // Ideal method: Reads password without echoing to screen
            char[] passwordChars = console.readPassword();
            return new String(passwordChars);
        } else {
            // Fallback for IDEs (like Eclipse, IntelliJ)
            // Note: This will echo the password.
            System.out.print("(Warning: IDE detected, password will be visible) ");
            return scanner.nextLine();
        }
    }
}