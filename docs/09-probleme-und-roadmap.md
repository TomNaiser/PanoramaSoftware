# 09 — Bekannte Probleme und offene Nachweise

[Inhalt](README.md) · [Tests und Messung](10-build-tests-und-wartung.md)

Diese Liste unterscheidet offene Ursachen, fehlende Funktionen und noch ausstehende Nachweise. Eine behobene Ursache wird nicht als heute noch unveränderter Defekt aufgeführt; der zugehörige reale Erfolg kann trotzdem noch offen sein.

## Offene Punkte

Aktuell geprüfter Stand vom 27.10.: Android **0.6.27**, reguläre Firmware
**2.4.75**, Windows **27.5.4**, Motorprofil **Revision 40**. Der STOP-Fehler
bei pausierter Android-Wiederaufnahme ist korrigiert; der Tilt-Startbremsvorhalt
bleibt bei 0,110 s. Anschließend wurden ausschließlich die Pan-Ankunftsfenster
0,30°/0,40° auf 0,15°/0,25° zurückgestellt. **96 reale Aufnahmen** ergaben Pan
maximal 0,236°, Median 0,089°; alle Pan-Werte blieben unter 0,3°. Der 72er-Lauf
enthielt 13 Zielverfehlungswarnungen und einen Tilt-Wert von 0,315°.
[Korrektur und Nachweisgrenzen](validation/2026-10-27-pan-positioning.md).
Revision 39 bleibt als Rückfallstand erhalten. Die vollständige neue
Surface-Bedienfolge einschließlich Wiederaufnahme ist weiterhin offen.

