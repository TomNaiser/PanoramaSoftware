# 07 — Variablen und Parameter

[Inhalt](README.md) · [Vollständiger Motorparameterkatalog](referenz/motorparameter.md)

Die Tabellen beschreiben fachlich wichtige Zustandsvariablen und Schnittstellen. Lokale Schleifenzähler und generierte GUI-Controls werden nicht als stabile öffentliche API behandelt. Die aktive Dateistruktur und Typen stehen in der [Quellcode-Referenz](referenz/quellcode.md).

## Einheitensystem

| Konvention | Bedeutung / Beispiel |
|---|---|
| `*Mdeg`, `mdeg` | Milligrad; `-42500` entspricht −42,5° |
| `*Ms` | Millisekunden; `500` entspricht 0,5 s |
| `*Us` | Mikrosekunden, besonders I2C-Budget |
| `*Dps`, Geschwindigkeit | Grad/s im Regler, auf dem Draht je nach Meldung Milligrad/s |
| `*Permille`, Tempo | 1.000 entspricht der konfigurierten Grundgeschwindigkeit; Lauftempo bis 2.000 nur mit Fähigkeit und eigener Obergrenze |
| `signedPwm` | Vorzeichenbehaftete Antriebsgröße auf bestehender 8-Bit-Skala; z. B. 40 sind 40 PWM-Zähler, nicht 40 % |
| Parameterwert / Skalierung | Festkomma: fachlicher Wert = Ganzzahl / `scale`; Rohwert 40.000 bei scale 1.000 bedeutet PWM 40 |
| Bild-/Spalten-/Reihenindex | Im Protokoll und Rezept intern überwiegend nullbasiert; Capture-Sequenz beginnt bei regulären Aufnahmen mit 1 |
| Host-Zeit / Gerätezeit | UTC-Empfangszeit ist nicht Firmware-`millis()` und nicht garantiert die tatsächliche Kamera-Auslösezeit |

## Firmware: Hardware und Regelzustand

| Variable / Typ | Ort | Bedeutung |
|---|---|---|
| `PanAxisSensorAvailable`, `TiltAxisSensorAvailable` | Sketch | Gültigkeit des aktuellen Sensorzugriffs, keine Positionsfehlergrenze |
| `AbsPos_x`, `AbsPos_y` | Sketch | Entrollte Encoderposition in Grad; Tilt-Rohreferenz vor UI-Nullpunkt |
| `PreviousPosition_x/y`, `TotalOffset_x/y` | Sketch | Vorherige Lage und Ganzumdrehungs-Offset für Winkelentrollung |
| `TargetPos_x/y` | Sketch | Hardware-Ziel in Grad; kein Nachweis der Ankunft |
| `lastPanDrive`, `lastTiltDrive` | Sketch | Zuletzt ausgegebener Antrieb unter Berücksichtigung der Motorpolarität |
| `encoderReadError[2]`, `encoderReadFailures[2]` | Trace-/Host-Build | Lesefehlercode und Fehlerzahl pro Achse; nicht in jedem Feld-Build als Detail sichtbar |
| `encoderBusRecoveries`, `encoderRecoveryBurst` | R4 / Host | Begrenzte I2C-Recovery und Burstüberwachung |
| `AxisMotionStatus.position/velocity` | `MotionController.h` | Beobachtete Position/Geschwindigkeit |
| `target`, `reference`, `referenceVelocity` | `AxisMotionStatus` | Endziel und zeitabhängige Referenzwerte |
| `signedPwm`, `phase`, `driveStage` | `AxisMotionStatus` | Aktorausgabe, Achsphase und gestufter Antriebszustand |
| `withinTolerance`, `quiet` | `AxisMotionStatus` | Getrennte Lage- und Ruheaussage |
| `_axes[2]`, `output`, `integral` | `MotionController` | Pan-/Tilt-Reglerzustände, Leistung und Integralanteil |
| `_targetMissedAxes` | `MotionController` | Zielverfehlung achsweise merken, ohne gesunde andere Achse vorzeitig zu stoppen |
| `_lastSampleMs`, `_sampleSequence` | `MotionController` | Aktualität und Zuordnung der Reglerbeobachtung |

## Firmware: Auftrag, Sicherheit und Konfiguration

