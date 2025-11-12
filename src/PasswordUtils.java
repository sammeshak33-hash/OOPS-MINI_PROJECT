import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 🛡️ PasswordUtils
 * Utility class for hashing and verifying passwords using PBKDF2.
 * All methods are static.
 */
public class PasswordUtils {
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_SIZE = 16;

    /**
     * Hashes a password with a random salt using PBKDF2.
     * @param password The plain-text password.
     * @return A string containing "salt:hash", both Base64 encoded.
     */
    public static String hashPassword(String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_SIZE];
        random.nextBytes(salt);

        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

        // Store as "salt:hash"
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Checks a plain-text password against a stored hashed password.
     * @param password           The plain-text password to check.
     * @param storedHashedPassword The "salt:hash" string from the database.
     * @return true if the password matches, false otherwise.
     */
    public static boolean checkPassword(String password, String storedHashedPassword)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        String[] parts = storedHashedPassword.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid stored password format.");
        }
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] hash = Base64.getDecoder().decode(parts[1]);

        byte[] testHash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

        // Time-constant comparison to prevent timing attacks
        return MessageDigest.isEqual(hash, testHash);
    }

    /**
     * Core PBKDF2 hashing function.
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }
}