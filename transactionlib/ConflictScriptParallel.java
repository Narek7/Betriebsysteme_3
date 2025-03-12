package transactionlib;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class ConflictScriptParallel {
    public static void main(String[] args) throws InterruptedException {
        // Dieses Script simuliert parallele Transaktionen in zwei Threads,
        // um Konflikte und das Rollback-Verhalten zu testen.
        
        // 1) ZFS-Dataset anpassen
        String dataset = "testpool/mydata";
        
        ZfsManager zfsManager = new ZfsManager(dataset);
        TransactionManager txManager = new TransactionManager(zfsManager);

        // Erzeuge und starte Thread A, der eine Transaktion ausführt.
        Thread threadA = new Thread(() -> {
            try {
                runTransactionA(txManager);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Erzeuge und starte Thread B, der parallel arbeitet.
        Thread threadB = new Thread(() -> {
            try {
                runTransactionB(txManager);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        threadA.start();
        threadB.start();

        // Warte, bis beide Threads ihre Transaktionen abgeschlossen haben.
        threadA.join();
        threadB.join();

        System.out.println("=== ConflictScriptParallel beendet ===");
    }

    private static void runTransactionA(TransactionManager txManager) throws Exception {
        System.out.println("[Thread A] Starte Transaktion A");
        Transaction txA = txManager.beginTransaction();
        File testFile = new File("test.txt");

        // Lese den aktuellen Zustand der Datei und kopiere ihn in den Arbeitsbereich.
        String contentA = txA.readFile(testFile);
        System.out.println("[Thread A] Inhalt vor Änderung: " + contentA);

        // Schreibe neuen Inhalt in die Arbeitskopie.
        txA.writeFile(testFile, "A: Änderung durch Thread A.\n");

        // Simuliere Bearbeitungszeit.
        Thread.sleep(2000);

        // Versuche, die Transaktion zu committen. Bei Konflikt erfolgt ein Rollback.
        boolean committedA = txA.commit();
        if (committedA) {
            System.out.println("[Thread A] Transaktion A erfolgreich committed.\n");
        } else {
            System.out.println("[Thread A] Commit fehlgeschlagen, Rollback durchgeführt.\n");
        }
    }

    private static void runTransactionB(TransactionManager txManager) throws Exception {
        System.out.println("[Thread B] Starte Transaktion B");
        Transaction txB = txManager.beginTransaction();
        File testFile = new File("test.txt");

        // Lese den Dateiinhalt, um den Zustand vor Änderungen zu erfassen.
        String contentB = txB.readFile(testFile);
        System.out.println("[Thread B] Inhalt vor Änderung: " + contentB);

        // Schreibe neuen Inhalt in die Arbeitskopie.
        txB.writeFile(testFile, "B: Änderung durch Thread B.\n");

        // Simuliere längere Bearbeitungszeit.
        Thread.sleep(3000);

        // Versuche, die Transaktion zu committen.
        boolean committedB = txB.commit();
        if (committedB) {
            System.out.println("[Thread B] Transaktion B erfolgreich committed.\n");
        } else {
            System.out.println("[Thread B] Commit fehlgeschlagen, Rollback durchgeführt.\n");
        }
    }
}
