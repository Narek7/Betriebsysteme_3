package transactionlib;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Transaction {
    // Diese Klasse repräsentiert eine einzelne Transaktion.
    // Sie erstellt zu Beginn einen ZFS-Snapshot und einen temporären Arbeitsbereich,
    // in dem Dateiänderungen vorgenommen werden.
    
    private String transactionId;
    private String snapshotName;
    private ZfsManager zfsManager;
    // Speichert die initialen Metadaten (Zeitstempel, Hash) der Dateien, um Konflikte zu erkennen.
    private Map<File, FileMetadata> fileMetadataMap;
    private boolean active;
    // Temporärer Arbeitsbereich, in dem alle Dateiänderungen vorgenommen werden.
    private File workingDir;

    /**
     * Startet eine neue Transaktion: erstellt den ZFS-Snapshot und den Arbeitsbereich.
     * Damit wird zu Beginn ein konsistenter Zustand des Dateisystems gesichert.
     */
    public Transaction(String transactionId, ZfsManager zfsManager) throws Exception {
        this.transactionId = transactionId;
        this.zfsManager = zfsManager;
        // Erstelle den ZFS-Snapshot mit einem eindeutigen Namen
        this.snapshotName = zfsManager.createSnapshot(transactionId);
        this.fileMetadataMap = new HashMap<>();
        this.active = true;
        // Erstelle einen temporären Arbeitsbereich, in dem alle Dateiänderungen erfolgen
        this.workingDir = new File(System.getProperty("java.io.tmpdir"), "tx_" + transactionId);
        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }
    }

    public String getTransactionId() {
        return transactionId;
    }
    
    public String getSnapshotName() {
        return snapshotName;
    }
    
    /**
     * Ermittelt die Arbeitskopie zu einer gegebenen Datei.
     * Hier wird vereinfacht davon ausgegangen, dass die Datei im aktuellen Verzeichnis liegt.
     */
    private File getWorkingFile(File file) {
        return new File(workingDir, file.getName());
    }
    
    /**
     * Liest den Inhalt einer Datei innerhalb der Transaktion.
     * Falls noch keine Arbeitskopie existiert, wird die Live-Datei in den Arbeitsbereich kopiert und
     * die ursprünglichen Metadaten werden gespeichert, um spätere Konflikte erkennen zu können.
     */
    public String readFile(File file) throws IOException, NoSuchAlgorithmException {
        File workingFile = getWorkingFile(file);
        if (!workingFile.exists()) {
            if (file.exists()) {
                // Kopiere die aktuelle Live-Datei in den temporären Arbeitsbereich
                Files.copy(file.toPath(), workingFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                // Falls die Datei nicht existiert, wird eine leere Datei erzeugt
                workingFile.createNewFile();
            }
            // Speichere die initialen Metadaten, um später Konflikte zu erkennen
            FileMetadata initialMetadata = FileMetadata.fromFile(file);
            fileMetadataMap.put(file, initialMetadata);
        }
        // Liefere den Inhalt der Arbeitskopie zurück
        return new String(Files.readAllBytes(workingFile.toPath()), StandardCharsets.UTF_8);
    }
    
    /**
     * Schreibt den Inhalt in die Arbeitskopie der Datei.
     * So erfolgen alle Schreibvorgänge isoliert im temporären Arbeitsbereich, bis der Commit erfolgt.
     */
    public void writeFile(File file, String content) throws IOException, NoSuchAlgorithmException {
        File workingFile = getWorkingFile(file);
        if (!workingFile.exists()) {
            if (file.exists()) {
                Files.copy(file.toPath(), workingFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                workingFile.createNewFile();
            }
            // Speichere initiale Metadaten, falls sie noch nicht erfasst wurden
            fileMetadataMap.put(file, FileMetadata.fromFile(file));
        }
        // Schreibe den neuen Inhalt in die Arbeitskopie
        Files.write(workingFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Löscht die Datei in der Arbeitskopie.
     * Dadurch wird eine Löschoperation innerhalb der Transaktion realisiert.
     */
    public void deleteFile(File file) throws IOException, NoSuchAlgorithmException {
        File workingFile = getWorkingFile(file);
        if (workingFile.exists()) {
            workingFile.delete();
        }
        // Speichere initiale Metadaten, falls sie noch nicht vorhanden sind, um Konflikte erkennen zu können
        if (!fileMetadataMap.containsKey(file)) {
            fileMetadataMap.put(file, FileMetadata.fromFile(file));
        }
    }
    
    /**
     * Versucht, die Transaktion zu committen.
     * Es werden alle bearbeiteten Dateien überprüft, ob sie seit dem Snapshot verändert wurden.
     * Bei Konflikten wird ein Rollback durchgeführt, ansonsten werden die Änderungen übernommen.
     *
     * @return true, falls Commit erfolgreich, false bei Konflikt.
     */
    public boolean commit() throws Exception {
        if (!active) {
            throw new IllegalStateException("Transaktion ist nicht mehr aktiv.");
        }
        // Konfliktprüfung: Vergleiche für jede bearbeitete Datei die gespeicherten Metadaten mit den aktuellen Metadaten.
        for (Map.Entry<File, FileMetadata> entry : fileMetadataMap.entrySet()) {
            File liveFile = entry.getKey();
            FileMetadata initialMetadata = entry.getValue();
            FileMetadata currentMetadata = FileMetadata.fromFile(liveFile);
            if (initialMetadata.getLastModified() != currentMetadata.getLastModified() ||
                !initialMetadata.getFileHash().equals(currentMetadata.getFileHash())) {
                // Falls ein Unterschied festgestellt wird, liegt ein Konflikt vor – führe Rollback aus.
                System.out.println("Konflikt erkannt für Datei: " + liveFile.getAbsolutePath());
                rollback();
                return false;
            }
        }
        // Falls keine Konflikte auftreten, werden die Änderungen aus dem Arbeitsbereich in das Live-Dateisystem übernommen.
        for (File liveFile : fileMetadataMap.keySet()) {
            File workingFile = getWorkingFile(liveFile);
            if (workingFile.exists()) {
                Files.copy(workingFile.toPath(), liveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                // Wenn die Arbeitskopie gelöscht wurde, lösche auch die Live-Datei.
                Files.deleteIfExists(liveFile.toPath());
            }
        }
        active = false;
        System.out.println("Transaktion " + transactionId + " erfolgreich committed.");
        // Optional: Hier könnte man den Snapshot löschen, wenn er nicht mehr benötigt wird.
        return true;
    }
    
    /**
     * Führt ein Rollback der Transaktion durch:
     * - Setzt den Zustand des Dateisystems mittels des zuvor erstellten ZFS-Snapshots zurück.
     * - Entfernt den temporären Arbeitsbereich.
     */
    public void rollback() throws Exception {
        if (!active) {
            throw new IllegalStateException("Transaktion ist nicht mehr aktiv.");
        }
        try {
            zfsManager.rollbackToSnapshot(snapshotName);
        } catch (IOException e) {
            // Logge den Fehler, aber markiere die Transaktion trotzdem als zurückgesetzt
            System.err.println("Rollback-Fehler: " + e.getMessage());
        }
        active = false;
        System.out.println("Transaktion " + transactionId + " wurde zurückgesetzt.");
        deleteDirectoryRecursively(workingDir);
    }

    
    /**
     * Hilfsmethode zum rekursiven Löschen eines Verzeichnisses.
     */
    private void deleteDirectoryRecursively(File dir) {
        if (dir.isDirectory()) {
            File[] entries = dir.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectoryRecursively(entry);
                }
            }
        }
        dir.delete();
    }
}