| Variable / Typ | Bedeutung und Lebensdauer |
|---|---|
| `PanoramaPlanV2` | Gesamtes Rezept; [alle übertragenen Felder](06-protokoll-und-datenmodell.md) |
| `TargetPositionV2.imageIndex/column/row/panMdeg/tiltMdeg` | Aktuell ausgewählter Punkt, nicht langfristige Bilddateizuordnung |
| `PanoramaExecutor._state/_phase` | Globaler Ablaufzustand und aktuelle Aufnahmephase |
| `_nextImageIndex` | Nächster noch nicht bestätigter Zielindex |
| `_captureCount` | Bestätigte reguläre Captures einschließlich echter Wiederholungen |
| `_retriedImageIndex` | An welchem Punkt der einmalige automatische Bewegungs-Retry bereits verbraucht wurde |
| `_planRevision`, `_segments`, `_segmentCount` | Historie von Planabschnitten nach Sprung/Wiederholung; RAM-begrenzt |
| `_movementStartedMs`, `_incrementalMoveBudgetMs` | Laufender Fahrtbeginn und aktives Zeitbudget; erneute Beruhigung setzt den Beginn nicht beliebig zurück |
| `_passiveSettling`, `_captureReady` | Motorfahrt beendet; passive Stabilität und Aufnahmefreigabe |
| `_capturePanMdeg/_captureTiltMdeg` | Gesicherte Ist-/Prüfposition für den aktuellen Aufnahmeablauf |
| `_timingPending`, `_pendingSettleMs/...` | Vorgemerkte Zeiten, die erst an einer zulässigen Bildgrenze aktiv werden |
| `SafetyController._motionEnabled` | Zentrale Bewegungsfreigabe; Stop deaktiviert sie |
| `_tiltMinimumMdeg/_tiltMaximumMdeg` | Gespeicherte physische Tilt-Rohgrenzen |
| `_tiltZeroOffsetMdeg` | Benutzerreferenz; wird von wirksamen Koordinaten abgezogen |
| `_bootId` | Persistente Bootfolge; Reconnect und echter Neustart unterscheiden |
| `V2Controller._jogToken/_jogUpdatedMs` | Besitzer/Lease der gehaltenen Fahrt |
| `_lastHeartbeatMs`, `_sessionActive` | V2-Sitzungsüberwachung; Fristwirkung abhängig von Auftragspolicy |
| `MotionSettings.values[129]` | Aktive Festkommakonfiguration in Indexreihenfolge des Katalogs |
| `StoredMotionConfig.schema/count/revision/checksum` | Versionierung und Integrität eines vollständigen EEPROM-Slots |

## Android

| Variable / Modell | Bedeutung |
|---|---|
| `DeviceState.state`, `phase`, `lastError` | Letzter bestätigter Firmware-Ablauf; nicht aus UI-Schleifenzählern berechnet |
| `runId`, `completed`, `total`, `nextImageIndex`, `revision` | Auftragskennung, Capturezahl, Plangröße, Fortsetzungspunkt, Planrevision |
| `pan`, `tilt`, `panMin/panMax`, `tiltMin/tiltMax` | Ist und gemeldete Grenzen in mdeg |
| `lastStatusMs`, `lastManualStatusMs` | Empfangszeit für Frischeprüfung |
| `plan`, `tiltZeroOffset`, `tiltCalibrationKnown` | Aktuelles Geräte-Rezept und bestätigte Tilt-Referenz |
| `runMaximumTempo`, `jogSupported`, `manualStatusSupported` | Bestätigte optionale Fähigkeiten; initial keine unbegrenzte Freigabe |
| `captureStability.spanMdeg/panDriftMdeg/tiltDriftMdeg/extraWaitMs/revision` | Aktive gespeicherte Stabilitätsgrenzen, unabhängig von Warnschwellen |
| `PanoramaService.busy/uncertain/recovering` | Befehl offen, Wirkung unklar, kontrollierte Wiederherstellung aktiv |
| `manualMoving/manualTrigger/manualPreparing` | Manuelle Aktion noch nicht abgeschlossen oder vorbereitet |
| `IncidentTracker` | Identität, Anlass und Quittierungsstand; ein neuer Anlass darf keine alte Quittierung erben |
| `CaptureStore.Checkpoint.boot/tiltZero/raster` | Gesicherte Wiederaufnahmebasis; Abgleich vor Laden des Restauftrags |
| `RasterRecovery.source/nextIndex` | Originalrezept und unterbrochener Punkt; keine Neubestimmung aus verändertem Bildwinkel |
| `run_links.start_column` | Beginn des Kindlaufs; bei sphärisch Bandgrenze trotz historischem Feldnamen |
| `LivePapywizard.pending/scheduled/status` | Zusammengefasste Schreibanforderungen, Workerzustand und sichtbares Sicherungsergebnis |
| SharedPreference `qualityPause` | Zusätzliche Android-Pause bei bestätigter Istabweichung über 1°; optional, keine optikabhängige Überlappungsgarantie |

## Windows

