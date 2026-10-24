# Bluetooth-COM-Protokoll V2

**Status:** Implementierte Schnittstellenspezifikation, ergänzt 5. Oktober 2026
**Transport:** klassischer serieller Bluetooth-COM-Port, 57.600 Baud, 8N1  
**Ziel:** dieselbe Schnittstelle für PanoramaControl und spätere Mobilclients

| Komponente | Version / Stand |
|---|---|
| PanoramaControl | 27.2 / 3. August 2026 |
| PanoDuinoControl | 2.4.0 / 5. Oktober 2026 (hardwarefrei gebaut) |
| Drahtprotokoll | 2 |

## 1. Grundregeln

- Ein Rahmen ist eine druckbare ASCII-Zeile. Ausgehende Diagnoseframes dürfen bis zu 256 Zeichen umfassen; der Nano akzeptiert eingehend höchstens 164 Zeichen. Der längste gültige Clientbefehl `LOAD_PLAN` benötigt bei voll ausgenutzten Zahlenwerten höchstens 163 Zeichen.
- Aufbau: `@P2|sequence|messageType|field1|...|CRC16\r\n`
- `sequence` liegt zwischen 1 und 65535; 0 ist nur für Antworten auf nicht zuordenbare Rahmenfehler reserviert.
- `CRC16` besteht aus vier hexadezimalen Stellen und ist CRC-16-CCITT-FALSE über alle Zeichen vom `@` bis unmittelbar vor dem letzten `|`.
- Felder dürfen weder `|` noch Steuerzeichen enthalten. Winkel werden als vorzeichenbehaftete Milligrad-Ganzzahlen übertragen, Zeiten als Millisekunden.
- Ein Client sendet höchstens ein ACK-pflichtiges Kommando gleichzeitig. Bei einem Timeout wiederholt er exakt denselben Rahmen mit derselben Sequenz.
- Die Firmware speichert das Ergebnis des letzten verändernden Kommandos. Ein identisches Duplikat erhält dasselbe ACK/NACK ohne zweite Wirkung; anderer Inhalt mit derselben Sequenz erhält `SEQUENCE_CONFLICT`.

## 2. Koordinaten

Der sichtbare Koordinatenvertrag von PanoramaControl bleibt unverändert. Die historisch vor dem V1-Senden vorgenommene Vorzeichenumkehr beider Achsen wird im V2-Client durch `CoordinateContract.UiToDevice()` explizit ausgeführt. Alle Winkel im Drahtprotokoll und alle Firmware-Hartgrenzen sind **Gerätekoordinaten**.

Android ab 0.6.2 verwendet für Tilt dasselbe Vorzeichen wie der Encoder: positiver Tilt hebt am vorhandenen Aufbau das Objektiv. Positive Werte stehen im Diagramm oben, negative unten; Pan behält die bisherige Vorzeichenumkehr. Ab Firmware 2.3.5 beziehen sich Tiltziele, Status, Diagnose und CAPABILITIES auf den eingestellten Tilt-Nullpunkt. Intern bleiben die physischen Grenzen in der Encoder-Werkreferenz gespeichert. Die Firmware rechnet Ziele für den Regler zurück in diese Rohreferenz.

## 3. Antworten und spontane Meldungen

| Nachricht | Felder |
|---|---|
| `ACK` | `requestSequence`, `resultCode` |
| `NACK` | `requestSequence`, `errorCode` |
| `CAPABILITIES` | `panMin`, `panMax`, `tiltMin`, `tiltMax`, `limitsConfirmed` |
| `DEVICE_INFO` | `firmwareVersion`, `firmwareDate`, `protocolVersion`, `hardwareTarget` |
| `DEVICE_SESSION` | `bootId` (uint32; ab Firmware 2.3.4, 0 = keine dauerhaft gespeicherte Kennung) |
| `TILT_CALIBRATION` | `zeroOffsetMdeg`, `absoluteTiltMinMdeg`, `absoluteTiltMaxMdeg`; ab 2.3.5 nach CAPABILITIES; absolute Grenzzahlen bereits auf den aktuellen Nullpunkt bezogen |
| `NAVIGATION_CAPABILITIES` | `inactiveNavigation` (0/1); ab 2.3.6 nach TILT_CALIBRATION. 1 erlaubt MANUAL_MOVE auch in PLAN_READY, STOPPED und COMPLETED ohne Änderung des Auftrags |
| `MOTION_CAPABILITIES` | `profiledMotion` (0/1); ab 2.4.0. 1 kennzeichnet Profilregelung, zeitbezogene Ruhebestätigung und MANUAL_MOVE mit optionalem Tempo |
| `PLAN` | dieselben 16 Felder wie `LOAD_PLAN`; wird nur bei vorhandenem Lauf gesendet |
| `STATUS` | `runId`, `state`, `phase`, `completed`, `total`, `column`, `row`, `targetPan`, `targetTilt`, `actualPan`, `actualTilt`, `pauseRequested`, `eventSequence`, `lastError`, `nextImageIndex`, `planRevision`, `segmentCount`, `currentSegmentCaptureCount`, optional `timelapseIntervalMs`, optional `timelapsePulseMs` |
| `STATE_CHANGED` | `eventSequence`, `state` |
| `RUN_SEGMENT` | `runId`, `planRevision`, `startImageIndex`, `captureCount` |
| `IMAGE_COMPLETED` | `eventSequence`, `runId`, `captureSequence`, `planRevision`, `imageIndex`, `column`, `row`, `targetPan`, `targetTilt`, `actualPan`, `actualTilt`, `reconstructed` |
| `MANUAL_IMAGE_TRIGGERED` | `eventSequence`, `runId`, `manualCaptureSequence`, `rasterCaptureCount`, `actualPan`, `actualTilt`, `deviceTimeMs`, `replayed` |
| `DIAGNOSTICS` | `runId`, `state`, `phase`, `nextImageIndex`, `sampleSequence`, `stableSamples`, `requiredStableSamples`, `targetPan`, `targetTilt`, `actualPan`, `actualTilt`, `panError`, `tiltError`, `panWithinTolerance`, `tiltWithinTolerance`, `phaseElapsedMs`, `movementElapsedMs`, `settleTimeMs`, `movementTimeoutMs`, `currentFreeRamBytes`, `minimumFreeRamBytes` |
| `MOTION_DIAGNOSTICS` | `referencePanMdeg`, `referenceTiltMdeg`, `actualPanVelocityMdegPerSecond`, `actualTiltVelocityMdegPerSecond`, `referencePanVelocityMdegPerSecond`, `referenceTiltVelocityMdegPerSecond`, `panSignedPwm`, `tiltSignedPwm`, `panMotionPhase`, `tiltMotionPhase`; ab 2.4.0 nach DIAGNOSTICS. Ab 2.4.14 optional hinten `panDriveStage`, `tiltDriveStage`: 0 inaktiv, 1 Anlauf, 2 kontinuierliche Fahrt, 3 Leistung aus/Beruhigung, 4 ruhendes Ziel. Die ersten zehn Felder bleiben an gleicher Stelle. Bei Phasenfahrt ist die Referenzkurve nominell; die Phase bestimmt die Motorausgabe. |
| `DEBUG_EVENT` | `logSequence`, `deviceTimeMs`, `eventCode`, `runId`, `imageIndex`, `state`, `phase`, `targetPan`, `targetTilt`, `actualPan`, `actualTilt`, `panError`, `tiltError`, `stableSamples` |
| `DEBUG_LOG_END` | `newestSequence`, `sentRecords`, `droppedRecords` |

