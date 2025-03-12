package transactionlib;

import java.io.*;
import java.security.*;

public class FileMetadata {
    // Diese Klasse erfasst die Metadaten einer Datei (Zeitstempel und SHA-256 Hash),
    // um Änderungen an Dateien innerhalb einer Transaktion zu überwachen und Konflikte zu erkennen.
    
    private long lastModified;
    private String fileHash;
    
    public FileMetadata(long lastModified, String fileHash) {
        this.lastModified = lastModified;
        this.fileHash = fileHash;
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    public String getFileHash() {
        return fileHash;
    }
    
    /**
     * Erzeugt FileMetadata aus einer Datei.
     * Falls die Datei nicht existiert, werden Standardwerte verwendet.
     */
    public static FileMetadata fromFile(File file) throws IOException, NoSuchAlgorithmException {
        if (!file.exists()) {
            // Falls die Datei nicht existiert, verwende Default-Werte.
            return new FileMetadata(0, "");
        }
        long lastModified = file.lastModified();
        String hash = computeHash(file);
        return new FileMetadata(lastModified, hash);
    }
    
    /**
     * Berechnet den SHA-256 Hash einer Datei.
     * Diese Methode wird genutzt, um den Inhalt einer Datei eindeutig zu identifizieren.
     */
    private static String computeHash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
