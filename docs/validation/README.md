# Prüfstand und bekannte Grenzen

[Gesamtdokumentation](../README.md) · [Testbefehle](../10-build-tests-und-wartung.md)

Dieser öffentliche Überblick fasst datierte Entwicklungsnachweise zusammen. Die vollständigen Telefon-/Datenbanksicherungen, Rohlogs und Messreihen gehören zur lokalen Entwicklungsablage und werden nicht als Teil des Quellrepositorys verteilt. Die folgenden Zahlen sind Ergebnisse dieser früheren Prüfungen, kein bei jedem Checkout neu erzeugter Zertifizierungsnachweis. Softwaretests sind aus den mitgelieferten Testquellen reproduzierbar; reale Fahrreihen erfordern einen passenden Aufbau.

## Festgehaltene Referenz und Windows-Abgleich — 27. Oktober 2026

**Aktueller Stand: Firmware 2.4.75, Android 0.6.27, Windows 27.5.4,
Motorprofil Revision 40.** Das gespeicherte Pan-Fenster wurde gezielt auf
0,15°/0,25° zurückgestellt, die anderen 127 Motorwerte bleiben erhalten.
Der reale 72er-Lauf mit Revision 40 ergab Pan maximal 0,236°, Median 0,106°.
Zusammen mit kurzem Vergleich und Gegenrichtung sind **96 Aufnahmen**
vollständig geprüft; Gegenrichtung Pan maximal 0,182° ohne Warnungen.
Über alle 96 Aufnahmen beträgt der Pan-Median 0,089°. Der 72er-Lauf
enthielt 13 Zielverfehlungswarnungen und einen Tilt-Wert von 0,315°.
Der Paketbuild bestand erneut 1.633 Softwareprüfungen; alle 129 Motorwerte
wurden abschließend zurückgelesen. Eine vollständige neue Surface-
Wiederaufnahme und optische Bildqualitätsprüfung bleiben offen.
[Ursache, Messreihen und neues Profilpaket](2026-10-27-pan-positioning.md).
Die nachstehenden Absätze halten den davor gesicherten Stand fest.

### Vorherige Vergleichsstände vom selben Tag