`STATUS` wird in einer V2-Sitzung mindestens alle 500 ms gesendet. Ein Client muss ihn als maßgebliche Wahrheit behandeln und darf den Ablaufzustand nicht aus lokalen UI-Schleifenzählern ableiten.

`completed` ist die monotone Anzahl tatsächlich abgeschlossener regulärer Rasterauslösungen; `total` bleibt die Größe des ursprünglichen Rasters. Nach einem Spaltenrücksprung kann `completed` daher größer als `total` werden. Der fachliche Fortsetzungspunkt steht in `nextImageIndex`, Wiederholungen in den `RUN_SEGMENT`-Meldungen.

## 4. Clientkommandos

| Kommando | Felder | Erfolgsantwort |
|---|---|---|
| `HELLO` | `clientName` | `HELLO_ACK`; danach `DEVICE_INFO`, `CAPABILITIES`, optional `PLAN`, `RUN_SEGMENT` und manuelle Puffereignisse, schließlich `STATUS` |
| `HEARTBEAT` | `runId` | keine Antwort |
| `GET_CAPABILITIES` | keine | `OK`, danach `CAPABILITIES` |
| `SET_TILT_LIMITS` | `minimumMdeg`, `maximumMdeg` in Gerätekoordinaten | `TILT_LIMITS_SAVED`, danach `CAPABILITIES`; nur im sicheren Stillstand und mit eingeschlossener Istposition |
| `SET_TILT_ZERO` | keine | `TILT_ZERO_SAVED`, danach CAPABILITIES, TILT_CALIBRATION und STATUS; aktuelle Position wird dauerhaft 0° |
| `RESET_TILT_ZERO` | keine | `TILT_ZERO_SAVED`, danach CAPABILITIES, TILT_CALIBRATION und STATUS; Encoder-Werkreferenz wiederherstellen |
| `GET_DIAGNOSTICS` | keine | `OK`, danach `DIAGNOSTICS`; read-only und ohne Einfluss auf Bewegung oder Zielerkennung |
| `GET_DEBUG_LOG` | optional `afterSequence` | Auf dem Nano R4 `OK`, danach bis zu 64 `DEBUG_EVENT` und `DEBUG_LOG_END`; alter AVR-Nano: `UNSUPPORTED_COMMAND` |
| `GET_SUPPORT_BUNDLE` | keine | `OK`, danach Geräteinfo, Fähigkeiten, Plan, Segmente, Status und Diagnose; Nano R4 zusätzlich Ereignisring |
| `CLEAR_DEBUG_LOG` | keine | Auf dem Nano R4 `CLEARED`; alter AVR-Nano: `UNSUPPORTED_COMMAND` |
| `GET_PLAN` | keine | `OK`, danach optional `PLAN` |
| `GET_STATUS` | keine | `OK`, danach `STATUS` |
| `LOAD_PLAN` | siehe Abschnitt 5 | `PLAN_ACCEPTED` |
| `START` | `runId` | `STARTED` |
| `PAUSE` | `runId` | `PAUSED` oder `PAUSE_PENDING` |
| `RESUME` | `runId` | `RESUMED` |
| `STOP` | `runId` | `STOPPED` |
| `ESTOP` | `runId`, `reason` | `STOPPED`; darf von jedem Client gesendet werden |
| `CLEAR_RUN` | keine | `CLEARED` |
| `REWIND_COLUMNS` | `runId`, `columns` | `REWOUND` oder `REWIND_PENDING` |
| `FORWARD_COLUMNS` | `runId`, `columns` | `FORWARDED` oder `FORWARD_PENDING` |
| `UPDATE_TIMING` | `runId`, `settleTimeMs`, `triggerPulseMs`, `exposureWaitMs` | `TIMING_UPDATED` oder `TIMING_QUEUED`; übernommene Zeiten erscheinen in `PLAN` |
| `MANUAL_MOVE` | `panMdeg`, `tiltMdeg`; ab MOTION_CAPABILITIES=1 optional zusätzlich `panTempoPermille`, `tiltTempoPermille` | `MOVING` |
| `TRIGGER_ONCE` | `pulseMs` | `TRIGGERED` |
| `START_TIMELAPSE` | `runId`, `intervalMs`, `pulseMs` | `TIMELAPSE_STARTED` |
| `STOP_TIMELAPSE` | `runId` | `TIMELAPSE_STOPPED` |

