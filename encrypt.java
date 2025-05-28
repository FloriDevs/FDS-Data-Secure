import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class App extends JFrame {

    private JTextField passwordField;
    private JLabel statusLabel;

    public App() {
        // Setzt den Titel des Fensters
        setTitle("FDSecure - Datei-Verschlüsselung");
        // Legt die Standardoperation beim Schließen des Fensters fest
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Setzt die Größe des Fensters
        setSize(500, 300);
        // Zentriert das Fenster auf dem Bildschirm
        setLocationRelativeTo(null);
        // Legt das Layout des Hauptfensters fest
        setLayout(new BorderLayout(10, 10)); // 10 Pixel Abstand zwischen Komponenten

        // Erstellt ein Panel für die Eingabesteuerung (Passwortfeld und Beschriftung)
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20)); // Polsterung

        // Erstellt eine Beschriftung für das Passwortfeld
        JLabel passwordLabel = new JLabel("Passwort:");
        passwordLabel.setFont(new Font("Inter", Font.PLAIN, 16)); // Schriftart anpassen
        inputPanel.add(passwordLabel);

        // Erstellt ein Textfeld für die Passworteingabe
        passwordField = new JPasswordField(20); // 20 Zeichen Breite
        passwordField.setFont(new Font("Inter", Font.PLAIN, 16));
        inputPanel.add(passwordField);
        add(inputPanel, BorderLayout.NORTH); // Fügt das Eingabepanel oben hinzu

        // Erstellt ein Panel für die Schaltflächen
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 15, 0)); // 1 Reihe, 2 Spalten, 15 Pixel horizontaler Abstand
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 50, 20, 50)); // Polsterung

        // Erstellt die Schaltfläche zum Verschlüsseln
        JButton encryptButton = new JButton("Datei verschlüsseln");
        encryptButton.setFont(new Font("Inter", Font.BOLD, 16));
        encryptButton.setBackground(new Color(60, 179, 113)); // Mittelmeergrün
        encryptButton.setForeground(Color.WHITE);
        encryptButton.setFocusPainted(false); // Keine Fokus-Umrandung
        encryptButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(46, 139, 87), 2), // Dunkelgrüner Rand
                BorderFactory.createEmptyBorder(10, 20, 10, 20) // Innenpolsterung
        ));
        encryptButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Hand-Cursor beim Überfahren
        encryptButton.addActionListener(e -> encryptFile()); // Aktion beim Klicken
        buttonPanel.add(encryptButton);

        // Erstellt die Schaltfläche zum Entschlüsseln
        JButton decryptButton = new JButton("Datei entschlüsseln");
        decryptButton.setFont(new Font("Inter", Font.BOLD, 16));
        decryptButton.setBackground(new Color(70, 130, 180)); // Stahlblau
        decryptButton.setForeground(Color.WHITE);
        decryptButton.setFocusPainted(false);
        decryptButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 105, 225), 2), // Königsblau-Rand
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        decryptButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        decryptButton.addActionListener(e -> decryptFile()); // Aktion beim Klicken
        buttonPanel.add(decryptButton);
        add(buttonPanel, BorderLayout.CENTER); // Fügt das Schaltflächenpanel in die Mitte hinzu

        // Erstellt eine Statusleiste am unteren Rand
        statusLabel = new JLabel("Bereit.", SwingConstants.CENTER); // Text zentrieren
        statusLabel.setFont(new Font("Inter", Font.ITALIC, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // Polsterung
        add(statusLabel, BorderLayout.SOUTH); // Fügt die Statusleiste unten hinzu

        // Macht das Fenster sichtbar
        setVisible(true);
    }

    // Methode zum Verschlüsseln einer Datei
    private void encryptFile() {
        // Holt das Passwort aus dem Textfeld
        String password = new String(passwordField.getText());
        if (password.isEmpty()) {
            // Zeigt eine Fehlermeldung an, wenn kein Passwort eingegeben wurde
            showMessage("Fehler", "Bitte geben Sie ein Passwort ein.", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Erstellt einen Dateiauswahldialog
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Datei zum Verschlüsseln auswählen");
        // Zeigt den Öffnen-Dialog an
        int userSelection = fileChooser.showOpenDialog(this);

        // Prüft, ob der Benutzer eine Datei ausgewählt hat
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File inputFile = fileChooser.getSelectedFile();
            // Erstellt den Namen der Ausgabedatei mit der Endung .fdsecure
            File outputFile = new File(inputFile.getAbsolutePath() + ".fdsecure");

            try {
                // Liest den Inhalt der Eingabedatei in ein Byte-Array
                byte[] fileBytes = readAllBytes(inputFile);
                // Erzeugt einen Schlüssel aus dem Passwort
                byte[] key = generateKey(password);

                // Verschlüsselt die Dateibytes mit dem Schlüssel (XOR-Operation)
                byte[] encryptedBytes = new byte[fileBytes.length];
                for (int i = 0; i < fileBytes.length; i++) {
                    encryptedBytes[i] = (byte) (fileBytes[i] ^ key[i % key.length]); // XOR-Operation
                }

                // Schreibt die verschlüsselten Bytes in die Ausgabedatei
                writeAllBytes(outputFile, encryptedBytes);
                // Aktualisiert die Statusleiste
                statusLabel.setText("Datei erfolgreich verschlüsselt: " + outputFile.getName());
                // Zeigt eine Erfolgsmeldung an
                showMessage("Erfolg", "Datei erfolgreich verschlüsselt!", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException | NoSuchAlgorithmException ex) {
                // Behandelt Fehler beim Lesen, Schreiben oder bei der Schlüsselgenerierung
                statusLabel.setText("Fehler beim Verschlüsseln: " + ex.getMessage());
                showMessage("Fehler", "Fehler beim Verschlüsseln der Datei: " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    // Methode zum Entschlüsseln einer Datei
    private void decryptFile() {
        // Holt das Passwort aus dem Textfeld
        String password = new String(passwordField.getText());
        if (password.isEmpty()) {
            // Zeigt eine Fehlermeldung an, wenn kein Passwort eingegeben wurde
            showMessage("Fehler", "Bitte geben Sie ein Passwort ein.", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Erstellt einen Dateiauswahldialog
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("FDSecure-Datei zum Entschlüsseln auswählen");
        // Setzt einen Dateifilter, um nur .fdsecure-Dateien anzuzeigen
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".fdsecure");
            }

            @Override
            public String getDescription() {
                return "FDSecure-Dateien (*.fdsecure)";
            }
        });
        // Zeigt den Öffnen-Dialog an
        int userSelection = fileChooser.showOpenDialog(this);

        // Prüft, ob der Benutzer eine Datei ausgewählt hat
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File inputFile = fileChooser.getSelectedFile();
            // Überprüft, ob die ausgewählte Datei die .fdsecure-Endung hat
            if (!inputFile.getName().toLowerCase().endsWith(".fdsecure")) {
                showMessage("Fehler", "Bitte wählen Sie eine .fdsecure-Datei zum Entschlüsseln aus.", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Erstellt den Namen der Ausgabedatei (entfernt die .fdsecure-Endung)
            String outputFileName = inputFile.getName().substring(0, inputFile.getName().length() - ".fdsecure".length());
            File outputFile = new File(inputFile.getParent(), outputFileName);

            try {
                // Liest den Inhalt der verschlüsselten Datei in ein Byte-Array
                byte[] encryptedBytes = readAllBytes(inputFile);
                // Erzeugt den Schlüssel aus dem Passwort
                byte[] key = generateKey(password);

                // Entschlüsselt die Bytes mit dem Schlüssel (erneute XOR-Operation)
                byte[] decryptedBytes = new byte[encryptedBytes.length];
                for (int i = 0; i < encryptedBytes.length; i++) {
                    decryptedBytes[i] = (byte) (encryptedBytes[i] ^ key[i % key.length]); // XOR-Operation
                }

                // Schreibt die entschlüsselten Bytes in die Ausgabedatei
                writeAllBytes(outputFile, decryptedBytes);
                // Aktualisiert die Statusleiste
                statusLabel.setText("Datei erfolgreich entschlüsselt und exportiert: " + outputFile.getName());
                // Zeigt eine Erfolgsmeldung an
                showMessage("Erfolg", "Datei erfolgreich entschlüsselt und exportiert!", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException | NoSuchAlgorithmException ex) {
                // Behandelt Fehler beim Lesen, Schreiben oder bei der Schlüsselgenerierung
                statusLabel.setText("Fehler beim Entschlüsseln: " + ex.getMessage());
                showMessage("Fehler", "Fehler beim Entschlüsseln der Datei: " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    // Hilfsmethode zum Lesen aller Bytes aus einer Datei
    private byte[] readAllBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            return buffer;
        }
    }

    // Hilfsmethode zum Schreiben aller Bytes in eine Datei
    private void writeAllBytes(File file, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    // Generiert einen Schlüssel aus dem Passwort mithilfe von SHA-256
    private byte[] generateKey(String password) throws NoSuchAlgorithmException {
        // Verwendet SHA-256, um einen Hash des Passworts zu erzeugen
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(password.getBytes());
    }

    // Hilfsmethode zum Anzeigen von Nachrichtenboxen
    private void showMessage(String title, String message, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    // Hauptmethode zum Starten der Anwendung
    public static void main(String[] args) {
        // Führt die GUI-Erstellung im Event-Dispatch-Thread aus
        SwingUtilities.invokeLater(App::new);
    }
}

