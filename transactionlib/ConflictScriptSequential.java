package transactionlib;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;

/**
 * Dieses Skript führt zwei Transaktionen A und B nacheinander aus.
 * Es demonstriert, dass B einen veralteten Stand sieht, da sein Snapshot vor A's Commit liegt,
 * und deshalb beim Commit ein Konflikt auftritt.
 */
public class ConflictScriptSequential {
    public static void main(String[] args) {
        try {
            // Konfiguration: ZFS-Dataset anpassen
            String dataset = "testpool/mydata";

            ZfsManager zfsManager = new ZfsManager(dataset);
            TransactionManager txManager = new TransactionManager(zfsManager);

            System.out.println("=== Starte Transaktion A ===");
            Transaction txA = txManager.beginTransaction();
            File testFile = new File("test.txt");

            // Transaktion A liest die Datei und speichert den aktuellen Zustand
            String contentA = txA.readFile(testFile);
            System.out.println("[Transaktion A] Inhalt vor Änderung: " + contentA);

            // Transaktion A ändert die Datei in der Arbeitskopie
            txA.writeFile(testFile, "A: Neue Zeile von A.\n");

            // Hier wird A pausiert, noch kein Commit erfolgt.
            System.out.println("\n=== Starte Transaktion B ===");
            Transaction txB = txManager.beginTransaction();

            // Transaktion B liest die Datei, sieht also den alten Stand
            String contentB = txB.readFile(testFile);
            System.out.println("[Transaktion B] Inhalt vor Änderung: " + contentB);

            // Transaktion B nimmt ebenfalls eine Änderung vor
            txB.writeFile(testFile, "B: Neue Zeile von B.\n");

            System.out.println("\n=== Committe nun Transaktion A zuerst ===");
            // Commit von Transaktion A – sollte konfliktfrei sein
            boolean committedA = txA.commit();
            if (committedA) {
                System.out.println("[Transaktion A] Erfolgreich committed.\n");
            } else {
                System.out.println("[Transaktion A] Commit fehlgeschlagen, Rollback durchgeführt.\n");
            }

            // Nun wird versucht, Transaktion B zu committen – hier sollte ein Konflikt erkannt werden
            System.out.println("=== Committe nun Transaktion B ===");
            boolean committedB = txB.commit();
            if (committedB) {
                System.out.println("[Transaktion B] Erfolgreich committed.\n");
            } else {
                System.out.println("[Transaktion B] Commit fehlgeschlagen, Rollback durchgeführt.\n");
            }

            System.out.println("=== ConflictScriptSequential beendet ===");

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