`STOP` setzt beide Motor-PWM-Ausgänge sofort auf null, deaktiviert alle Kameraausgänge und führt keine Parkfahrt aus. Die erforderliche Benutzerbestätigung ist Aufgabe des UI-Clients. `ESTOP` benötigt keine Steuerungs-Lease.

### Profilregelung ab Firmware 2.4.0

Bei `MOTION_CAPABILITIES|1` gelten für `MANUAL_MOVE` genau zwei oder vier Felder. Tempowerte müssen zwischen 1 und 1000 liegen; zwei Felder verwenden 500‰ je Achse. Das Tempo begrenzt die Profilgeschwindigkeit und skaliert nicht mehr den PWM-Ausgang. Dasselbe gilt für `panSpeedPermille` / `tiltSpeedPermille` im Aufnahmeplan. Beschleunigung, Ruck, Leistungsgrenzen und Reglerwerte sind vorläufige Firmwarestandards. Alte Firmware erhält weiterhin zwei Zielfelder.

Nach regulärer manueller Ankunft und nach Laufabschluss bleibt die Halteüberwachung aktiv. Ein ausdrücklicher STOP deaktiviert sie auch in IDLE oder COMPLETED; die vorhandene Laufkennung bleibt zu prüfen. Eine Teilrückfahrt mit Endposition außerhalb der Grenzen deaktiviert das Halten und erlaubt weitere Rückschritte. MANUAL_MOVE während RUNNING wird ohne Auftragsabbruch abgelehnt.

`stableSamples` / `requiredStableSamples` sind bei Profilregelung kompatible Fortschrittsfelder für die zeitliche Ruhebestätigung, keine Anzahl unverarbeiteter Sensorsamples. Die erforderliche Bestätigung beträgt derzeit 250 ms; `requiredStableSamples=13`, Fortschritt auf 0..13 normiert. Ein fertiges Profil, Fehler-, Geschwindigkeits- und Spannenfenster müssen gleichzeitig erfüllt sein. Settling beginnt mit dem gemeinsamen Ruhekandidaten beider Achsen; Freigabe nach `max(250 ms, settleTimeMs)` kontinuierlicher Ruhe. Der Bewegungstimer läuft bei erneuter Beruhigung weiter.

Die Motion-Phasen sind 0=AUS, 1=FAHREN, 2=UMKEHR, 3=HALTEN; HALTEN umfasst auch begrenzte Driftkorrekturen. Geschwindigkeiten stehen in Milligrad pro Sekunde, PWM auf der bestehenden vorzeichenbehafteten 8-Bit-Skala. GET_DIAGNOSTICS erzeugt beide Diagnosemeldungen. Unbekannte optionale Meldungen dürfen ältere Clients ignorieren.

Ruheverlust während Trigger oder Belichtungswartezeit erzeugt `EXPOSURE_UNSTABLE`; das Bild wird nicht als abgeschlossen gezählt. Ausbleibender Abtriebsfortschritt, überschrittenes Umkehrbudget, fehlende Sensorantworten, zu lange Regelzykluslücken oder TX-Pufferüberlauf schalten Bewegung und Kamera ab. Reconnect und erneute Freigabe starten kein vorheriges Ziel automatisch.