Historischer Ausgangsbefund vom 26.10.: Das Surface-Supportlog mit Firmware 2.4.75 belegt Pan-Gegenkorrekturen am unveränderten Aufnahmeziel. Windows 27.5.2 ergänzt direkte Touchbedienung der Pfeile. Der damals offene [Diagnosestand](https://github.com/TomNaiser/PanoramaControl/blob/master/docs/history/WINDOWS-27.5.2-SURFACE-TOUCH.md) wurde durch identische Vergleichsrezepte und die oben dokumentierte Pan-Korrektur fortgeführt. Ein allgemeiner Geschwindigkeitsvorteil ist daraus nicht belegt.

Separater lokaler Firmwareversuch 2.4.76 vom 27.10.: Die Tilt-Winkelberechnung wird vom flüchtigen Sensor-Nullregister gelöst. Der dortige reale vollständige Neustart liest Nullregister 0 bei unverändert plausiblem Tilt-Winkel; 129 Motorparameter bleiben identisch. [Versuch und Nachweisgrenzen](https://github.com/TomNaiser/PanoDuinoControl/blob/master/docs/history/TILT-REFERENCE-2.4.76.md). Dieser Versuch ist nicht auf dem aktuell geprüften Kopf installiert und erklärt weder den STOP-Fehler noch die Pan-Korrektur.

| Priorität | Problem / Lücke | Aktueller Stand | Konkreter nächster Nachweis |
|---|---|---|---|
| Hoch | Sporadischer Tilt-Sensor-Fault im Feld unter Wind | Ein gültiger I2C-Lesezyklus fehlte; die elektrische Ursache ist unbekannt. Kein Beleg für bloß zu große Soll-/Ist-Differenz | Detailmessung von Busfehlern, Versorgung, Steckern/Kabeln und zeitlichem Bezug zur Last; anhaltenden Fehler weiterhin hart stoppen |
| Hoch | Wiederaufnahme nach tatsächlichem neuen Feld-Fault | Android-Code und isolierte Pixel-Prüfungen korrigiert, neuer Motor-/Windversuch nach 0.6.24/0.6.25 noch nicht durchgeführt | Nach realer Störung Prüfung, Spalten-/Bandwahl, Laden, ausdrücklicher Start und stimmige Kamera-/Exportfolge belegen |
| Hoch | Reale Fehlerquote und Laufdurchgängigkeit | Aktuelle 1/1.000-Anforderung nicht statistisch belegt | Längere getrennte Reihen mit Fehlertypen, Wiederholungen, Pausen, vollständiger Expositions-/Laufzeit und belastbarem Nenner |
| Hoch | Reale Abnahme der Windows-Wiederaufnahme | 27.5.0 implementiert Checkpoints, sphärische Restziele und verknüpften Export; softwaregeprüft | Bluetooth-Portzuordnung, Recovery und Kamerafolge am realen Kopf abnehmen |
| Mittel | Windows-XML auf weiteren Dateisystemen | 27.5.0 veröffentlicht vollständige atomare Fassungen; Dateisperre und Neustart-Rebuild lokal geprüft | Verhalten mit produktivem Zielpfad, weiteren Dateisystemen und extern lesendem Stitcher prüfen |
| Mittel | Kleine Zielabweichung vs. tatsächliche Überlappung | Warnschwelle und optionale feste 1°-Pause sind getrennt; keine allgemeine optische Garantie | Tatsächliche Nachbarlagen mit Bildwinkel/Orientierung auswerten, mindestens 20 % als Abnahmebedingung |
| Mittel | Bilddateien und Bildschärfe unbekannt | Elektrischer Trigger und Encoderposition sind bekannt, keine Kamera-Erfolgsrückmeldung | Reale Dateizählung, Reihenfolge, Schärfe/Nachschwingen und mögliche Zusatzdatei nach instabiler Belichtung prüfen |
| Mittel | Windows-Gesamtabnahme mit aktuellem Kopf | 27.5.4: 1.633 Softwareprüfungen sowie 96 reale Aufnahmen mit Revision 40 am Entwicklungsrechner; vollständige Surface-Bedienfolge offen | Gemischter Bedienlauf einschließlich Fehler, Neuverbindung, Jog und Export auf dem Surface |
| Mittel | Exportdetails Android/Windows unterschiedlich | Windows 27.5.0 führt Rollwinkel explizit und exportiert UTC; CSV-/Archivschemata bleiben unterschiedlich | Gewünschten PTGui-Workflow und Kameraorientierung festlegen, gemeinsame Referenzdateien prüfen |
| Mittel | Vollständige Kugelaufnahme | Bandgeometrie vorhanden; reale Tilt-Grenzen begrenzen die Abdeckung, vollständiger Zenit/Nadir ist nicht nachgewiesen | Mechanik-/Optikabdeckung und Montage separat abnehmen; mathematischen Modus nicht als mechanische Freigabe darstellen |
| Niedrig | 0.6.25-Extras-Texte visuell noch nicht erneut geprüft | Sicherungsservice, Dateien und Prozessneustart geprüft; letzte Displayprüfung scheiterte am nicht entsperrten Vordergrund | Extras bei entsperrtem Gerät in Hoch-/Querformat prüfen |
| Ausbau | Gemeinsames Archiv und kompletter Parametereditor | Nicht umgesetzt | Eigenen Umfang definieren, Datenherkunft und Revisionsmigration erhalten |

## Behobene Ursachen mit verbleibender Prüfgrenze

| Frühere Ursache | Korrektur | Nicht daraus ableiten |
|---|---|---|
| Zu großes gespeichertes Pan-Ankunftsfenster | Revision 40: 0,15°/0,25°; andere 127 Werte unverändert; 96 reale Aufnahmen mit Pan maximal 0,236° | Keine Warnungsfreiheit, Lastunabhängigkeit oder optische Garantie |
| STOP vor Android-Restauftrag ohne ursprüngliche Laufnummer | Android 0.6.27 übermittelt den ursprünglichen Lauf; protokolltreue Wiederaufnahmeprüfung | Kein Nachweis einer neuen realen Sensor-Fault-Wiederaufnahme |
| Überlappende Windows-Diagnoseübertragungen erzeugten TX_OVERFLOW | 27.5.4 wartet auf DEBUG_LOG_END und serialisiert große Abrufe; am Kopf geprüft | Keine allgemeine Fehlerfreiheit von Bluetooth |
| Pan-Zielverfehlung schaltete noch fahrendes Tilt mit ab | Firmware ab 2.4.74: Zielverfehlung pro Achse, andere Achse beendet begrenzte Fahrt | Keine generelle Wind- oder Blockadefreiheit |
| Quittierter alter Vorfall unterdrückte neue Qualitätsmeldung | Android 0.6.23: Anlass-/Quittierungsidentitäten getrennt | Kein optischer Qualitätsnachweis |
| `MOTION_NOT_QUIET` führte zum vermeidbaren endgültigen Ablaufende | Firmware 2.4.75: begrenzter Retry/erhaltene Pause | Der fehlerfreie 333er-Lauf benötigte diesen Retry nicht und prüfte ihn daher nicht real |
| Sensor-Fault fehlte in Androids Wiederaufnahmeliste; Aktion unverständlich verborgen | Android 0.6.24: Checkpoint, sichtbarer Sperrgrund, Restauftrag und sphärische Bänder | Kein Übergehen anhaltender Sensorfehler oder geänderter Referenz |
| Android erzeugte XML nur bei explizitem Export | Android 0.6.25: laufende atomare Sicherung und Rebuild | Keine gemeinsame atomare DB-/Dateisystemtransaktion |
| Blockierter Bluetooth-Write hielt Protokoll-/UI-Ablauf fest | Android unabhängiger Schreibwatchdog; Windows 27.4.4 asynchroner serieller Write, 27.5.0 unabhängige Schreibfrist und abgetrennte alte Portobjekte | Keine Garantie, dass Funk oder Transport niemals ausfallen |

## Reale Befunde richtig lesen

Der Basislauf vom 23.10. mit älterer Firmware 2.4.73 enthielt 892 bestätigte Zyklen und vier grobe Tilt-Fehlpositionen. Die Ursache war das gekoppelte Abschalten beider Achsen, nicht ein überschrittenes gemeinsames Bewegungsbudget. Nach der Korrektur durchfuhr der kurze 48-Punkte-Vergleich den entscheidenden Fehlerpfad erfolgreich. [Befund (Zusammenfassung)](validation/README.md#motor-2026-10-23).

Der anschließende Lauf mit 2.4.75 wurde auf Benutzerwunsch nach **333 bestätigten Punkten** beendet: keine spontane Unterbrechung, 13 vollständige Spaltenrückfahrten, 628 geprüfte Nachbarpaare ohne Unterschreiten von 20 %. Dies ist keine vollständige 1.000er-Motorqualifikation. [Messreihe (Zusammenfassung)](validation/README.md#motor-2026-10-23).

Der Feldlauf am 24.10. erreichte **264/300 bestätigte Aufnahmen**. Beim nächsten Punkt trat `FAULT_TILT_SENSOR` auf. Die letzte protokollierte kleine Zielabweichung rechtfertigt nicht die Behauptung, der Encoder habe nur „unsinnige Winkel“ geliefert oder die Firmware habe lediglich eine Zielschwelle falsch benannt. Für die tatsächliche elektrische Ursache fehlen die passenden Rohmessungen. [Feldbefund und Wiederaufnahmefix (Zusammenfassung)](validation/README.md#recovery-2026-10-24).

## Sinnvolle Reihenfolge weiterer Arbeit

1. Android mit aktueller Firmware erneut real prüfen: erst kurze Wiederaufnahme- und Stopfälle, dann längere Reihen. Aufbau, Balance und Wetter getrennt erfassen.
2. Jeden Fehler nach Zielverfehlung, Stillstand, Sensor-/Transportfehler, instabiler Belichtung und bewusster Benutzeraktion klassifizieren; Fortsetzungsdauer und verlorene Aufnahmen messen.
3. Den Sensor-Fault nur mit gezielten Bus-/Versorgungsdaten auf eine elektrische Ursache zurückführen. Eine pauschale Toleranzlockerung löst fehlende Messdaten nicht.
4. Windows 27.5.4 über die geprüften Aufnahmefolgen hinaus bei Wiederaufnahme, Stabilität und XML-Verfügbarkeit auf dem Surface real abnehmen.
5. Danach die Statistik für den vereinbarten Grenzwert und den tatsächlichen Kamera-/Stitcherworkflow abschließen.

Eine neue Hardwaremessung setzt einen angeschlossenen, freien und beaufsichtigten Aufbau voraus. Die reine Erstellung dieser Dokumentation ist keine Freigabe für Fahrten oder Auslösungen.
