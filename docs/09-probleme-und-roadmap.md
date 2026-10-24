# 09 — Bekannte Probleme und offene Nachweise

[Inhalt](README.md) · [Tests und Messung](10-build-tests-und-wartung.md)

Diese Liste unterscheidet offene Ursachen, fehlende Funktionen und noch ausstehende Nachweise. Eine behobene Ursache wird nicht als heute noch unveränderter Defekt aufgeführt; der zugehörige reale Erfolg kann trotzdem noch offen sein.

## Offene Punkte

| Priorität | Problem / Lücke | Aktueller Stand | Konkreter nächster Nachweis |
|---|---|---|---|
| Hoch | Sporadischer Tilt-Sensor-Fault im Feld unter Wind | Ein gültiger I2C-Lesezyklus fehlte; die elektrische Ursache ist unbekannt. Kein Beleg für bloß zu große Soll-/Ist-Differenz | Detailmessung von Busfehlern, Versorgung, Steckern/Kabeln und zeitlichem Bezug zur Last; anhaltenden Fehler weiterhin hart stoppen |
| Hoch | Wiederaufnahme nach tatsächlichem neuen Feld-Fault | Android-Code und isolierte Pixel-Prüfungen korrigiert, neuer Motor-/Windversuch nach 0.6.24/0.6.25 noch nicht durchgeführt | Nach realer Störung Prüfung, Spalten-/Bandwahl, Laden, ausdrücklicher Start und stimmige Kamera-/Exportfolge belegen |
| Hoch | Reale Fehlerquote und Laufdurchgängigkeit | Aktuelle 1/1.000-Anforderung nicht statistisch belegt | Längere getrennte Reihen mit Fehlertypen, Wiederholungen, Pausen, vollständiger Expositions-/Laufzeit und belastbarem Nenner |
| Hoch | Windows-Fortsetzungsführung hinter Android | `RecoverAsync()` kann Auftrag löschen; Checkpoint-/Restauftragsführung fehlt | Android-Verhalten einschließlich sphärischer Originalziele portieren und simuliert sowie real abnehmen |
| Mittel | Windows-Live-XML bei Prozessabbruch unvollständig | Inkrementeller Writer schließt Tags erst bei Abschluss; Journalexport ist Ausweichweg | Atomare vollständige Fassungen nach Capture und Neustart-Rebuild implementieren |
| Mittel | Kleine Zielabweichung vs. tatsächliche Überlappung | Warnschwelle und optionale feste 1°-Pause sind getrennt; keine allgemeine optische Garantie | Tatsächliche Nachbarlagen mit Bildwinkel/Orientierung auswerten, mindestens 20 % als Abnahmebedingung |
| Mittel | Bilddateien und Bildschärfe unbekannt | Elektrischer Trigger und Encoderposition sind bekannt, keine Kamera-Erfolgsrückmeldung | Reale Dateizählung, Reihenfolge, Schärfe/Nachschwingen und mögliche Zusatzdatei nach instabiler Belichtung prüfen |
| Mittel | Windows-Gesamtabnahme mit aktuellem Kopf | Neueste Windows-Tests simuliert; frühere reale Prüfung nur kurz und älterer Firmwarestand | Gemischter Bedienlauf einschließlich Fehler, Neuverbindung, Jog und Export mit aktueller Version |
| Mittel | Exportdetails Android/Windows unterschiedlich | Rollwert, Zeitstil, CSV-Spalten und lokaler Archivaufbau abweichend | Gewünschten PTGui-Workflow und Kameraorientierung festlegen, gemeinsame Referenzdateien prüfen |
| Mittel | Vollständige Kugelaufnahme | Bandgeometrie vorhanden; reale Tilt-Grenzen begrenzen die Abdeckung, vollständiger Zenit/Nadir ist nicht nachgewiesen | Mechanik-/Optikabdeckung und Montage separat abnehmen; mathematischen Modus nicht als mechanische Freigabe darstellen |
| Niedrig | 0.6.25-Extras-Texte visuell noch nicht erneut geprüft | Sicherungsservice, Dateien und Prozessneustart geprüft; letzte Displayprüfung scheiterte am nicht entsperrten Vordergrund | Extras bei entsperrtem Gerät in Hoch-/Querformat prüfen |
| Ausbau | Gemeinsames Archiv und kompletter Parametereditor | Nicht umgesetzt | Eigenen Umfang definieren, Datenherkunft und Revisionsmigration erhalten |

