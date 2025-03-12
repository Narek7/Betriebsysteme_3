package transactionlib;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * VALIDIERUNGSTOOL
 *
 * Dieses Tool simuliert mehrere gleichzeitige Transaktionen mit zufälligen Dateioperationen,
 * um Konflikte zu erzeugen und die Robustheit des Systems zu testen.
 * Es werden Metriken erhoben wie:
 * - Erfolgreiche Commits vs. Konflikte (Rollbacks)
 * - Durchschnittliche Transaktionsdauer
 *
 * Neuerungen für erhöhte Konfliktwahrscheinlichkeit:
 *  - Nur 1 Datei (file1.txt), damit sich alle Transaktionen wirklich "in die Quere kommen".
 *  - 10 zufällige Operationen pro Transaktion (jeweils 90% Schreibwahrscheinlichkeit).
 *  - Nach jeder Operation ein Thread.sleep(...) zwischen 1000ms und 5000ms,
 *    um die Transaktion zu verlängern und Überschneidungen zu provozieren.
 *  - 100 Threads, 200 Transaktionen (anpassbar).
 */
public class ValidationTool {

    public static void main(String[] args) throws Exception {
        // 1) ZFS-Dataset und Manager-Klassen initialisieren
        String dataset = "testpool/mydata";  // Anpassen an dein ZFS-Dataset
        ZfsManager zfsManager = new ZfsManager(dataset);
        TransactionManager txManager = new TransactionManager(zfsManager);

        // 2) Verzeichnis und Dateien für die Validierung anlegen
        File sharedDir = new File("validation");
        if (!sharedDir.exists()) {
            sharedDir.mkdir();
        }
        
        // Nur 1 Datei => Konfliktwahrscheinlichkeit steigt
        File file = new File(sharedDir, "file1.txt");
        if (!file.exists()) {
            Files.write(file.toPath(), "Initial content\n".getBytes(StandardCharsets.UTF_8));
        }

        // 3) Parameter für die Simulation
        final int numberOfTransactions = 200;   // Anzahl Transaktionen insgesamt
        final int concurrentThreads = 100;       // Anzahl Threads, die parallel Transaktionen starten
        final double writeProbability = 0.9;    // 90% Schreibzugriffe, 10% nur Lesen
        final int operationsPerTransaction = 10; // Mehrere Operationen pro Transaktion
        final int minSleepMs = 1000;             // Minimale künstliche Wartezeit
        final int maxSleepMs = 5000;             // Maximale künstliche Wartezeit

        // Thread-Pool für parallele Ausführung
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        List<Future<?>> futures = new ArrayList<>();

        // Metriken: Erfolgszähler, Konfliktzähler, Summenzeiten
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicLong totalTransactionTime = new AtomicLong(0);

        Random random = new Random();

        // 4) Starte die Transaktionen in parallelen Threads
        for (int i = 0; i < numberOfTransactions; i++) {
            Future<?> future = executor.submit(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    // Beginne eine neue Transaktion
                    Transaction tx = txManager.beginTransaction();

                    // Führe mehrere Operationen in dieser Transaktion durch
                    for (int op = 0; op < operationsPerTransaction; op++) {
                        // Mit hoher Wahrscheinlichkeit: Schreibzugriff, sonst nur Lesen
                        if (random.nextDouble() < writeProbability) {
                            String currentContent = tx.readFile(file);
                            String randomText = "RandomText_" + random.nextInt(1000) + "\n";
                            String newContent = currentContent + randomText;
                            tx.writeFile(file, newContent);
                        } else {
                            // Nur Lesen
                            tx.readFile(file);
                        }

                        // Künstliche Verzögerung, um Überlappungen zu erhöhen
                        int sleepTime = random.nextInt(maxSleepMs - minSleepMs + 1) + minSleepMs;
                        Thread.sleep(sleepTime);
                    }

                    // Versuche, die Transaktion zu committen (Konflikte => Rollback)
                    boolean committed = tx.commit();
                    long elapsed = System.currentTimeMillis() - startTime;
                    totalTransactionTime.addAndGet(elapsed);

                    if (committed) {
                        successCount.incrementAndGet();
                    } else {
                        conflictCount.incrementAndGet();
                    }

                } catch (IOException e) {
                    System.err.println("IOException: " + e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // 5) Warte auf Abschluss aller Transaktionen
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        // 6) Ausgabe der Metriken
        System.out.println("=== Validierung abgeschlossen ===");
        System.out.println("Gesamttransaktionen: " + numberOfTransactions);
        System.out.println("Erfolgreiche Commits: " + successCount.get());
        System.out.println("Rollbacks/Konflikte: " + conflictCount.get());
        double avgTransactionTime = (double) totalTransactionTime.get() / numberOfTransactions;
        System.out.println("Durchschnittliche Transaktionsdauer (ms): " + avgTransactionTime);
    }
}
