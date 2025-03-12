package transactionlib;

import java.io.*;
import java.util.*;

public class ZfsManager {
    // Diese Klasse ist zuständig für das Erstellen und Verwalten von ZFS-Snapshots.
    // Sie wird von den Transaktionen verwendet, um zu Beginn einen konsistenten Zustand des Dateisystems zu sichern
    // und bei Konflikten einen Rollback durchzuführen.
    
    private String dataset;
    
    /**
     * Konstruktor.
     * @param dataset Name des ZFS-Datasets, z.B. "testpool/mydata"
     */
    public ZfsManager(String dataset) {
        this.dataset = dataset;
    }
    
    /**
     * Erstellt einen ZFS-Snapshot, der zur Transaktion gehört.
     * Hier wird ein Snapshot mit einem eindeutigen Namen (inklusive Transaktions-ID) angelegt.
     * @param transactionId Eindeutige Transaktions-ID
     * @return Den Namen des erstellten Snapshots
     */
    public String createSnapshot(String transactionId) throws IOException, InterruptedException {
        String snapshotName = dataset + "@tx_" + transactionId;
        ProcessBuilder pb = new ProcessBuilder("sudo", "zfs", "snapshot", snapshotName);
        int exitCode = runCommand(pb);
        if (exitCode != 0) {
            throw new IOException("Fehler beim Erstellen des Snapshots: " + snapshotName);
        }
        System.out.println("Snapshot erstellt: " + snapshotName);
        return snapshotName;
    }
    
    /**
     * Führt einen Rollback zum angegebenen Snapshot durch.
     * Falls der Snapshot nicht existiert, wird dies geloggt und der Rollback als erfolgreich angesehen.
     * @param snapshotName Der Snapshot, zu dem zurückgesetzt wird.
     */
    public void rollbackToSnapshot(String snapshotName) throws IOException, InterruptedException {
        // Prüfe, ob der Snapshot existiert.
        if (!snapshotExists(snapshotName)) {
            System.out.println("Snapshot " + snapshotName + " existiert nicht. Rollback wird übersprungen.");
            return;
        }
        // Hier wird "-r" hinzugefügt, um den Rollback zu erzwingen.
        ProcessBuilder pb = new ProcessBuilder("sudo", "zfs", "rollback", "-r", snapshotName);
        int exitCode = runCommand(pb);
        if (exitCode != 0) {
            throw new IOException("Fehler beim Rollback zum Snapshot: " + snapshotName);
        }
        System.out.println("Rollback durchgeführt: " + snapshotName);
    }
    
    /**
     * Überprüft, ob ein Snapshot existiert.
     */
    private boolean snapshotExists(String snapshotName) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("sudo", "zfs", "list", "-t", "snapshot", snapshotName);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        boolean found = false;
        while ((line = reader.readLine()) != null) {
            if (line.contains(snapshotName)) {
                found = true;
                break;
            }
        }
        process.waitFor();
        return found;
    }
    
    /**
     * Führt den übergebenen ProcessBuilder aus und gibt den Exit-Code zurück.
     */
    private int runCommand(ProcessBuilder pb) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        return process.waitFor();
    }
}