Android **0.6.27**, reguläre Firmware **2.4.75** und Motorprofil **Revision 39**
bilden den bestätigten Stand, Hauptprojekt-Commit `665f0dc`.
[Reale Positionierungsgegenprobe](2026-10-27-positioning.md).
Windows **27.5.3** ist dazu gebaut: 1.427 Host-/Controller-, 129 WinForms-,
16 Vorschau-, 34 Navigations- und 19 Touchprüfungen bestanden. Der echte
Windows-/Surface-Hardwarelauf steht noch aus.
[Paket, gleiche Aufnahmebedingungen und Grenzen](https://github.com/TomNaiser/PanoramaControl/blob/master/docs/WINDOWS-27.5.3-ANDROID-0.6.27.md).

Fortsetzung: **Windows 27.5.4** besteht 1.633 Softwareprüfungen und den realen
Lesevergleich serialisierter Diagnoseübertragungen. Zwei Windows-Fahrten mit
dem identischen Android-Rezept lieferten zuletzt Pan max. 0,348°, Median
0,174°; Android-Referenz max. 0,242°, Median 0,152°. Die Parser liefern für
dieselben Messframes exakt dieselben Positionen. Die verbleibende Pan-
Restabweichung ist kein nachgewiesener Windows-Anzeigefehler und wird nicht
als behoben ausgegeben. **81/100** Diagnoseauslösungen insgesamt.
[Messwerte, Referenzvorgaben und Korrekturen](https://github.com/TomNaiser/PanoramaControl/blob/master/docs/WINDOWS-27.5.4-PAN-VERGLEICH.md).

<a id="firmware-2-4-75"></a>
## Firmware 2.4.75 — 23. Oktober 2026

- 20.286 Host-Assertions mit FakeArduino/Wire und Regel-/Hardwaremodellen bestanden.
- Normaler Nano-R4-Bluetooth-Build sowie Flash-/SRAM- und Boot-Stack-Prüfung bestanden.
- Kleine Zielverfehlungen werden achsweise behandelt, damit die andere Achse ihre zulässige Fahrt beendet.
- Für ausgewählte Bewegungsfehler ist ein einmaliger Retry am noch unbestätigten Bildpunkt implementiert. Danach bleibt ein geeigneter Auftrag pausiert.
- Sensor-, Grenz-, Kontrollzeit- und Not-Stop-Schutz bleiben vorrangig.

Die Hostmodelle sind kein Nachweis einer realen Motorfehlerrate von höchstens 1/1.000.

<a id="motor-2026-10-23"></a>
## Reale Motorreihen — 23. Oktober 2026

| Reihe | Stand / Umfang | Ergebnis und Grenze |
|---|---|---|
| Basis | Ältere Firmware 2.4.73; 892 bestätigte Punkte | Vier grobe Tilt-Fehlpositionen, zwölf Nachbarpaare unter 20 % Überlappung. Ursache: Pan-Zielverfehlung stoppte die noch fahrende Tilt-Achse mit |
| Kurzer Vergleich | Firmware 2.4.74; 48 Punkte | Entscheidender gekoppelter Fehlerpfad erfolgreich durchlaufen; keine grobe Fehlposition, kein Ersatz für Langzeitmessung |
| Nachfolge | Firmware 2.4.75; 333 Punkte | Auf Benutzerwunsch beendet, keine spontane Unterbrechung; 13 vollständige Spaltenrückfahrten, 628 Nachbarpaare ohne Überlappung unter 20 % |

Im 333er-Lauf betrugen die maximalen absoluten Zielabweichungen Pan 0,355° und Tilt 0,865°. Die minimale geprüfte Überlappung lag bei Tilt-Nachbarn bei 23,88 %, bei Pan-Nachbarn bei 30,96 %. Die zusätzliche Android-Qualitätspause über 1° war für diesen Motortest bewusst ausgeschaltet. Der automatische Retry wurde im fehlerfreien Nachfolgelauf nicht benötigt und damit nicht real provoziert.

<a id="recovery-2026-10-24"></a>
## Praxisfehler und Android 0.6.24 — 24. Oktober 2026

Ein Praxispanorama endete nach 264 von 300 bestätigten Bildern mit `FAULT_TILT_SENSOR`. Dieser Fehlercode bezeichnet einen nicht gültig abgeschlossenen Sensorzugriff; eine bloß zu große Soll-/Ist-Abweichung ist damit nicht belegt. Die elektrische Ursache bleibt offen.

Androids Wiederaufnahme wurde um solche Unterbrechungen erweitert. Frischer Gerätestand, Bootkennung und Tilt-Referenz bleiben Voraussetzung. Betroffene oder vorherige Spalte beziehungsweise vollständige sphärische Originalbänder können als neuer verknüpfter Restauftrag geladen und danach ausdrücklich gestartet werden.

- 131 JVM-Tests, Lint und APK-Build bestanden.
- In einer isolierten Pixel-Test-App: 69 Wiederaufnahme-/Dialogprüfungen und 21 Speicherprüfungen bestanden.
- Vorhandene 3.332 Aufnahmezeilen und Einstellungen blieben beim Update unverändert.
- Kein neuer Wind-/Motorversuch nach dieser Korrektur; die neueste reale Wiederaufnahme ist damit weiterhin offen.

<a id="papywizard-2026-10-24"></a>
## Android 0.6.25: laufende XML — 24. Oktober 2026

- 136 JVM-Tests bestanden, darunter fünf Tests der automatischen Sicherung.
- 1.001 atomar geschriebene XML-Fassungen von null bis 1.000 Aufnahmen jeweils geparst.
- Auf dem Pixel 1.000 einzelne tatsächliche SQLite-/XML-Aktualisierungen geprüft; insgesamt 3.024 Assertions einschließlich Deduplizierung, Schreibfehler, Neustart, verknüpftem Teilauftrag, Rekonstruktion und verspäteten Istwerten bestanden.
- Gesamtdauer der Pixel-Suite einschließlich Datenbank, XML-Parsing und Exportvergleich: 91,631 Sekunden. Das ist keine isolierte Dateischreibzeit.
- Aus den bestehenden Daten automatisch 42 vollständige XML-Dateien mit 3.332 Aufnahmen erzeugt und gegen SQLite verglichen; Daten und Einstellungen unverändert.
- Der Praxisexport mit 264 bestätigten Bildern wurde auch nach echtem App-Prozessneustart vollständig und bytegleich wiederhergestellt.

Ein abrupter Prozessabbruch zwischen Datenbank-Commit und Hintergrundsicherung kann die XML kurz zurücklassen; beim nächsten Start wird sie aus SQLite ergänzt. Die 1.000 XML-Aktualisierungen sind keine 1.000 Motorfahrten und kein optischer Bildqualitätsnachweis.

<a id="windows-27-4-4"></a>
## Windows 27.4.4 — 22. Oktober 2026

192 Protokoll-/Controller-Prüfungen und 115 simulierte WinForms-Prüfungen bestanden. Serielle Writes laufen außerhalb des UI-Threads und der ACK-/Abbruchsperre. Die neueren Android-Pfade für Fault-Restaufträge und fortlaufend abgeschlossene XML sind noch nicht vollständig portiert. Vorherige kurze reale Windows-Versuche ersetzen keine aktuelle Dauermessung.

<a id="offene-nachweise"></a>
## Weiter offen

Elektrische Ursache des Sensor-Faults, reale Wiederaufnahme mit aktueller Kombination, Fehlerquote unter verschiedenen Last-/Wetterbedingungen, tatsächliche Kameradateien/Bildschärfe und vollständige mechanische Kugelabdeckung. [Probleme und nächste Schritte](../09-probleme-und-roadmap.md).

## Windows 27.5.0 — 25. Oktober 2026

Der lokale Paketlauf bestand 1.415 Host-/Protokoll-/Controllerprüfungen, 129 simulierte WinForms-Prüfungen und 16 Raster-/Skalierungs-/Transferprüfungen. Die .NET-4.8-App und das ZIP-Paket wurden erfolgreich gebaut. Geprüft wurden unter anderem echter SQLite-Zugriff über den Windows-Systemprovider, Journalimport, vorübergehende DB-Schreibsperre mit Nachsicherung des originalen Messereignisses, Dateisperren, Neustart-Rebuild, abgebrochene Restvorbereitung nach CLEAR/LOAD, sphärische Originalziele, Start-Sperre bei falschem Restplan, Stabilitätsrevisionen, Qualitätspause und Sprachwechsel ohne Zurücksetzen des aktuellen Zustands.

Das ist eine Software-/Simulatorabnahme. Reale Bluetooth-Portzuordnung, Treiber, Kameradateifolge und Wiederaufnahme unter Last wurden mit dieser Windows-Version noch nicht neu geprüft. [Umsetzung, Bedienung und Grenzen](https://github.com/TomNaiser/PanoramaControl/blob/master/docs/history/WINDOWS-27.5.0-ANDROID-PARITAET.md).
