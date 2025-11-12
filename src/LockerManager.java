import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🗄️ LockerManager
 * Manages file operations (upload, download, delete, list).
 * Stores file metadata in a serialized HashMap.
 */
public class LockerManager {
    // Maps: username -> { original_filename -> encrypted_filename_UUID }
    private final Map<String, Map<String, String>> fileDatabase;
    private static final String METADATA_FILE = "locker_data/file_metadata.dat";
    private static final String FILES_DIR = "locker_data/files";


    /**
     * Constructor. Loads existing file metadata.
     */
    @SuppressWarnings("unchecked")
    public LockerManager() {
        this.fileDatabase = new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(METADATA_FILE))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                fileDatabase.putAll((Map<String, Map<String, String>>) obj);
            }
        } catch (FileNotFoundException e) {
            // Metadata file doesn't exist yet, which is fine.
            System.out.println("Notice: File metadata not found, creating a new one.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading file metadata: " + e.getMessage());
        }
    }

    /**
     * Saves the current file metadata to the file.
     */
    private void saveMetadata() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(METADATA_FILE))) {
            oos.writeObject(fileDatabase);
        }
    }

    /**
     * Uploads a file: encrypts it, saves it, and updates metadata.
     * @param username   The user uploading the file.
     * @param password   The user's password for encryption.
     * @param sourcePath The path to the original file.
     */
    public void uploadFile(String username, String password, String sourcePath)
            throws GeneralSecurityException, IOException, InvalidKeySpecException {
        File sourceFile = new File(sourcePath);
        String originalName = sourceFile.getName();

        // Generate a unique, obfuscated name for the stored file
        String encryptedName = UUID.randomUUID().toString() + ".loc";
        String encryptedPath = FILES_DIR + "/" + encryptedName;

        // 1. Encrypt and save the file using the utility class
        CryptoManager.encrypt(sourcePath, encryptedPath, password);

        // 2. Update and save metadata
        fileDatabase.putIfAbsent(username, new HashMap<>());
        fileDatabase.get(username).put(originalName, encryptedName);
        saveMetadata();
    }

    /**
     * Downloads a file: finds it, decrypts it, and saves it.
     * @param username     The user downloading the file.
     * @param password     The user's password for decryption.
     * @param originalName The name of the file to download.
     * @param destPath     The path to save the decrypted file.
     * @return true if successful, false if file not found.
     */
    public boolean downloadFile(String username, String password, String originalName, String destPath)
            throws GeneralSecurityException, IOException, InvalidKeySpecException {
        Map<String, String> userFiles = fileDatabase.get(username);
        if (userFiles == null || !userFiles.containsKey(originalName)) {
            return false; // File not found
        }

        String encryptedName = userFiles.get(originalName);
        String encryptedPath = FILES_DIR + "/" + encryptedName;

        if (!Files.exists(Paths.get(encryptedPath))) {
            // Metadata exists but file is missing (corruption)
            throw new IOException("Metadata inconsistency: File " + encryptedName + " not found on disk.");
        }

        // Decrypt and save the file using the utility class
        CryptoManager.decrypt(encryptedPath, destPath, password);
        return true;
    }

    /**
     * Deletes a file: removes the encrypted file and its metadata.
     * @param username     The user deleting the file.
     * @param originalName The name of the file to delete.
     * @return true if successful, false if file not found.
     */
    public boolean deleteFile(String username, String originalName) throws IOException {
        Map<String, String> userFiles = fileDatabase.get(username);
        if (userFiles == null || !userFiles.containsKey(originalName)) {
            return false; // File not found
        }

        // 1. Delete the physical file
        String encryptedName = userFiles.get(originalName);
        String encryptedPath = FILES_DIR + "/" + encryptedName;

        try {
            Files.deleteIfExists(Paths.get(encryptedPath));
        } catch (IOException e) {
            System.err.println("Warning: Could not delete file from disk, but will remove metadata: " + e.getMessage());
        }

        // 2. Remove from metadata and save
        userFiles.remove(originalName);
        saveMetadata();
        return true;
    }

    /**
     * Lists all files for a given user.
     * @param username The user.
     * @return A Set of original file names.
     */
    public Set<String> listFiles(String username) {
        Map<String, String> userFiles = fileDatabase.get(username);
        if (userFiles == null) {
            return Collections.emptySet();
        }
        // Return a copy to prevent modification
        return userFiles.keySet().stream().collect(Collectors.toSet());
    }
}

