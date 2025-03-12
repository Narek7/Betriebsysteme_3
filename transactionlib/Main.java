package transactionlib;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        try {
            // Konfiguration: Hier wird das ZFS-Dataset angegeben.
            // Dies entspricht der Vorgabe, dass der Snapshot programmatisch erstellt wird.
            String dataset = "testpool/mydata";
            ZfsManager zfsManager = new ZfsManager(dataset);
            TransactionManager txManager = new TransactionManager(zfsManager);
            
            // Beginne eine neue Transaktion, die intern einen Snapshot anlegt und einen Arbeitsbereich erstellt.
            Transaction tx = txManager.beginTransaction();
            
            // Beispielhafte Dateioperationen: Eine Datei "test.txt" wird im Kontext der Transaktion gelesen und bearbeitet.
            File testFile = new File("test.txt");
            
            // Lese den Inhalt der Datei.
            // Dabei wird im ersten Zugriff die Live-Datei in den Arbeitsbereich kopiert.
            String content = tx.readFile(testFile);
            System.out.println("Inhalt vor der Änderung: " + content);
            
            // Schreibe neuen Inhalt in die Datei.
            // Dieser Vorgang wird zunächst nur in der Arbeitskopie durchgeführt.
            tx.writeFile(testFile, "Neuer Inhalt in der Datei.");
            
            // Versuche, die Transaktion zu committen.
            // Dabei werden die gespeicherten Metadaten mit den aktuellen Live-Daten verglichen,
            // um sicherzustellen, dass keine Konflikte aufgetreten sind.
            boolean committed = tx.commit();
            if (!committed) {
                System.out.println("Commit fehlgeschlagen, Transaktion wurde zurückgesetzt.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
