package transactionlib;

import java.util.concurrent.atomic.AtomicInteger;

public class TransactionManager {
    // Verwaltet die Erzeugung von Transaktionen und stellt sicher, dass jede Transaktion eine eindeutige ID erhält.
    
    private ZfsManager zfsManager;
    private AtomicInteger transactionCounter;
    
    public TransactionManager(ZfsManager zfsManager) {
        this.zfsManager = zfsManager;
        this.transactionCounter = new AtomicInteger(0);
    }
    
    /**
     * Beginnt eine neue Transaktion und gibt das Transaction-Objekt zurück.
     * Die Transaktions-ID wird dynamisch erzeugt (Kombination aus einem Zähler und dem aktuellen Zeitstempel).
     */
    public Transaction beginTransaction() throws Exception {
        int count = transactionCounter.incrementAndGet();
        long timestamp = System.currentTimeMillis();
        String transactionId = "tx_" + count + "_" + timestamp;
        // Übergibt die Transaktions-ID und den ZfsManager an die Transaction
        return new Transaction(transactionId, zfsManager);
    }
}
