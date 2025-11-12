import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

/**
 * 🔑 AuthManager
 * Handles user registration and login.
 * Stores user data in a serialized HashMap.
 */
public class AuthManager {
    // Path to the file where user data is stored
    private static final String USER_FILE = "locker_data/users.dat";
    private final Map<String, String> userDatabase;

    /**
     * Constructor. Loads existing users from the data file.
     */
    @SuppressWarnings("unchecked")
    public AuthManager() {
        this.userDatabase = new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USER_FILE))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                userDatabase.putAll((Map<String, String>) obj);
            }
        } catch (FileNotFoundException e) {
            // File doesn't exist yet, which is fine. It will be created on first registration.
            System.out.println("Notice: User database not found, creating a new one.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading user database: " + e.getMessage());
        }
    }

    /**
     * Saves the current user database to the file.
     */
    private void saveUsers() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_FILE))) {
            oos.writeObject(userDatabase);
        }
    }

    /**
     * Registers a new user.
     * @param username The desired username.
     * @param password The plain-text password.
     * @return true if registration is successful, false if username already exists.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws IOException
     */
    public boolean register(String username, String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        if (userDatabase.containsKey(username)) {
            return false; // Username already exists
        }
        // Hash the password using the utility class
        String hashedPassword = PasswordUtils.hashPassword(password);
        userDatabase.put(username, hashedPassword);
        saveUsers();
        return true;
    }

    /**
     * Logs in a user.
     * @param username The username.
     * @param password The plain-text password.
     * @return true if login is successful, false otherwise.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public boolean login(String username, String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        String storedHashedPassword = userDatabase.get(username);
        if (storedHashedPassword == null) {
            return false; // User not found
        }
        // Check the password using the utility class
        return PasswordUtils.checkPassword(password, storedHashedPassword);
    }
}
