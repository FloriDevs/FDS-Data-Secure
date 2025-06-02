import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class SecureFileDeleter extends JFrame {
    private JList<File> fileList;
    private DefaultListModel<File> listModel;
    private JProgressBar progressBar;
    private JTextArea logArea;
    private JComboBox<DeletionMethod> methodComboBox;
    private JButton addFileButton, addFolderButton, removeButton, clearButton, deleteButton;
    private JCheckBox recursiveCheckBox;
    
    // Deletion methods
    enum DeletionMethod {
        SIMPLE("Simple Delete"),
        DOD_3_PASS("DoD 3-Pass (3x overwrite)"),
        DOD_7_PASS("DoD 7-Pass (7x overwrite)"),
        GUTMANN("Gutmann Method (35x overwrite)"),
        RANDOM_3_PASS("Random 3-Pass"),
        ZERO_FILL("Zero Fill Overwrite");
        
        private final String description;
        
        DeletionMethod(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
    
    public SecureFileDeleter() {
        initializeGUI();
    }
    
    private void initializeGUI() {
        setTitle("Secure File Deleter v1.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // File panel
        JPanel filePanel = createFilePanel();
        mainPanel.add(filePanel, BorderLayout.CENTER);
        
        // Options panel
        JPanel optionsPanel = createOptionsPanel();
        mainPanel.add(optionsPanel, BorderLayout.SOUTH);
        
        // Log panel
        JPanel logPanel = createLogPanel();
        
        // Split pane for main area and log
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPanel, logPanel);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.7);
        
        add(splitPane, BorderLayout.CENTER);
        
        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(600, 500));
    }
    
    private JPanel createFilePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder("Files to Delete"));
        
        // File list
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setCellRenderer(new FileListCellRenderer());
        
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        addFileButton = new JButton("Add Files");
        addFileButton.addActionListener(e -> addFiles());
        
        addFolderButton = new JButton("Add Folder");
        addFolderButton.addActionListener(e -> addFolder());
        
        removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeSelected());
        
        clearButton = new JButton("Clear All");
        clearButton.addActionListener(e -> clearAll());
        
        buttonPanel.add(addFileButton);
        buttonPanel.add(addFolderButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Deletion Options"));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Deletion method
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 10);
        panel.add(new JLabel("Deletion Method:"), gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        methodComboBox = new JComboBox<>(DeletionMethod.values());
        methodComboBox.setSelectedItem(DeletionMethod.DOD_3_PASS);
        panel.add(methodComboBox, gbc);
        
        // Recursive option
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        recursiveCheckBox = new JCheckBox("Delete folders recursively");
        recursiveCheckBox.setSelected(true);
        panel.add(recursiveCheckBox, gbc);
        
        // Progress bar
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        panel.add(progressBar, gbc);
        
        // Delete button
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        deleteButton = new JButton("SECURE DELETE");
        deleteButton.setBackground(Color.RED);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFont(deleteButton.getFont().deriveFont(Font.BOLD, 14f));
        deleteButton.addActionListener(e -> confirmAndDelete());
        panel.add(deleteButton, gbc);
        
        return panel;
    }
    
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Log"));
        
        logArea = new JTextArea(8, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.addActionListener(e -> logArea.setText(""));
        
        JPanel logButtonPanel = new JPanel(new FlowLayout());
        logButtonPanel.add(clearLogButton);
        panel.add(logButtonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            for (File file : files) {
                if (!listModel.contains(file)) {
                    listModel.addElement(file);
                }
            }
            log("Added: " + files.length + " file(s)");
        }
    }
    
    private void addFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            if (!listModel.contains(folder)) {
                listModel.addElement(folder);
                log("Folder added: " + folder.getName());
            }
        }
    }
    
    private void removeSelected() {
        int[] indices = fileList.getSelectedIndices();
        for (int i = indices.length - 1; i >= 0; i--) {
            listModel.removeElementAt(indices[i]);
        }
    }
    
    private void clearAll() {
        listModel.clear();
        log("All entries removed");
    }
    
    private void confirmAndDelete() {
        if (listModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No files selected for deletion!");
            return;
        }
        
        String message = "WARNING: This action will PERMANENTLY delete the selected files!\n\n" +
                        "Number of entries: " + listModel.getSize() + "\n" +
                        "Deletion method: " + methodComboBox.getSelectedItem() + "\n\n" +
                        "Are you sure you want to continue?";
        
        int result = JOptionPane.showConfirmDialog(this, message, "Confirmation Required", 
                                                  JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            // Second confirmation for critical action
            String confirmText = JOptionPane.showInputDialog(this, 
                "Type 'DELETE' to confirm:");
            
            if ("DELETE".equals(confirmText)) {
                performDeletion();
            } else {
                log("Deletion cancelled - incorrect confirmation");
            }
        }
    }
    
    private void performDeletion() {
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                deleteButton.setEnabled(false);
                DeletionMethod method = (DeletionMethod) methodComboBox.getSelectedItem();
                
                List<File> allFiles = collectAllFiles();
                progressBar.setMaximum(allFiles.size());
                progressBar.setValue(0);
                
                publish("=== Deletion process started ===");
                publish("Method: " + method);
                publish("Total files: " + allFiles.size());
                
                int deleted = 0;
                for (int i = 0; i < allFiles.size(); i++) {
                    File file = allFiles.get(i);
                    try {
                        if (secureDelete(file, method)) {
                            deleted++;
                            publish("✓ Deleted: " + file.getAbsolutePath());
                        } else {
                            publish("✗ Error deleting: " + file.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        publish("✗ Exception with " + file.getName() + ": " + e.getMessage());
                    }
                    progressBar.setValue(i + 1);
                }
                
                publish("=== Deletion process completed ===");
                publish("Successfully deleted: " + deleted + "/" + allFiles.size());
                
                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    log(message);
                }
            }
            
            @Override
            protected void done() {
                deleteButton.setEnabled(true);
                progressBar.setValue(0);
                listModel.clear();
            }
        };
        
        worker.execute();
    }
    
    private List<File> collectAllFiles() {
        List<File> allFiles = new ArrayList<>();
        
        for (int i = 0; i < listModel.getSize(); i++) {
            File item = listModel.getElementAt(i);
            if (item.isFile()) {
                allFiles.add(item);
            } else if (item.isDirectory() && recursiveCheckBox.isSelected()) {
                collectFilesRecursively(item, allFiles);
            }
        }
        
        return allFiles;
    }
    
    private void collectFilesRecursively(File dir, List<File> fileList) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file);
                } else if (file.isDirectory()) {
                    collectFilesRecursively(file, fileList);
                }
            }
        }
    }
    
    private boolean secureDelete(File file, DeletionMethod method) throws IOException {
        if (!file.exists()) return false;
        
        switch (method) {
            case SIMPLE:
                return file.delete();
            case DOD_3_PASS:
                return overwriteAndDelete(file, 3, getDoDPatterns());
            case DOD_7_PASS:
                return overwriteAndDelete(file, 7, getDoDPatterns());
            case GUTMANN:
                return overwriteAndDelete(file, 35, getGutmannPatterns());
            case RANDOM_3_PASS:
                return overwriteAndDelete(file, 3, getRandomPatterns(3));
            case ZERO_FILL:
                return overwriteAndDelete(file, 1, new byte[][]{{0}});
            default:
                return file.delete();
        }
    }
    
    private boolean overwriteAndDelete(File file, int passes, byte[][] patterns) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rws")) {
            long fileSize = raf.length();
            
            for (int pass = 0; pass < passes; pass++) {
                byte[] pattern = patterns[pass % patterns.length];
                raf.seek(0);
                
                long remaining = fileSize;
                while (remaining > 0) {
                    int writeSize = (int) Math.min(pattern.length, remaining);
                    raf.write(pattern, 0, writeSize);
                    remaining -= writeSize;
                }
                raf.getFD().sync(); // Force write operation
            }
        }
        
        // Rename file and delete
        File tempFile = new File(file.getParent(), "deleted_" + System.currentTimeMillis());
        file.renameTo(tempFile);
        return tempFile.delete();
    }
    
    private byte[][] getDoDPatterns() {
        return new byte[][] {
            {(byte)0x00}, // Zeros
            {(byte)0xFF}, // Ones
            {(byte)0x00}  // Zeros
        };
    }
    
    private byte[][] getGutmannPatterns() {
        // Simplified Gutmann patterns (only some of the 35 passes)
        return new byte[][] {
            {(byte)0x55}, {(byte)0xAA}, {(byte)0x92, (byte)0x49, (byte)0x24},
            {(byte)0x49, (byte)0x24, (byte)0x92}, {(byte)0x24, (byte)0x92, (byte)0x49},
            {(byte)0x00}, {(byte)0x11}, {(byte)0x22}, {(byte)0x33}, {(byte)0x44},
            {(byte)0x55}, {(byte)0x66}, {(byte)0x77}, {(byte)0x88}, {(byte)0x99},
            {(byte)0xAA}, {(byte)0xBB}, {(byte)0xCC}, {(byte)0xDD}, {(byte)0xEE},
            {(byte)0xFF}
        };
    }
    
    private byte[][] getRandomPatterns(int count) {
        SecureRandom random = new SecureRandom();
        byte[][] patterns = new byte[count][];
        
        for (int i = 0; i < count; i++) {
            patterns[i] = new byte[1024]; // 1KB random data
            random.nextBytes(patterns[i]);
        }
        
        return patterns;
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + java.time.LocalTime.now().toString().substring(0, 8) + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    // Custom Cell Renderer for the file list
    private static class FileListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof File) {
                File file = (File) value;
                String text = file.getName();
                if (file.isDirectory()) {
                    text += " [FOLDER]";
                    setIcon(UIManager.getIcon("FileView.directoryIcon"));
                } else {
                    text += " (" + formatFileSize(file.length()) + ")";
                    setIcon(UIManager.getIcon("FileView.fileIcon"));
                }
                setText(text);
                setToolTipText(file.getAbsolutePath());
            }
            
            return this;
        }
        
        private String formatFileSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
            if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
            return (bytes / (1024 * 1024 * 1024)) + " GB";
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Try to set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Fallback to default look and feel
                System.out.println("Could not set system look and feel, using default");
            }
            
            new SecureFileDeleter().setVisible(true);
        });
    }
}