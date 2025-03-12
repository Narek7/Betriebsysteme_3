# Projekt: Optimistische Nebenläufigkeit mit ZFS-Snapshots

## Installation & Einrichtung

### 1. Systemabhängigkeiten überprüfen
Bevor du das Projekt ausführst, überprüfe, ob alle notwendigen Abhängigkeiten installiert sind. Falls nicht, installiere sie mit den folgenden Befehlen:

```sh
sudo apt update && sudo apt upgrade -y
sudo apt install openjdk-21-jdk zfsutils-linux -y
```

### 2. Projekt klonen


```sh
git clone <https://github.com/Narek7/Betriebsysteme_3>
cd java-project
```

Falls du die Dateien manuell übertragen hast, stelle sicher, dass du dich im richtigen Verzeichnis befindest:

```sh
cd ~/java-project
```

### 3. Installierte Pakete anzeigen (optional)
Falls du überprüfen möchtest, welche Pakete installiert sind, kannst du die folgende Datei einsehen:

```sh
cat installed_requirements.txt
```


## Kompilieren & Ausführen

### 1. Java-Code kompilieren

```sh
javac transactionlib/*.java
```


### 2. Programme ausführen

```sh
java transactionlib.Main 
#oder auch andere dateien wie 
#java transactionlib.ConflictScriptSequential
#java transactionlib.ConflictScriptParallel
#java transactionlib.BrainstormingApp
#java transactionlib.ValidationTool
```

Falls ein anderes Startmodul verwendet wird, passe den Befehl entsprechend an.


## Tests & Validierung

### 1. Test-Skripte ausführbar machen
Falls nötig, erstelle die ausführbaren Rechte für die Shell-Skripte:

```sh
chmod +x test_parallel.sh
```

### 2. Testlauf ausführen
Um den parallelen Testlauf zu starten:

```sh
./test_parallel.sh
```

## Bekannte Probleme & Fehlerbehebung

### Problem: Kein Speicherplatz mehr verfügbar (`No space left on device`)
Falls du während der Ausführung oder Kompilierung auf folgende Fehlermeldung stößt:

```
error: error while writing <Datei>: No space left on device
```

oder

```
OpenJDK 64-Bit Server VM warning: Insufficient space for shared memory file
```

**Lösung:** Überprüfe den Speicherplatz mit:

```sh
df -h
```

Falls das Root-Verzeichnis (`/`) oder ein anderes kritisches Dateisystem voll ist, bereinige temporäre Dateien und alte Logs:

```sh
rm -rf ~/.vscode-server
rm -rf ~/.vscode
sudo apt autoremove -y
sudo apt clean
```

Falls das Problem weiterhin besteht, kann es sein, dass zu viele Snapshots in ZFS existieren. Prüfe diese mit:

```sh
zfs list -t snapshot
```

Falls notwendig, lösche alte Snapshots:

```sh
sudo zfs destroy testpool/mydata@<snapshot-name>
```

**Vorsicht:** Stelle sicher, dass du nur Snapshots löschst, die nicht mehr benötigt werden!


### Problem: ZFS Rollback-Fehler (`dataset does not exist`)
Falls du folgende Fehlermeldung erhältst:

```
cannot open 'testpool/mydata@tx_xxx': dataset does not exist
Rollback-Fehler: Fehler beim Rollback zum Snapshot: testpool/mydata@tx_xxx
```

**Lösung:** Überprüfe, ob der Snapshot existiert:

```sh
zfs list -t snapshot | grep mydata
```

Falls der Snapshot nicht existiert, wurde er entweder gelöscht oder nicht korrekt erstellt. Prüfe, ob das System richtig konfiguriert ist und ob genügend Speicherplatz für Snapshots vorhanden ist.

Falls Snapshots vorhanden sind, kannst du einen manuellen Rollback versuchen:

```sh
sudo zfs rollback testpool/mydata@<snapshot-name>
```

Falls Snapshots fehlen, überprüfe das Transaktionslog und stelle sicher, dass Snapshots korrekt erstellt werden.


## Fazit
Das Projekt simuliert Transaktionsvalidierung mit ZFS-Snapshots und testet verschiedene konkurrierende Schreib-/Leseoperationen. Während der Entwicklung wurde festgestellt, dass Speicherplatz und Snapshot-Management eine entscheidende Rolle spielen. Zudem konnte durch gezielte Anpassungen im `ZfsManager.java` und `Transaction.java` die Konfliktrate maximiert werden. Die endgültige Validierung zeigte eine hohe Konfliktrate, was den gewünschten Effekt erzielte. Dennoch bleibt eine Optimierung hinsichtlich Speicherverwaltung und effizienter Rollback-Strategien ein potenzieller Verbesserungspunkt.

