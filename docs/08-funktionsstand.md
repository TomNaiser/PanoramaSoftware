# 08 — Umgesetzte und fehlende Funktionen

[Inhalt](README.md) · [Probleme und Roadmap](09-probleme-und-roadmap.md)

Stand: Firmware 2.4.75 / Android 0.6.25 / Windows 27.4.4. **Umgesetzt** beschreibt den Quellcode; die rechte Spalte begrenzt den Nachweis. „Teilweise“ bezeichnet einen echten verbleibenden Unterschied und nicht nur fehlende Dokumentation.

## Funktionsmatrix

| Funktion | Firmware | Android | Windows | Nachweis / Einschränkung |
|---|---|---|---|---|
| V2 mit CRC, ACK/NACK und Retry | Umgesetzt | Umgesetzt | Umgesetzt | Host-/Simulatorprüfungen und reale Verbindungen dokumentiert |
| Priorisierter Not-Stop | Umgesetzt | Umgesetzt | Umgesetzt | Softwaretests; Funkverlust kann Zustellung verhindern, Lease/Policy bleiben zusätzlich relevant |
| Encodergeführte Pan-/Tiltfahrt | Umgesetzt | Bedienung umgesetzt | Bedienung umgesetzt | Reale Fahrten vorhanden; keine universelle Last-/Windabnahme |
| Achsweise Behandlung kleiner Zielverfehlung | Umgesetzt seit 2.4.74 | Warnung auswertbar | Basiswarnung auswertbar | Gekoppelte Fahrt real und im Modell geprüft |
| Manuelle Ziel-/Schrittfahrt | Umgesetzt | Umgesetzt | Umgesetzt | Kleine reale Fahrten dokumentiert |
| Gehaltene Pfeile mit Lease | Umgesetzt | Umgesetzt | Umgesetzt | Loslassen/Fokusverlust und Token geprüft |
| Start/Ende merken und anfahren | Grundlage umgesetzt | Umgesetzt | Umgesetzt | Frische Istwerte und Beruhigung; numerische Eingabe ist ein bewusster Entwurf |
| Tilt-Nullpunkt und gespeicherte Grenzen | Umgesetzt | Umgesetzt | Umgesetzt | Keine automatische mechanische Vermessung |
| Getrennte manuelle Achstempi | Umgesetzt | Umgesetzt | Umgesetzt | Tempo ist keine direkte PWM-Vorgabe |
| Lauftempo bis 2.000 Promille | Fähigkeit umgesetzt | Umgesetzt | Umgesetzt | Nur mit gemeldeter Fähigkeit und absoluter Achsgrenze |
| Kameraprofile / Bildwinkel / Überlappung | Rezeptannahme | Umgesetzt | Umgesetzt | Keine automatische Objektiverkennung |
| Rastervorschau und autonome Acquise | Umgesetzt | Umgesetzt | Umgesetzt | Mehrere reale Reihen; aktueller Fehlerquotennachweis offen |
| Sphärische Bandgeometrie | Umgesetzt | Umgesetzt | Umgesetzt | Gemeinsame Geometrie und kurze reale Bänder; vollständige Kugel am begrenzten Kopf nicht nachgewiesen |
| Laden getrennt von Start | Umgesetzt | Umgesetzt | Umgesetzt | ACK plus aktiver Geräteplan beachten |
| Normale Pause / Resume | Umgesetzt | Umgesetzt | Umgesetzt | Gilt nicht pauschal für Fault/Stopped |
| Spaltenrücksprung / Vorsprung | Umgesetzt | Umgesetzt | Umgesetzt | Modus-/Zustandsprüfung; echte Wiederholungen separat zählen |
| Zeiten während Acquise ändern | Umgesetzt | Umgesetzt | Umgesetzt | Vorgemerkt und aktiv unterscheiden |
| Autonome Timelapse | Umgesetzt | Umgesetzt | Umgesetzt | Elektrische Pulse, keine Bilddatei-Rückmeldung |
| Fortsetzen bei Funkverlust als Policy | Umgesetzt | Umgesetzt | Umgesetzt | Zwei wählbare Policies; nicht beide zugleich |
| Begrenzter automatischer Bewegungs-Retry | Umgesetzt | Meldung/Bedienung umgesetzt | Keine gleichwertige spezielle UI | Wirkung firmwareseitig; keine unbeschränkte Wiederholung |
| Erhaltene Pause nach behebbarer Instabilität | Umgesetzt | Spezifische Führung umgesetzt | Normaler Pausepfad vorhanden | Android-Fehlerführung nicht vollständig portiert |
| Restauftrag nach Sensor-/Transport-/Stop-Fall | Gerätebausteine vorhanden | Umgesetzt seit 0.6.24 | Fehlt als gleichwertiger Ablauf | Android software-/Pixel-geprüft; neuer Wind-Hardwaretest offen |
| Wahl betroffene/vorherige Spalte | Bausteine / Retry | Umgesetzt | Kein gleichwertiger Fault-Restauftragsdialog | Gemeinsamer Export der Android-Kindläufe |
| Sphärische Wiederaufnahme mit Originalbändern | Kein beliebiger Rezeptneuschnitt | Umgesetzt seit 0.6.24 | Fehlt | Ganzzahlige Originalziele in Software geprüft |
| Warnschwellen und rote Warnpunkte | Umgesetzt | Umgesetzt | Umgesetzt | Warnung wird nicht automatisch Sicherheits-Fault |
| Zusätzliche Qualitätspause über 1° | Kein entsprechender optischer Guard | Optional umgesetzt | Fehlt als gleichwertiger Guard | Für dokumentierten Motortest deaktiviert; kein 20-%-Garantiealgorithmus |
| Stabilität separat lesen/schreiben | Umgesetzt | Umgesetzt | Neue Spezialbedienung fehlt | Spanne, Drift und zusätzliche passive Wartezeit |
| Vollständiger Motorparameterkatalog | 129, persistent | Über Protokoll/Werkzeuge nutzbar | Über Protokoll/Werkzeuge nutzbar | Kein vollständiger GUI-Profil-Editor in beiden Apps |
| Dauerhaftes Capture-Archiv | Kein vollständiges Langzeitarchiv | SQLite umgesetzt | Journal umgesetzt | Keine automatische Archiv-Synchronisierung |
| Replay-Deduplizierung / Rekonstruktion | Begrenzte Segmente/Replays | Umgesetzt | Umgesetzt | Rekonstruktion ist kein gemessener Istwert |
| Papywizard-/CSV-Snapshot | Clientaufgabe | Umgesetzt | Umgesetzt | Formate nicht in allen Details identisch |
| Laufende jederzeit abgeschlossene XML | Clientaufgabe | Umgesetzt seit 0.6.25 | Fehlt im Live-Writer | Windows benötigt Abschluss oder separaten Journalexport |
| Atomare automatische XML-Ersetzung | Clientaufgabe | Umgesetzt | Fehlt | 1.000 Pixel-Updates getestet; DB-/Datei-Commit nicht gemeinsam atomar |
| Diagnose-/Supportexport | Umgesetzt, zielabhängig | Umgesetzt | Umgesetzt | Begrenzte Ringe/Logs, keine beliebig lange vollständige Rohsensorhistorie |
| Gespeicherte Läufe grafisch anzeigen | Clientaufgabe | Umgesetzt | Umgesetzt | Unbekannte historische Istpositionen bleiben unbekannt |
| Kamera-Dateierfolg / Bildtransfer / Stitching | Fehlt | Fehlt | Fehlt | Capturezähler sind kein Foto-/Schärfenachweis |
| Garantierte reale Mindestüberlappung | Fehlt als allgemeine Garantie | Nachträgliche Analyse möglich | Nachträgliche Analyse möglich | Anforderung mindestens 20 % bleibt aufbau-/optikabhängig |
| Fehlerquote höchstens 1/1.000 ohne Laufblockade | Nicht nachgewiesen | Nicht nachgewiesen | Nicht nachgewiesen | 333er-Motorlauf und 1.000er-XML-Test nicht vermischen |