| Variable / Eigenschaft | Bedeutung |
|---|---|
| `CurrentStatus`, `_activePlan`, `_runSegments` | Gemeldeter Zustand, übernommener Plan und Segmente im SessionController |
| `LastStatusUtc`, `LastManualStatusUtc`, `StatusFresh` | Bestätigte Empfangsfrische; reguläre Freigabe verlangt Alter unter 2.500 ms |
| `Features`, `Capabilities` | Verarbeitete Fähigkeiten und tatsächliche Grenzen |
| `BootId`, `BootKnown` | Neustart-/Sitzungszuordnung |
| `Incident`, `Uncertain`, `_recovering` | Vorfall, unklare Wirkung und laufende Prüfung |
| `ManualPanTempo`, `ManualTiltTempo` | Getrennte manuelle Tempoeingaben; nicht aktive Motor-PWM |
| `PendingTiming` | Vorgemerkte Änderung im Gegensatz zum bestätigten aktiven Plan |
| `ManualStatus`, `_manualPendingSequence`, `_manualDeadlineUtc` | Abschlussüberwachung der angenommenen manuellen Aktion |
| `_jogToken`, `_jogSequence`, `_jogTimer` | Windows-Haltefahrt und laufende Besitzerbestätigung |
| `RunEvent` | Ziel/Ist, Capture-Identität, manuell/regulär, rekonstruiert und Warnung |
| `RunArchive.stored/plans` | In-Memory-Indizes des dauerhaften Journals; nicht Ersatz für Dateischreiben |
| `PapyWizardRunExporter._nextPictureId/_exportedEvents/_open` | Exportreihenfolge, Replayfilter und Zustand des inkrementellen Writers |

## Wichtige feste Zeit- und Größenwerte

| Wert | Code-Default / Bedeutung |
|---|---|
| 20 ms / 100 ms | Firmware-Sampleperiode / maximal zulässige Samplelücke |
| 2.000 µs / 8.000 µs | I2C-Einzeltransfer / gemeinsames Recoverybudget auf R4 |
| 250 ms | Default kontinuierliche Ankunfts-Ruhebestätigung, einstellbar über Motorparameter |
| 120.000 ms | Globale maximale Fahrtbegrenzung; konkrete Teilfahrten können deutlich kürzere dynamische Budgets besitzen |
| 15.000 ms | Default zusätzliche passive Stabilitätswartezeit, konfigurierbar |
| 750 ms / drei Retries | Standard-ACK-Frist der Clients; erster Versuch kommt hinzu |
| 200 ms | Android-Heartbeatintervall nach Handshake |
| 500 ms | Firmware-Statusintervall; zusätzlich Windows-COM-Lese-/Schreibtimeout jeweils 500 ms |
| 5.000 ms | Android-Verbindungsmonitor bei ausgebliebenen Statusdaten; frühere Bedien-Freshness separat |
| 4.000 Bilder | Maximale geplante Auftragspunkte |
| 64 / 5 Segmente | R4 / alter Nano |
| 129 | Aktuelle Anzahl Motorparameter, nicht pauschal bei älterer Firmware voraussetzen |

## Motorparameter richtig lesen

Der [generierte Katalog](referenz/motorparameter.md) enthält alle 129 Deskriptoren exakt in Indexreihenfolge. Dort sind **Code-Defaults und zulässige Einzelwertbereiche** aufgeführt, keine automatisch vom angeschlossenen Kopf gelesenen Istwerte. Das am 27.10.2026 vollständig zurückgelesene reale Profil ist **Revision 40**: Pan-Ankunft 0,15°, Pan-Haltehysterese 0,25°, Tilt-Startbremsvorhalt 0,110 s, Tilt-Feinannäherung aus. Gegenüber Revision 39 sind nur die beiden Pan-Werte geändert. [Vollständiges gespeichertes Profil](../PanoramaAndroid/config/MotionConfig.precise-pan-2.4.75-20261027.json), [Messnachweis und Grenzen](validation/2026-10-27-pan-positioning.md). Die Dokumentationsprüfung selbst liest oder verändert den Kopf nicht.

Wichtige Gruppen sind `control.*` (Ruhe/Fortschritt/Leistungsreserve), `pan.*` und `tilt.*` (Regler/Fahrmodell), `manual.*` (Jog/Zielfahrt), `run.*` (Lauftempi/Rückfahrt), `panorama.*` (Zeitbudget) und `capture.*` (Warnung/Stabilität). Namen sind kein Ersatz für die tatsächliche Verwendung im Regler. Bei Änderung sind Querbedingungen in `MotionSettings::validate()` und die vollständige Revisionsprüfung zu beachten.
