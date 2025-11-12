import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * Creates the Login and Registration GUI.
 * This is the first window the user sees.
 */
public class LoginWindow extends JFrame {

    private final JTextField tfUsername;
    private final JPasswordField pfPassword;
    private final JButton btnLogin;
    private final JButton btnSignUp;
    private final AuthManager authManager;

    public LoginWindow() {
        // Initialize the AuthManager
        this.authManager = new AuthManager();

        // --- Frame Setup ---
        setTitle("Digital Locker - Login");
        setSize(400, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window
        setLayout(new BorderLayout(10, 10));

        // --- Components ---
        tfUsername = new JTextField();
        pfPassword = new JPasswordField();
        btnLogin = new JButton("Login");
        btnSignUp = new JButton("Sign Up");

        // --- Layout ---
        // Panel for labels and fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        formPanel.add(tfUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        formPanel.add(pfPassword, gbc);

        // Panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnSignUp);

        // Add panels to frame
        add(new JLabel("Welcome!", SwingConstants.CENTER), BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Event Listeners ---
        btnLogin.addActionListener(e -> onLogin());
        btnSignUp.addActionListener(e -> onSignUp());
    }

    /**
     * Handles the login button click.
     */
    private void onLogin() {
        String username = tfUsername.getText();
        String password = new String(pfPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Username and password cannot be empty.",
                    "Login Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (authManager.login(username, password)) {
                // Login successful!
                // Close this login window
                dispose();
                // Open the main file locker window
                FileLockerWindow fileLocker = new FileLockerWindow(username, password);
                fileLocker.setVisible(true);

            } else {
                // Login failed
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            JOptionPane.showMessageDialog(this,
                    "An error occurred during login: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handles the sign-up button click.
     */
    private void onSignUp() {
        String username = tfUsername.getText();
        String password = new String(pfPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Username and password cannot be empty.",
                    "Sign Up Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (authManager.register(username, password)) {
                // Registration successful
                JOptionPane.showMessageDialog(this,
                        "Registration successful! You can now log in.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                // Clear fields
                tfUsername.setText("");
                pfPassword.setText("");
            } else {
                // Username already exists
                JOptionPane.showMessageDialog(this,
                        "Username already exists. Please choose another.",
                        "Sign Up Failed", JOptionPane.WARNING_MESSAGE);
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "An error occurred during registration: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}