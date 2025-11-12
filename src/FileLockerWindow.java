import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * The main application window for managing files.
 * Displays a list of files and provides buttons to upload, download, and delete.
 */
public class FileLockerWindow extends JFrame {

    private final String currentUsername;
    private final String currentUserPassword;
    private final LockerManager lockerManager;

    private final JTable fileTable;
    private final DefaultTableModel tableModel;
    private final JButton btnUpload;
    private final JButton btnDownload;
    private final JButton btnDelete;

    public FileLockerWindow(String username, String password) {
        this.currentUsername = username;
        this.currentUserPassword = password; // Stored for encryption/decryption
        this.lockerManager = new LockerManager();

        // --- Frame Setup ---
        setTitle("Digital File Locker - Welcome, " + currentUsername);
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- Components ---
        // Table
        String[] columnNames = {"File Name"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        fileTable = new JTable(tableModel);
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(fileTable); // Add table to scroll pane

        // Buttons
        btnUpload = new JButton("Upload File");
        btnDownload = new JButton("Download File");
        btnDelete = new JButton("Delete File");

        // --- Layout ---
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(btnUpload);
        buttonPanel.add(btnDownload);
        buttonPanel.add(btnDelete);

        setLayout(new BorderLayout(10, 10));
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Event Listeners ---
        btnUpload.addActionListener(e -> onUpload());
        btnDownload.addActionListener(e -> onDownload());
        btnDelete.addActionListener(e -> onDelete());

        // Initial file load
        loadUserFiles();
    }

    /**
     * Loads/Refreshes the user's file list into the JTable.
     */
    private void loadUserFiles() {
        // Clear existing table data
        tableModel.setRowCount(0);
        try {
            Set<String> files = lockerManager.listFiles(currentUsername);
            for (String fileName : files) {
                tableModel.addRow(new Object[]{fileName});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading files: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handles the Upload button click.
     * Uses JFileChooser and performs the upload in a background thread.
     */
    private void onUpload() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String sourcePath = selectedFile.getAbsolutePath();

            // Disable buttons during operation
            toggleButtons(false);

            // Use SwingWorker to perform file I/O in the background
            // This prevents the GUI from freezing
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    // This runs on a background thread
                    lockerManager.uploadFile(currentUsername, currentUserPassword, sourcePath);
                    return selectedFile.getName();
                }

                @Override
                protected void done() {
                    // This runs on the EDT (Event Dispatch Thread) after doInBackground
                    // is finished
                    try {
                        String fileName = get(); // Get the result from doInBackground
                        JOptionPane.showMessageDialog(FileLockerWindow.this,
                                "File '" + fileName + "' uploaded successfully!",
                                "Upload Success", JOptionPane.INFORMATION_MESSAGE);
                        loadUserFiles(); // Refresh the file list
                    } catch (InterruptedException | ExecutionException e) {
                        JOptionPane.showMessageDialog(FileLockerWindow.this,
                                "Error uploading file: " + e.getCause().getMessage(),
                                "Upload Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        toggleButtons(true); // Re-enable buttons
                    }
                }
            }.execute();
        }
    }

    /**
     * Handles the Download button click.
     * Performs the download in a background thread.
     */
    private void onDownload() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a file to download.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
        String destPath = Main.DOWNLOADS_DIR + "/" + fileName;

        toggleButtons(false);

        // Use SwingWorker for background operation
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                lockerManager.downloadFile(currentUsername, currentUserPassword, fileName, destPath);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions from doInBackground
                    JOptionPane.showMessageDialog(FileLockerWindow.this,
                            "File '" + fileName + "' downloaded to '" + destPath + "'",
                            "Download Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(FileLockerWindow.this,
                            "Error downloading file: " + e.getCause().getMessage(),
                            "Download Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    toggleButtons(true);
                }
            }
        }.execute();
    }

    /**
     * Handles the Delete button click.
     * Performs the deletion in a background thread.
     */
    private void onDelete() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a file to delete.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fileName = (String) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete '" + fileName + "'?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            toggleButtons(false);

            // Use SwingWorker for background operation
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    lockerManager.deleteFile(currentUsername, fileName);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // Check for exceptions
                        JOptionPane.showMessageDialog(FileLockerWindow.this,
                                "File '" + fileName + "' deleted successfully.",
                                "Delete Success", JOptionPane.INFORMATION_MESSAGE);
                        loadUserFiles(); // Refresh the list
                    } catch (InterruptedException | ExecutionException e) {
                        JOptionPane.showMessageDialog(FileLockerWindow.this,
                                "Error deleting file: " + e.getCause().getMessage(),
                                "Delete Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        toggleButtons(true);
                    }
                }
            }.execute();
        }
    }

    /**
     * Helper method to enable/disable buttons during long operations.
     * @param enabled true to enable, false to disable
     */
    private void toggleButtons(boolean enabled) {
        btnUpload.setEnabled(enabled);
        btnDownload.setEnabled(enabled);
        btnDelete.setEnabled(enabled);
    }
}
