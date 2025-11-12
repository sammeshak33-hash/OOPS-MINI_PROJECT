import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * 🔒 CryptoManager
 * Handles file encryption and decryption using AES-256 GCM.
 * All methods are static.
 */
public class CryptoManager {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final int SALT_LENGTH = 16;
    private static final int PBKDF2_ITERATIONS = 65536; // Same as password hashing

    /**
     * Derives a 256-bit AES key from a user's password and a salt.
     * Uses PBKDF2, the same as password hashing, for key stretching.
     */
    private static SecretKey getAESKeyFromPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Encrypts a file.
     * Prepends the file with [salt][iv] for decryption.
     * File Format: [16-byte salt][12-byte iv][encrypted data]
     * @param sourcePath The path to the plain-text file.
     * @param destPath   The path to store the encrypted file.
     * @param password   The user's password to derive the key.
     */
    public static void encrypt(String sourcePath, String destPath, String password)
            throws GeneralSecurityException, IOException, InvalidKeySpecException {

        // 1. Generate random salt and IV
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // 2. Derive key from password and salt
        SecretKey aesKey = getAESKeyFromPassword(password, salt);

        // 3. Initialize Cipher
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

        // 4. Write salt, IV, and encrypted data to file
        try (FileOutputStream fos = new FileOutputStream(destPath);
             FileInputStream fis = new FileInputStream(sourcePath)) {

            // Write salt and IV to the beginning of the file
            fos.write(salt);
            fos.write(iv);

            // Encrypt the rest of the file
            try (CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    /**
     * Decrypts a file.
     * Reads the [salt][iv] from the beginning of the file.
     * @param sourcePath The path to the encrypted file.
     * @param destPath   The path to store the decrypted file.
     * @param password   The user's password to derive the key.
     */
    public static void decrypt(String sourcePath, String destPath, String password)
            throws GeneralSecurityException, IOException, InvalidKeySpecException {

        try (FileInputStream fis = new FileInputStream(sourcePath)) {
            // 1. Read salt and IV
            byte[] salt = new byte[SALT_LENGTH];
            if (fis.read(salt) != SALT_LENGTH)
                throw new IOException("Invalid file: missing salt.");

            byte[] iv = new byte[GCM_IV_LENGTH];
            if (fis.read(iv) != GCM_IV_LENGTH)
                throw new IOException("Invalid file: missing IV.");

            // 2. Derive key from password and salt
            SecretKey aesKey = getAESKeyFromPassword(password, salt);

            // 3. Initialize Cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

            // 4. Read and decrypt the rest of the file
            // The inner try-with-resources is now wrapped in a try-catch
            // to catch the IOException that wraps the AEADBadTagException.
            try (FileOutputStream fos = new FileOutputStream(destPath);
                 CipherInputStream cis = new CipherInputStream(fis, cipher)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                // Implicit cis.close() happens here, which triggers the tag check
            } catch (IOException e) {
                // Check if the cause of the IOException is a bad tag
                if (e.getCause() instanceof AEADBadTagException) {
                    // This is the error for a bad password or corrupt file!
                    throw new GeneralSecurityException("Decryption failed. Wrong password or corrupted file.", e.getCause());
                } else {
                    // This was a different, standard I/O error
                    throw e;
                }
            }
        }
    }
}
