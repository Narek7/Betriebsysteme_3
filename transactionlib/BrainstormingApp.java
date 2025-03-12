package transactionlib;

import java.io.File;
import java.util.Scanner;

public class BrainstormingApp {
    // Passe den Namen des ZFS-Datasets nach Bedarf an.
    private static final String DATASET = "testpool/mydata";
    // Das Verzeichnis, in dem die Ideen-Dateien abgelegt werden.
    private static final String IDEAS_DIR = "ideas";

    public static void main(String[] args) {
        // Sicherstellen, dass das Ideen-Verzeichnis existiert
        File ideasDir = new File(IDEAS_DIR);
        if (!ideasDir.exists()) {
            ideasDir.mkdir();
        }

        // Initialisiere ZfsManager und TransactionManager
        ZfsManager zfsManager = new ZfsManager(DATASET);
        TransactionManager txManager = new TransactionManager(zfsManager);

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Brainstorming Tool ===");
            System.out.println("1. Neue Idee hinzufügen");
            System.out.println("2. Bestehende Ideen anzeigen");
            System.out.println("3. Kommentar zu einer Idee hinzufügen");
            System.out.println("4. Beenden");
            System.out.print("Wähle eine Option: ");
            String option = scanner.nextLine();

            try {
                switch (option) {
                    case "1":
                        addIdea(txManager, scanner);
                        break;
                    case "2":
                        listIdeas();
                        break;
                    case "3":
                        addComment(txManager, scanner);
                        break;
                    case "4":
                        System.out.println("Anwendung wird beendet.");
                        scanner.close();
                        return;
                    default:
                        System.out.println("Ungültige Option. Bitte erneut versuchen.");
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Erstellt eine neue Idee.
     * Die Idee wird in einer Datei gespeichert, wobei als Inhalt auch ein Header für zukünftige Kommentare angelegt wird.
     */
    private static void addIdea(TransactionManager txManager, Scanner scanner) throws Exception {
        System.out.println("Gib den Titel der Idee ein:");
        String title = scanner.nextLine();
        System.out.println("Gib den Inhalt der Idee ein:");
        String content = scanner.nextLine();

        // Dateiname ableiten (z.B. idea_My_Idee.txt)
        String fileName = "idea_" + title.replaceAll("\\s+", "_") + ".txt";
        File ideaFile = new File(IDEAS_DIR, fileName);

        Transaction tx = txManager.beginTransaction();
        // Schreibe die Idee in die Datei, inklusive Platzhalter für Kommentare.
        String ideaContent = "Titel: " + title + "\n" + content + "\n\nKommentare:\n";
        tx.writeFile(ideaFile, ideaContent);
        if (tx.commit()) {
            System.out.println("Idee wurde erfolgreich hinzugefügt: " + fileName);
        } else {
            System.out.println("Fehler: Idee konnte nicht gespeichert werden. Transaktion wurde zurückgesetzt.");
        }
    }

    /**
     * Zeigt alle existierenden Ideen im Ideen-Verzeichnis an.
     */
    private static void listIdeas() {
        File ideasDir = new File(IDEAS_DIR);
        File[] ideaFiles = ideasDir.listFiles();
        if (ideaFiles == null || ideaFiles.length == 0) {
            System.out.println("Keine Ideen vorhanden.");
            return;
        }
        System.out.println("Vorhandene Ideen:");
        for (File file : ideaFiles) {
            System.out.println("- " + file.getName());
        }
    }

    /**
     * Fügt einer bestehenden Idee einen Kommentar hinzu.
     * Die Transaktion liest zunächst den aktuellen Inhalt, hängt den Kommentar an und commitet die Änderung.
     */
    private static void addComment(TransactionManager txManager, Scanner scanner) throws Exception {
        System.out.println("Gib den Dateinamen der Idee ein (z.B. idea_My_Idee.txt):");
        String fileName = scanner.nextLine();
        File ideaFile = new File(IDEAS_DIR, fileName);
        if (!ideaFile.exists()) {
            System.out.println("Die angegebene Idee existiert nicht.");
            return;
        }
        System.out.println("Gib deinen Kommentar ein:");
        String comment = scanner.nextLine();

        Transaction tx = txManager.beginTransaction();
        // Lese den aktuellen Inhalt der Ideen-Datei
        String currentContent = tx.readFile(ideaFile);
        // Füge den Kommentar am Ende der Datei hinzu
        String newContent = currentContent + comment + "\n";
        tx.writeFile(ideaFile, newContent);
        if (tx.commit()) {
            System.out.println("Kommentar wurde erfolgreich hinzugefügt.");
        } else {
            System.out.println("Fehler: Kommentar konnte nicht gespeichert werden. Transaktion wurde zurückgesetzt.");
        }
    }
}
