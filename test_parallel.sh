#!/bin/bash

echo "Starte wiederholte parallele Tests für BrainstormingApp..."

for i in {1..5}; do
  (
    echo "3"
    sleep 1
    echo "idea_Idea2.txt"
    sleep 1
    echo "Kommentar A in Durchlauf $i"
    sleep 1
    echo "4"
  ) | java -cp bin transactionlib.BrainstormingApp &

  (
    echo "3"
    sleep 1
    echo "idea_Idea2.txt"
    sleep 1
    echo "Kommentar B in Durchlauf $i"
    sleep 1
    echo "4"
  ) | java -cp bin transactionlib.BrainstormingApp &

  wait
  echo "Durchlauf $i beendet."
done

echo "Alle Tests abgeschlossen."