## Was ausdrücklich noch fehlt

1. Windows-Port der neuen Android-Wiederaufnahme mit dauerhaftem Checkpoint, sphärischen Restbändern, verständlicher Auswahl und verknüpftem Export.
2. Windows-Live-XML mit abgeschlossenen Fassungen und atomarem Austausch sowie klarer Dateifehlerführung.
3. Vollständige reale Langzeitabnahme mit aktueller Kombination, Kameralast, mehreren Ausgleichs-/Wetterbedingungen und überprüfter Wiederaufnahme nach tatsächlich auftretenden Fehlern.
4. Nachweis der optischen Qualität und tatsächlichen Kameradateien; bei Bedarf eine kameraseitige Rückmeldung, die aktuell nicht existiert.
5. Allgemeine optikabhängige Qualitätsentscheidung aus tatsächlichen Nachbarpositionen statt einer festen Zielabweichung allein.
6. Expliziter Austausch der lokalen Archive zwischen beiden Apps und vereinheitlichte Exportdetails, falls identische Stitcher-Workflows gefordert sind.

Ein vollständiger 129-Parameter-GUI-Editor ist ein möglicher Ausbau, aber bisher kein notwendiger Bestandteil der vereinbarten Bedienparität. Grundlagen und Spezialwerkzeuge existieren; eine fertige GUI darf daraus nicht abgeleitet werden.