GET_DEBUG_LOG / GET_SUPPORT_BUNDLE übertragen den Ereignisring als Snapshot schrittweise und enden mit DEBUG_LOG_END. Der Client wartet auf diese Endmeldung, nicht nur auf ACK. UART-Ausgaben werden gepuffert; der synchrone Nano-R4-UART sendet höchstens acht Bytes pro Transportdurchlauf. Prüfstand und vorläufige Parameter: [Implementierungsbericht (Zusammenfassung)](docs/validation/README.md#offene-nachweise).

### Autonome Timelapse

Timelapse ist eine eigene Betriebsart und läuft auf dem Arduino auch ohne verbundenen Client. `START_TIMELAPSE` ist nur bei `IDLE` zulässig; währenddessen werden Panoramaaufträge, manuelle Fahrten und zusätzliche Einzeltrigger abgelehnt. `intervalMs` liegt zwischen 100 und 3.600.000, `pulseMs` zwischen 1 und 60.000 und muss kleiner als das Intervall sein. Die erste Aufnahme beginnt nach einem vollständigen Intervall; weitere Trigger beginnen im festen Abstand, bis `STOP_TIMELAPSE` oder `ESTOP` eintrifft. Stop deaktiviert einen laufenden Triggerpuls sofort.

`STATUS` meldet `TIMELAPSE` beziehungsweise `TIMELAPSE_STOPPED`, die Timelapse-`runId`, die Zahl abgeschlossener Trigger in `completed`, `total=0` für die unbegrenzte Laufzeit sowie Intervall und Pulsdauer in den beiden neuen Schlussfeldern. Ein neu verbundener Client kann damit den Lauf anzeigen und stoppen. Ein Arduino-Reset beginnt weiterhin ohne automatisches Wiederaufnehmen. Der Nano-Build und das statische Flash-/SRAM-Budget sind geprüft; Laufzeit-SRAM und Hardwaretest stehen aus.

Ab Firmware 2.3.2 entfernt `CLEAR_RUN` nach gestoppter Timelapse auch deren Zustand, Intervall und Zähler. Dies gilt nach `STOP_TIMELAPSE` ebenso wie nach `ESTOP`, auch wenn der interne Panoramaablauf bereits `IDLE` ist. Bestätigung ist `CLEARED`, gefolgt von aktuellem `STATUS` mit `IDLE`. Bei aktiver Timelapse wird `CLEAR_RUN` mit `INVALID_STATE` abgelehnt; zuvor stoppen. Auf dem Nano R4 sind Intervallbetrieb, Weiterlaufen bei getrennter Pixel-App, erneute Übernahme und Stop am 3. Oktober 2026 mit zehn echten Fotos bestätigt. Das Löschen nach Stop und Not-Stop ist auf Firmware 2.3.2 zusätzlich geprüft; elektrische Pulsmessung und Dauerlauf bleiben offen.

## 5. Panoramaauftrag

SET_TILT_ZERO/RESET_TILT_ZERO sind nur in IDLE ohne manuelle Fahrt, Trigger oder Timelapse zulässig. Bei Firmware vor 2.4.0 muss die Motorfreigabe aus sein; ab 2.4.0 ist zusätzlich bestätigtes ruhiges Halten erlaubt, das vor der Änderung deaktiviert wird. Die Istposition muss innerhalb der bestätigten Grenzen liegen. Sie erzeugen keine Fahrt und erweitern den physischen Fahrbereich nicht. Der Nano R4 speichert den geprüften Nullpunktdatensatz in EEPROM ab Adresse 16 (Tilt-Grenzen 0..15, Nullpunkt 16..27, Neustartkennung ab 32 und 48). Bei Speicherfehler wird die alte Referenz im Hardwareadapter wiederhergestellt. Ein identischer Retry setzt den Nullpunkt nicht erneut.

Die Android-App leert den lokalen Plan nach bestätigter Nullpunktänderung und fragt das Supportbundle erneut ab, damit auch nach einer wiederholten ACK-Antwort die aktuelle Referenz und Grenzen vorliegen. Ein Vergleich der gespeicherten Referenz je Gerät erkennt Änderungen auch beim späteren Reconnect. Gespeicherte Aufnahmen bleiben in ihrer damaligen Referenz erhalten.

`LOAD_PLAN` überträgt genau diese 16 Felder:

1. `runId` – 1 bis 4294967295
2. `planVersion` – 1 für Raster; 2 für sphärische Aufträge
3. `moveMode` – 0 (Spalten mit Rücklauf), 1 (sphärische Tilt-Bänder)
4. `startPanMdeg`
5. `startTiltMdeg`
6. `columns` – 1 bis 65535
7. `rows` – 1 bis 65535
8. `stepPanMdeg`
9. `stepTiltMdeg`
10. `settleTimeMs` – 0 bis 600000
11. `triggerPulseMs` – 1 bis 60000
12. `exposureWaitMs` – 0 bis 3600000
13. `panSpeedPermille` – 1 bis 1000
14. `tiltSpeedPermille` – 1 bis 1000
15. `disconnectPolicy` – 0 `CONTINUE`, 1 `ESTOP_ON_TIMEOUT`
16. `heartbeatTimeoutMs` – bei Policy 1 zwischen 500 und 60000, sonst 0

Bei `planVersion=2` und `moveMode=1` folgen Feld 17 `endPanMdeg` und Feld 18 `endTiltMdeg`. `rows` beschreibt die Zahl der Tilt-Bänder, `columns` die maximale Bildzahl eines Bandes. Die Tiltpositionen werden zwischen Start und Ende ganzzahlig interpoliert; die Zahl der Panbilder pro Band folgt der ganzzahligen Kosinusnäherung aus `SphericalGeometry`. Ein voller 360°-Kreis enthält keinen doppelten Endpunkt. Client und Firmware verwenden dasselbe Rezept; die Gesamtbildzahl ist die Summe aller Bänder und höchstens 4000.

Bild `i` wird ohne Positionsliste berechnet:

```text
column = i / rows
row    = i % rows
pan    = startPan  + column * stepPan
tilt   = startTilt + row    * stepTilt
```

Die Firmware akzeptiert den Plan erst, wenn alle Extremwerte innerhalb ihrer bestätigten Pan- und Tilt-Hartgrenzen liegen. `SAFETY_LIMITS_CONFIRMED=0` sperrt Plan- und manuelle Fahrbefehle fail-closed.

## 6. Zustände und Pause

Hauptzustände: `IDLE`, `PLAN_READY`, `RUNNING`, `PAUSE_PENDING`, `PAUSED`, `STOPPED`, `COMPLETED`, `FAULT`.

Phasen: `NONE`, `SELECT_TARGET`, `MOVING`, `SETTLING`, `TRIGGER_ACTIVE`, `EXPOSURE_WAIT`, `COMMIT_IMAGE`.

Eine Pause zwischen Bildern wirkt sofort. Eine Pause innerhalb eines Bildschritts wird zu `PAUSE_PENDING`: Trigger und konfigurierte Belichtungswartezeit werden beendet, das Bild wird genau einmal in `COMMIT_IMAGE` verbucht, anschließend werden die Motoren abgeschaltet und der Zustand wird `PAUSED`.

Im vollständig erreichten Zustand `PAUSED` darf ein Client sichere Positionen mit `MANUAL_MOVE` anfahren und mit `TRIGGER_ONCE` zusätzliche Bilder auslösen. Die Firmware hält den autonomen Rastercursor dabei unverändert. `RESUME` ist erst zulässig, wenn keine manuelle Bewegung und kein Trigger mehr aktiv ist. `REWIND_COLUMNS` wirkt in `PAUSED` sofort (`REWOUND`). Während `RUNNING` oder `PAUSE_PENDING` wird der Rücksprung mit `REWIND_PENDING` bestätigt und erst nach dem vollständig verbuchten aktuellen Bild angewendet; mehrere bis dahin eingehende Klicks werden zu einem Rücksprung zusammengefasst. Das Raster läuft danach automatisch weiter beziehungsweise bleibt bei angeforderter Pause pausiert. Der Cursor springt auf Zeile 0 der früheren Spalte und eröffnet eine neue `planRevision`; die monoton steigende `captureSequence` wird nicht zurückgesetzt. Dadurch bleiben wiederholte Rasterpositionen als eigenständige Aufnahmen unterscheidbar.

`FORWARD_COLUMNS` verwendet dieselben Laufzustände und die gleiche Bildgrenze. Der Befehl springt von der aktuellen Spalte auf Zeile 0 einer späteren Spalte. Während eines aktiven Bildes wird das Bild zuerst vollständig verbucht; mehrere Vorwärtsbefehle werden bis dahin zusammengefasst. In `PAUSED` wirkt der Sprung sofort. Ein Sprung über die letzte Spalte hinaus wird mit `LAST_COLUMN` abgelehnt, und ein Gegenrichtungsbefehl während eines vorgemerkten Sprungs mit `NAVIGATION_PENDING`. Jeder angewendete Sprung eröffnet ein neues `RUN_SEGMENT` mit neuer `planRevision`; der Aufnahmezähler bleibt monoton. Insgesamt sind pro Raster vier Richtungswechsel möglich (`SEGMENT_LIMIT` danach).

`UPDATE_TIMING|runId|settleTimeMs|triggerPulseMs|exposureWaitMs` ändert die drei Zeitwerte eines geladenen Plans. In `PLAN_READY` und `PAUSED` wirkt der Befehl sofort (`TIMING_UPDATED`). Während eines laufenden Bildes bestätigt die Firmware `TIMING_QUEUED` und übernimmt den letzten vollständig übertragenen Wertesatz erst nach `COMMIT_IMAGE`; damit werden ein aktiver Triggerpuls und die anschließende Belichtungswartezeit nicht nachträglich verkürzt oder verlängert. Vor der nächsten Zielwahl sendet sie den aktualisierten `PLAN` erneut. Die Grenzwerte entsprechen `LOAD_PLAN`: 0–600.000 ms Beruhigung, 1–60.000 ms Triggerpuls, 0–3.600.000 ms Belichtungswartezeit. Bei deaktivierter PC-Option „Control Camera Trigger Duration“ sendet PanoramaControl weiterhin 30 ms Triggerpuls.

Ab Firmware 2.3.6 kündigt `NAVIGATION_CAPABILITIES|1` zusätzliche manuelle Fahrzustände an: `PLAN_READY`, `STOPPED` und `COMPLETED`. `MANUAL_MOVE` verändert dabei weder Laufzustand noch Rezept, Cursor, Revision oder Aufnahmezähler. `PAUSED` und `IDLE` bleiben wie bisher fahrbar. In `RUNNING`, `PAUSE_PENDING`, aktiver Timelapse und `FAULT` wird der Befehl abgelehnt; ein abgelehnter Pfeilbefehl führt keinen Stop oder Clear aus. `START`, `RESUME` und `LOAD_PLAN` sind während einer manuellen Fahrt gesperrt. Ältere Firmware kündigt die Fähigkeit nicht an; der Android-Client gibt dort nur die bisherigen Fahrzustände frei.

Ab Firmware 2.3.7 werden beide Encoderabfragen vor Befehlsverarbeitung und Motorausgabe auf I²C-ACK und vollständige Winkelbytes geprüft. Fehlende Rückmeldung führt zu `FAULT_PAN_SENSOR` oder `FAULT_TILT_SENSOR` und stoppt beide Motoren sowie Kamera; Wiederanschließen setzt die Fahrt nicht fort. Der letzte gültige Istwinkel bleibt in STATUS erhalten, der Fehlerzustand kennzeichnet ihn als nicht aktuell verwendbar. Ohne gültige Rückmeldung bleibt Bewegung auch nach CLEAR_RUN gesperrt.

### 6.1 Verbindliche Reihenfolge vor jeder Aufnahme

Ab Firmware 2.4.0 gilt die Profilregelung aus Abschnitt 4: frische Encoderwerte, abgeschlossenes Profil, Ruhefenster beider Achsen und gemeinsame kontinuierliche Bestätigungs-/Settlingzeit. Eine unveränderte Achse behält ihren Haltezustand. Der Bewegungs-Timeout bleibt 120.000 ms ab Fahrbeginn; engere Ruhefenster sind vorläufig und am Aufbau zu prüfen.

Die folgende Kette dokumentiert Firmware 2.3.7 und früher:

1. `SELECT_TARGET` setzt das neue Pan-/Tilt-Ziel und setzt die Ankunftsbestätigung ausdrücklich zurück.
2. Erst nachdem beide Encoder für dieses Ziel frische Werte geliefert haben, werden Ankunftssamples gezählt.
3. Beide Achsen müssen fünf aufeinanderfolgende Messzyklen innerhalb der bestehenden Fehler- und Verlaufsgrenzen liegen.
4. Danach beginnt `SETTLING` für die vom Client als `settleTimeMs` übertragene Beruhigungszeit (`VibrationDelay` in PanoramaControl).
5. Während der gesamten Beruhigungszeit wird die Zielposition weiter geprüft. Drift oder Nachschwingen führt zurück zu `MOVING`.
6. Erst wenn die komplette Beruhigungszeit stabil abgelaufen ist, beginnt `TRIGGER_ACTIVE`.

Im historischen Regler betrug der aktuelle Fehler höchstens 1,5 Grad und die Fehlerverlaufsgrenze 10 Grad². Diese Bedingungen werden im neuen MotionController durch Profilabschluss und zeitbezogene Ruhefenster ersetzt; die alten MSE-/PWM-Abschaltschwellen sind dort nicht aktiv.

### 6.2 Debugverhalten ohne Timingverfälschung

`GET_DIAGNOSTICS` liefert einen CRC-geschützten Snapshot und startet keinen kontinuierlichen Datenstrom; ab 2.4.0 folgt die optionale Meldung MOTION_DIAGNOSTICS. PanoramaControl fragt ihn nach dem Handshake einmal ab und zeigt eine kompakte Zeile im vorhandenen Kommentarbereich an. Weitere Clients dürfen ihn bei Bedarf abfragen. Der frühere Arduino-Mega-Debugbuild hatte einen standardmäßig deaktivierten USB-CSV-Strom; der neue R4-Sketch verwendet die begrenzte Protokollausgabe.

`currentFreeRamBytes` ist der beim Aufbau des Diagnoseframes aktuell freie Abstand zwischen Heapende und Stack. `minimumFreeRamBytes` ist der konservative Tiefstwert seit Ende der Firmwareinitialisierung. Dafür markiert die Firmware den damals freien SRAM einmal mit `0xA5` und zählt bei jeder Diagnoseabfrage den noch unberührten Bereich; ein Sicherheitsabstand von 16 Byte wird nicht mitgezählt. Ein Reset beginnt eine neue Messung. Firmware 2.1.0/2.1.1 sendet nur die ersten 19 Felder; PanoramaControl 27.2 akzeptiert beide Varianten.

Der Nano R4 hält die letzten 64 Schlüsselereignisse in einem festen Ring; der Host-Debugbuild behält drei für seine Tests. Der alte AVR-Nano-Build verzichtet auf den Ring. Der Ring überbrückt nur begrenzte Verbindungsunterbrechungen; für einen langen autonomen Lauf liefern `STATUS`, Plan und Segmente den aktuellen Laufzustand, aber keinen vollständigen historischen Istpositionsverlauf. Die beiden bisherigen freien-RAM-Felder melden auf dem R4 derzeit 0 für „nicht verfügbar“, bis ein ARM-Laufzeitmonitor implementiert ist.

| Code | Ereignis | Code | Ereignis |
|---:|---|---:|---|
| 0 | `BOOT` | 10 | `PAUSED` |
| 1 | `SESSION_OPENED` | 11 | `RESUMED` |
| 2 | `PLAN_LOADED` | 12 | `REWOUND` |
| 3 | `RUN_STARTED` | 13 | `MANUAL_MOVE_STARTED` |
| 4 | `TARGET_SELECTED` | 14 | `MANUAL_MOVE_REACHED` |
| 5 | `TARGET_REACHED` | 15 | `MANUAL_TRIGGERED` |
| 6 | `SETTLING_LOST` | 16 | `STOPPED` |
| 7 | `TRIGGER_STARTED` | 17 | `COMPLETED` |
| 8 | `IMAGE_COMMITTED` | 18 | `FAULT` |
| 9 | `PAUSE_PENDING` |  |  |

PanoramaControl schreibt den vollständigen gesendeten und empfangenen V2-Zeilenverkehr einschließlich CRC, Retry-Angabe, UTC-Zeit und Clientmeldungen automatisch nach `%LOCALAPPDATA%\PanoramaControl\SupportLogs\PanoramaSupport_*.log`. Die Datei ist auf 5 MiB begrenzt und wird nach jeder Zeile gespült. Bei `COMPLETED`, `STOPPED` oder `FAULT` wird automatisch ein Supportbundle angefordert. Das Protokoll enthält keine Zugangsdaten.

## 7. Verbindungsverlust und Ereignisse

Ab Firmware 2.3.4 folgt auf `DEVICE_INFO` ein separates `DEVICE_SESSION|bootId`. Auf dem Nano R4 erhöht sich die in EEPROM gespeicherte Kennung bei jedem Firmware-Start; wiederholtes HELLO und Bluetooth-Reconnect behalten sie. Zwei geprüfte Speichersätze liegen ab EEPROM-Adresse 32 und 48, getrennt von den Tilt-Grenzen ab Adresse 0. Ohne gültig zurückgelesene Kennung wird 0 gemeldet. Der bisherige Aufbau von DEVICE_INFO bleibt erhalten.

Bleibt das Bluetooth-Modul beim MCU-Neustart verbunden, sendet die Firmware beim ersten HEARTBEAT der neuen Sitzung ebenfalls Geräteinfo, Kennung, Grenzen und Status. Die Android-App verwirft bei einer geänderten Kennung den lokalen Planentwurf. Bei einem Neustart innerhalb der laufenden Verbindung verwirft sie außerdem die alte Befehlssitzung und verbindet erneut; ausstehende Aktionen werden nicht in diese neue Verbindung übernommen. Dieselbe Kennung bei Reconnect erhält den Entwurf. Bei Kennung 0 wird der Entwurf vorsichtshalber einmal je neuer Verbindung geleert.

- `CONTINUE`: Der Controller führt den gespeicherten Auftrag weiter aus.
- `ESTOP_ON_TIMEOUT`: Bleibt der Heartbeat länger als konfiguriert aus, verwendet die Firmware dieselbe zentrale Abschaltung wie `ESTOP` und meldet `CONNECTION_TIMEOUT`.
- Live-Ereignisse enthalten Soll- und damalige Istposition.
- Nach einer Verbindungslücke rekonstruiert der Client fehlende Bildereignisse anhand von `PLAN`, `RUN_SEGMENT` und `completedImages`. Diese Ereignisse tragen `Reconstructed=true` und verwenden mangels historischem Sensorspeicher die Sollposition.
- Manuelle Aufnahmen benötigen einen verbundenen Client und werden als `MANUAL_IMAGE_TRIGGERED` sowie im PC-Supportlog erfasst. Beim Nano R4 kann der Ereignisring einen Verbindungsverlust genau während des Triggerpulses nachweisen, solange das Ereignis nicht aus dem 64er-Ring verdrängt wurde. Der aktuelle Status zeigt außerdem den fortgeschriebenen Zähler.
- Dadurch bleiben typische Rasterläufe mit bis zu 4.000 Bildern auf dem unveränderten Arduino Nano logisch rekonstruierbar; historische Istwerte jedes während einer Trennung aufgenommenen Bildes würden zusätzlichen nichtflüchtigen Speicher erfordern.

PanoramaControl verwendet standardmäßig `CONTINUE`. Die optionale Sicherung wird ohne UI-Änderung in `PanoControlConfig.xml` aktiviert:

```xml
<StopOnConnectionLoss>true</StopOnConnectionLoss>
<ConnectionTimeoutMs>5000</ConnectionTimeoutMs>
```

## 8. Bilddatei- und PapyWizard-Zuordnung

Die Kamera liefert im aktuellen Aufbau weder eine Erfolgsbestätigung noch den Dateinamen. Deshalb bedeutet ein Bildereignis technisch: Der Triggerpuls wurde ausgegeben und die konfigurierte Wartezeit ist beendet. PanoramaControl vergibt für jede tatsächliche Auslösung eine lückenlose `picture_id` ab 0 und schreibt:

- den weiterhin PapyWizard-kompatiblen XML-Eintrag mit dieser `picture_id`;
- eine parallele `CaptureManifest*.csv` mit `camera_file_order`, stabilem `capture_key`, Lauf-, Ereignis-, Raster- und Revisionsnummer, Aufnahmeart, Herkunft der Position, Gerätezeit sowie Soll- und Istwinkeln im bisherigen UI-/PapyWizard-Koordinatenvertrag.

XML und Manifest tragen denselben millisekundengenauen Laufstempel im Dateinamen und sind dadurch als Paar erkennbar.

Kameradateien werden nach ihrer chronologischen Aufnahmereihenfolge der Spalte `camera_file_order` (ab 1) zugeordnet. Rasterwiederholungen und manuelle Bilder erhalten jeweils eine eigene Zeile. Ein Retry oder ein beim Reconnect erneut gesendetes Ereignis wird über `capture_key` dedupliziert. Ohne eine spätere Kameraanbindung kann die Software keinen vom Kamerasystem vergebenen Dateinamen beweisen; diese Grenze wird im Manifest nicht verschleiert.

## 9. Fehlercodes

Implementiert sind mindestens: `BAD_FIELD_COUNT`, `VALUE_OUT_OF_RANGE`, `INVALID_CRC`, `CRC_ERROR`, `FRAME_TOO_LONG`, `UNSUPPORTED_VERSION`, `UNSUPPORTED_MESSAGE`, `SEQUENCE_CONFLICT`, `INVALID_STATE`, `RUN_ID_MISMATCH`, `SAFETY_UNCONFIRMED`, `PAN_LIMIT`, `TILT_LIMIT`, `TARGET_REJECTED`, `MOVEMENT_TIMEOUT` und `CONNECTION_TIMEOUT`.

## 10. Sicherheitsfreigabe

Die in `SafetyConfiguration.h` eingetragenen Tiltwerte sind Platzhalter und ausdrücklich **nicht freigegeben**. Vor einer realen Fahrt müssen obere und untere Kollisionsgrenze mit montiertem Objektiv und Stecker, Nachlaufweg und Sicherheitsabstand vermessen werden. Erst danach dürfen die Werte ersetzt und `SAFETY_LIMITS_CONFIRMED` auf 1 gesetzt werden.

## 11. Begrenzte Antriebsmessung (nur Nano-R4-USB-Testbuild)

Die normale Bluetooth-Firmware unterstützt keine unregulierten Messfahrten. Der explizite USB-Testbuild veröffentlicht ab 2.4.10 `BENCH_CAPABILITIES|3|40|3000|5000|20|192|5000|500|500`: Messschema, maximale absolute PWM, Zeitgrenze ms, Weggrenze mdeg, Abtastperiode ms, Pufferkapazität, Limitreserve mdeg, Zeitgrenze ms für absolute PWM über 30, erforderlicher Anlauffortschritt mdeg. Die Werte sind feste Grenzen, keine Motorskalierung.

- `BENCH_PULSE|axis|signedPwm|durationMs|travelMdeg` fährt genau eine Achse (0 Pan, 1 Tilt), ohne Positionsreglertorque. Nur IDLE ohne Auftrag, Kameraaktion oder aktive Motorfreigabe; frische ruhende Encoderhistorie und bestätigte Limits erforderlich. Maximal ±40/255, 3 s, 5°; bei absoluter PWM über 30 höchstens 500 ms. Der Pulsendpunkt muss 5° Abstand zu den Limits lassen. Leistungsabschaltung begrenzt den angetriebenen Weg; mechanische Weiterbewegung wird zusätzlich aufgezeichnet.
- `BENCH_CRUISE|axis|signedRunningPwm|durationMs|travelMdeg` verwendet dieselben Freigaben, Weg- und Limitgrenzen. Zuerst wird mit höchstens ±40 angefahren, bis 0,5° Ausgangsfortschritt in Fahrtrichtung erreicht sind; dann folgt ohne Zwischenstop eine begrenzte Rampe auf die gewünschte laufende PWM (±1 bis ±30). Gesamtzeit 600–3000 ms, angetriebener Gesamtweg 1000–5000 mdeg, einschließlich Anlauf. Kein Anlauffortschritt nach 500 ms führt zu `START_TIMEOUT`; Gegenbewegung von 500 mdeg zu `WRONG_DIRECTION`. Die Ausgangsleistung liegt spätestens nach 500 ms bei höchstens ±30, auch während der Abwärtsrampe. Keine automatische Wiederholung eines fehlgeschlagenen Anlaufs.
- `GET_BENCH_STATUS` liefert `BENCH_STATUS|count|active|recording|reason`. Zeit-/Weggrenze, Sensorverlust, Taktlücke, Stop, USB-Verlust und Hostheartbeatverlust nehmen Leistung weg. Nach Abschalten werden 500 ms Encoderbewegung weiter gepuffert. Währenddessen sind normale Fahr-, Kamera-, Plan- und Referenzänderungen gesperrt; Stop und Diagnose bleiben möglich.
- `GET_BENCH_TRACE|startIndex` liefert nach Aufzeichnungsende bis zu acht Frames `BENCH_SAMPLE|index|elapsedMs|panMdeg|tiltMdeg|requestedPwm|stage`. Stage: 0 konstanter Puls, 1 Anlauf, 2 Weiterfahrt, 3 stromlose Nachbeobachtung. Gerätezeit statt späterer Übertragungszeit verwenden. PWM ist die interne Anforderung, keine Messung der Motorspannung. Messschema 2 hatte noch kein Stagefeld.
- `GET_BENCH_OUTPUT` liefert `BENCH_OUTPUT|panAppliedPwm|tiltAppliedPwm|tiltPositivePin|tiltNegativePin|tiltPeriodCounts|tiltDutyCounts|tiltTimerCounter|tiltPeripheralMux`. Dies prüft Treiberargument, Richtungspinpegel, PWM-Pinzuordnung und Timeraktivität. Der Dutywert stammt aus dem Treiber, er beweist weder Spannung noch Strom am Motor.

Jeder Befehl erhält ACK/NACK. Ein wiederholter Pulsbefehl mit gleicher Sequenz startet keinen weiteren Puls. Nicht unterstützte Adapter antworten `BENCH_UNSUPPORTED`. Der Messzugang gehört nicht zur Android-Bedienoberfläche.

## 12. Aufnahme-Stabilität und erhaltener Auftrag ab Firmware 2.4.71

`CAPTURE_RECOVERY_CAPABILITIES|1` kündigt die quittierbare Problempause und Spaltenvorbereitung an. `CAPTURE_STABILITY_SETTINGS|spanMdeg|panDriftMdeg|tiltDriftMdeg|extraWaitMs|configRevision` meldet die tatsächlich wirksamen Werte bei HELLO/Supportdaten und nach einer Übernahme. `GET_CAPTURE_STABILITY` ohne Felder fordert sie erneut an.

`SET_CAPTURE_STABILITY|spanMdeg|panDriftMdeg|tiltDriftMdeg|extraWaitMs|baseRevision` speichert alle vier Werte atomar. Bereiche: Span und Pan 44–1000 mdeg, Tilt 44–1500 mdeg, zusätzliche passive Wartezeit 1000–120000 ms. Vorgaben sind 250/250/400 mdeg und 15000 ms. Die Basisrevision muss aktuell und kleiner als UINT32_MAX sein. Eine offene vollständige Config-Transaktion verhindert die Übernahme. Ungültige Teile oder Speicherfehler ändern keine aktiven Werte. Identische Werte verursachen keinen weiteren Speicherzyklus. Erfolg: `ACK|commandSeq|STABILITY_SETTINGS_SAVED`, danach bestätigte Werte und Revision. Das vorhandene Schema 1 umfasst nun 129 Parameter; der neue Parameter wird am Ende ergänzt, ältere gespeicherte Präfixe bleiben erhalten.

Schreiben ist mit deaktivierten Motoren und Kamera ohne Auftrag in IDLE/STOPPED oder mit erhaltenem Auftrag in PAUSED möglich. Manuelle Fahrt, Jog, Trigger, Timelapse und Benchbetrieb sperren es. Die Hardware verlangt zusätzlich frische ruhende Rückmeldung. Dies lockert ausschließlich den Zugang zu diesen Aufnahmegrenzen; die vollständige Motor-Konfiguration bleibt bei geladenem Auftrag gesperrt.

Vor Auslösung und während Trigger/Belichtungswartezeit/Verbuchung gelten identische Stabilitätsbedingungen. Nach dem Abschalten muss die volle geplante Beruhigungszeit streng bestätigt sein. Die zusätzliche Wartezeit verlängert keine Motorfahrt. Fehlt die Freigabe bis Beruhigungszeit plus extraWaitMs, pausiert der Auftrag mit `lastError=CAPTURE_NOT_STABLE`, ohne Bildzählung und ohne Auslösung.

`EXPOSURE_UNSTABLE`, `MOTION_STALLED`, `MOTION_SETTLE_TIMEOUT`, `MOTION_FINE_TIMEOUT` und `MOVEMENT_TIMEOUT` pausieren einen laufenden Auftrag bei weiterhin gültigen Grenzen ebenfalls: STATUS meldet PAUSED/NONE mit dem konkreten Fehler. Motoren und Kamera sind aus, Run-ID, Plan, Aufnahmezähler und nächster Bildindex bleiben erhalten. Eine möglicherweise schon ausgelöste, instabile Aufnahme wird nicht gezählt. Sensor-/Grenz-/Kontrollfehler und Not-Stop behalten die harte Abschaltung. Es gibt keinen automatischen RESUME.

`RETRY_COLUMN|runId|previous` bereitet im pausierten Raster eine Wiederholung vor: previous=0 beginnt am Anfang der aktuellen Fortsetzungsspalte, previous=1 eine Spalte davor (an Spalte 0 begrenzt). Es gelten dieselbe Run-ID, gültiger Bildindex und keine manuelle Aktion. Die Planrevision steigt; der Aufnahmezähler sinkt nicht. Erfolg `COLUMN_RETRY_READY` plus STATUS. Der Auftrag bleibt PAUSED und fährt erst nach separatem `RESUME|runId`. Wiederholungen ohne bisherige Bilder im aktuellen Segment verbrauchen keinen zusätzlichen Segmentplatz. Der R4 hat 64 Segmentplätze, der klassische Nano weiterhin fünf. Ältere Clients können einen bestätigten PAUSED-Auftrag weiterhin mit dem bestehenden RESUME fortsetzen; unbekannte zusätzliche Fähigkeitsframes ändern die Protokollversion 2 nicht.