## Behobene Ursachen mit verbleibender Prüfgrenze

| Frühere Ursache | Korrektur | Nicht daraus ableiten |
|---|---|---|
| Pan-Zielverfehlung schaltete noch fahrendes Tilt mit ab | Firmware ab 2.4.74: Zielverfehlung pro Achse, andere Achse beendet begrenzte Fahrt | Keine generelle Wind- oder Blockadefreiheit |
| Quittierter alter Vorfall unterdrückte neue Qualitätsmeldung | Android 0.6.23: Anlass-/Quittierungsidentitäten getrennt | Kein optischer Qualitätsnachweis |
| `MOTION_NOT_QUIET` führte zum vermeidbaren endgültigen Ablaufende | Firmware 2.4.75: begrenzter Retry/erhaltene Pause | Der fehlerfreie 333er-Lauf benötigte diesen Retry nicht und prüfte ihn daher nicht real |
| Sensor-Fault fehlte in Androids Wiederaufnahmeliste; Aktion unverständlich verborgen | Android 0.6.24: Checkpoint, sichtbarer Sperrgrund, Restauftrag und sphärische Bänder | Kein Übergehen anhaltender Sensorfehler oder geänderter Referenz |
| Android erzeugte XML nur bei explizitem Export | Android 0.6.25: laufende atomare Sicherung und Rebuild | Keine gemeinsame atomare DB-/Dateisystemtransaktion |
| Blockierter Bluetooth-Write hielt Protokoll-/UI-Ablauf fest | Android unabhängiger Schreibwatchdog; Windows 27.4.4 asynchroner serieller Write | Keine Garantie, dass Funk oder Transport niemals ausfallen |

## Reale Befunde richtig lesen

Der Basislauf vom 23.10. mit älterer Firmware 2.4.73 enthielt 892 bestätigte Zyklen und vier grobe Tilt-Fehlpositionen. Die Ursache war das gekoppelte Abschalten beider Achsen, nicht ein überschrittenes gemeinsames Bewegungsbudget. Nach der Korrektur durchfuhr der kurze 48-Punkte-Vergleich den entscheidenden Fehlerpfad erfolgreich. [Befund (Zusammenfassung)](validation/README.md#motor-2026-10-23).

Der anschließende Lauf mit 2.4.75 wurde auf Benutzerwunsch nach **333 bestätigten Punkten** beendet: keine spontane Unterbrechung, 13 vollständige Spaltenrückfahrten, 628 geprüfte Nachbarpaare ohne Unterschreiten von 20 %. Dies ist keine vollständige 1.000er-Motorqualifikation. [Messreihe (Zusammenfassung)](validation/README.md#motor-2026-10-23).

Der Feldlauf am 24.10. erreichte **264/300 bestätigte Aufnahmen**. Beim nächsten Punkt trat `FAULT_TILT_SENSOR` auf. Die letzte protokollierte kleine Zielabweichung rechtfertigt nicht die Behauptung, der Encoder habe nur „unsinnige Winkel“ geliefert oder die Firmware habe lediglich eine Zielschwelle falsch benannt. Für die tatsächliche elektrische Ursache fehlen die passenden Rohmessungen. [Feldbefund und Wiederaufnahmefix (Zusammenfassung)](validation/README.md#recovery-2026-10-24).

## Sinnvolle Reihenfolge weiterer Arbeit

1. Android mit aktueller Firmware erneut real prüfen: erst kurze Wiederaufnahme- und Stopfälle, dann längere Reihen. Aufbau, Balance und Wetter getrennt erfassen.
2. Jeden Fehler nach Zielverfehlung, Stillstand, Sensor-/Transportfehler, instabiler Belichtung und bewusster Benutzeraktion klassifizieren; Fortsetzungsdauer und verlorene Aufnahmen messen.
3. Den Sensor-Fault nur mit gezielten Bus-/Versorgungsdaten auf eine elektrische Ursache zurückführen. Eine pauschale Toleranzlockerung löst fehlende Messdaten nicht.
4. Windows bei Wiederaufnahme, Stabilitätseinstellungen und XML-Verfügbarkeit nachziehen.
5. Danach die Statistik für den vereinbarten Grenzwert und den tatsächlichen Kamera-/Stitcherworkflow abschließen.

Eine neue Hardwaremessung setzt einen angeschlossenen, freien und beaufsichtigten Aufbau voraus. Die reine Erstellung dieser Dokumentation ist keine Freigabe für Fahrten oder Auslösungen.
