# 10 — Build, Tests und Wartung

[Inhalt](README.md) · [GitHub und Dokumentationspflege](11-github-und-dokumentationspflege.md)

## Voraussetzungen und Checkout

```powershell
git clone --recurse-submodules <URL-des-Gesamtprojekts>
cd PanoramaSoftware
git submodule update --init --recursive
```

Android benötigt JDK 17 oder eine kompatible Android-Studio-JBR, Android SDK 35 und den enthaltenen Gradle-Wrapper (8.11.1, Android-Gradle-Plugin 8.10.1). Windows benötigt das .NET-8-SDK für Hosttests sowie .NET-Framework-4.8-Buildunterstützung für WinForms. Die C++-Hosttests sind als Visual-C++-Projekt mit Toolset v143 angelegt. Für R4 verwendet das vorhandene Skript Arduino CLI und `arduino:renesas_uno` 1.6.0 sowie die AS5048B-Sensorbibliothek.

Pfade in den folgenden Beispielen sind relativ zum Gesamtprojekt. Lokale SDK-/Toolpfade werden über Umgebung oder Skriptparameter gesetzt; die Dokumentation setzt keinen persönlichen Benutzerpfad voraus.

## Android ohne Hardware bauen und testen

```powershell
./PanoramaAndroid/gradlew.bat -p PanoramaAndroid testDebugUnitTest lintDebug assembleDebug
```

Ergebnis: `PanoramaAndroid/app/build/outputs/apk/debug/app-debug.apk`. `--offline` nur ergänzen, wenn Abhängigkeiten bereits lokal vorhanden sind. Build und Lint greifen nicht auf einen Panoramakopf zu. Die JVM-Tests prüfen unter anderem Rezeptgeometrie, Frische/Freigaben, Vorfälle, Restaufträge, Protokollverhalten und atomare Dateisicherung.

Für die Pixel-Integration **getrennte App-ID** verwenden, damit Tests keine produktive Datenbank ersetzen:

```powershell
./PanoramaAndroid/gradlew.bat -p PanoramaAndroid `
  -PvalidationApplicationId=de.tomnaiser.panoramaandroid.validation `
  assembleDebug assembleDebugAndroidTest

adb install -r PanoramaAndroid/app/build/outputs/apk/debug/app-debug.apk
adb install -r PanoramaAndroid/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e liveExportCheck true `
  de.tomnaiser.panoramaandroid.validation.test/de.tomnaiser.panoramaandroid.AppChecks
```

`liveExportCheck` verwendet echte SQLite-/Dateioperationen mit synthetischen Aufnahmen und öffnet keine Hardwareverbindung. Für einen kurzen gezielten Fall kann `-e liveExportIterations 10` ergänzt werden. Standard sind 1.000 Aktualisierungen. `restRecoveryCheck` prüft zusätzlich Service/Dialoge mit simuliertem Transport; dazu können entsperrtes Display und Berechtigungen der **Validation-App** nötig sein.

Einige andere Instrumentierungsflags führen ausdrücklich reale Fahrten/Auslösungen aus. Daher nicht beliebige `AppChecks`-Optionen als hardwarefrei voraussetzen. Instrumentierungsprozess-Exitcode allein reicht nicht: explizites `PASS` und Assertionausgabe prüfen.

Nach einem Validation-Build ist die `app-debug.apk` unter derselben Ausgabeadresse ein anderes Paket. Vor einer produktiven Installation erneut **ohne** `validationApplicationId` bauen und `output-metadata.json` auf Paketkennung, Version und Code prüfen. APK-Updates mit `install -r` ersetzen nicht absichtlich die App-Daten; Datenerhalt dennoch durch Sicherung und Vergleich prüfen. Ein Uninstall der produktiven App ist kein normales Update.

## Firmware

R4-Build, noch ohne Flash:

```powershell
pwsh -File PanoduinoControlGitRepo/Tests/build-r4.ps1
```

Das Skript erwartet standardmäßig `Tests/.r4-tools/`, Boardpaket und Sensorbibliotheken. Über `-ToolDirectory`, `-SensorLibraryRoot` und `-DriveLetter` anpassbar. Es kompiliert den regulären Bluetooth-Build, prüft Flash-/SRAM-Reserve und ruft die Boot-Stack-Prüfung auf. `-UsbControl`, `-MotionTraceUsb` und `-BufferedPanTrace` sind getrennte Diagnosevarianten; ein Diagnose-Build darf nicht stillschweigend als Feld-Firmware ausgegeben werden.

Hosttests in einer Visual-Studio-Developer-PowerShell mit installiertem C++-Toolset:

```powershell
MSBuild.exe PanoduinoControlGitRepo/Tests/FirmwareExecutorTests/FirmwareExecutorTests.vcxproj `
  /p:Configuration=Release /p:Platform=x64
```

Danach das gebaute `FirmwareExecutorTests.exe` aus dem im Build gemeldeten Ausgabeverzeichnis ausführen. FakeArduino/Wire und Modelle prüfen Zustand, Protokoll, Sicherheitsabbruch, Regelverhalten und injizierte Fehler. Sie senden keine Motorbefehle an einen realen Kopf.

Upload ist ein gesonderter Eingriff über `Tests/upload-r4.ps1 -Port <konkreter-Port>`. Das Skript prüft eine eindeutig passende R4-Kennung. Für diese Dokumentation wurde kein Upload ausgeführt. Ein echter Upload oder Hardwaretest erfordert vorher gesicherten Stillstand, richtigen Build und passenden Aufbau. Der alte AVR-Build hat ein eigenes `build-avr.ps1`; ein kompilierter AVR-Pfad ist keine aktuelle R4-Hardwareabnahme.

## Windows

Einzelne hardwarefreie Hostsuite vom Gesamtprojekt:

```powershell
dotnet run --project PanoramaControlGitRepo/Tests/ProtocolTests/ProtocolTests.csproj
```

Kompletter vorhandener Paketbuild:

```powershell
pwsh -File PanoramaControlGitRepo/Tests/build-windows.ps1 `
  -MotionProfile PanoramaAndroid/config/MotionConfig.precise-pan-2.4.75-20261027.json
```

Das Paketskript baut die .NET-Framework-App und führt simulierte WinForms-/Vorschau-/Navigations-/Touchprüfungen aus. Der gezeigte Aufruf aus dem Gesamtprojekt erzeugt `releases/27.5.4-pan-r40` und `releases/PanoramaControl-27.5.4-pan-r40.zip` im Windows-Unterprojekt mit dem vollständigen geprüften Profil Revision 40. Ohne `-MotionProfile` bleibt das Ausgabeziel `releases/27.5.4`; das frühere Profil `MotionConfig.precise-2.4.75-20261027.json` mit Revision 39 wird weiterhin unterstützt. `REFERENCE.json` hält Revision und Profil-Prüfsumme fest. Das Profil wird nicht automatisch angewandt. Diese GUI-Tests benötigen eine geeignete lokale Windows-Sitzung; sie verwenden einen Simulator. Separat vorhandene Hardwaretests nicht automatisch mit ausführen. Der Paketbuild kann Dateien im Ausgabeverzeichnis ersetzen; lokale Benutzerdaten bleiben davon getrennt.

## Nachweisstand, nicht neu ausgeführte Regression

| Datum / Stand | Beleg |
|---|---|
| Windows 27.5.4 / Motorprofil Revision 40, 27.10. | 1.432 Host-, 132 UI-, 16 Vorschau-, 34 Navigations- und 19 Touchprüfungen: 1.633 insgesamt. 96 reale Aufnahmen, Pan maximal 0,236°, Median 0,089°; 13 Warnungen im 72er-Lauf. Alle 129 Parameter zurückgelesen; [Pan-Korrektur, Profil und Grenzen](validation/2026-10-27-pan-positioning.md) |
| Windows 27.5.1, 26.10. | 1.415 Host-, 129 UI-, 16 Raster- und 29 gezielte Navigationsprüfungen bestanden; [Befunde und offene Surface-Diagnose](https://github.com/TomNaiser/PanoramaControl/blob/master/docs/history/WINDOWS-27.5.1-NAVIGATION-DIAGNOSE.md) |
| Windows 27.5.2, 26.10. | Direkte Pointer-Eingabe mit 19 Touchprüfungen; [Surface-Log, Korrektur und Messplan](https://github.com/TomNaiser/PanoramaControl/blob/master/docs/history/WINDOWS-27.5.2-SURFACE-TOUCH.md) |
| Windows 27.5.3, 27.10. | 1.427 Host-/Controllerprüfungen, Android-Praxisraster und pausierte Restaufträge; [Referenzbedingungen und Paket](https://github.com/TomNaiser/PanoramaControl/blob/master/docs/WINDOWS-27.5.3-ANDROID-0.6.27.md) |
| Android 0.6.27 / Firmware 2.4.75, 27.10. | Festgehaltene Referenz mit Motorprofil Revision 39; 57 reale Auslösungen, davon 39 mit verbessertem Bremswert; [Gegenversuch und Grenzen](validation/2026-10-27-positioning.md) |
| Firmware 2.4.75, 23.10. | 20.286 Host-Assertions, normaler R4-Build und Boot-Stack-Prüfung laut [Firmware-Abnahme (Zusammenfassung)](validation/README.md#firmware-2-4-75) und [Build (Zusammenfassung)](validation/README.md#firmware-2-4-75) |
| Firmware 2.4.76, 26./27.10. | 20.295 Host-Assertions, neun gezielte Sensorreferenzprüfungen, R4-/Startstackprüfung und realer Sensorstart mit Nullregister 0; [Nachweis und Grenzen](https://github.com/TomNaiser/PanoDuinoControl/blob/master/docs/history/TILT-REFERENCE-2.4.76.md) |
| Windows 27.5.0, 25.10. | 1.415 Host-/Protokoll-/Controller-, 129 WinForms- und 16 Raster-/Transferprüfungen; Paketbuild erfolgreich; [Nachweis und Grenzen](https://github.com/TomNaiser/PanoramaControl/blob/master/docs/history/WINDOWS-27.5.0-ANDROID-PARITAET.md) |
| Windows 27.4.4, 22.10. | 192 Protokoll-/Controller- und 115 simulierte UI-Prüfungen laut [Versionsnachweis](https://github.com/TomNaiser/PanoramaControl/blob/master/docs/history/WINDOWS-27.4.4-ROBUSTHEIT.md) |
| Android 0.6.24, 24.10. | 131 JVM-, 69 Wiederaufnahme-/Dialog- und 21 Speicherprüfungen laut [Befund (Zusammenfassung)](validation/README.md#recovery-2026-10-24) |
| Android 0.6.25, 24.10. | 136 JVM-Tests, Lint, 3.024 Pixel-Prüfungen mit 1.000 einzelnen XML-Updates laut [Papywizard-Nachweis (Zusammenfassung)](validation/README.md#papywizard-2026-10-24) |
| Produktive Daten, 24.10. | 3.332 Captures unverändert; 42 XML mit allen Positionen geprüft und Praxisdatei nach echtem Prozessneustart bytegleich |

Die Dokumentationsänderung führt diese Software-/Hardwaretests nicht erneut aus; sie prüft Quellen, Referenzen und Diagramme. Die genannten Zahlen gehören zu ihren jeweiligen datierten Ständen.

## Erfolg real messen

Eine Messreihe muss mindestens Versions-/Konfigurationsstand, Kamera/Objektiv, Balance, Grenzen, Tempo, Zeiten, Wetter/Last, geplante und bestätigte Punkte, Abbruchgrund und tatsächliche Laufzeit erfassen. Ein Benutzerabbruch ist kein spontaner Systemfehler; nicht ausgeführte Punkte gehören nicht als Fehlversuche in den Nenner.

Erforderliche getrennte Kennzahlen:

- echte Stillstände, grobe Fehlpositionen, kleine Warnungen, Sensor-/Transportfehler;
- automatische Retries, manuelle Pausen, erfolgreiche Fortsetzungen und Wiederaufnahmezeit;
- bestätigte Capturezahl gegenüber Kameradateien und möglichen Zusatzbelichtungen;
- Soll-/Ist-Abweichungen und tatsächliche Nachbarüberlappung, Pan/Tilt getrennt;
- UI-Latenz mit Verteilung und Maximum, statt nur „App blieb offen“;
- Dateiverfügbarkeit nach Stop, Fehler und Prozessneustart.

Für 20 % Mindestüberlappung ist unter einer vereinfachten eindimensionalen Winkelbetrachtung der tatsächliche Abstand benachbarter Aufnahmezentren gegen den passenden Bildwinkel zu prüfen: `overlap = 1 - Abstand/Bildwinkel`. Nahtübergang, sphärische Geometrie und Kameraorientierung gesondert berücksichtigen. Zwölf schlechte Nachbarpaare können aus vier Motorfehlern entstehen und sind keine zwölf unabhängigen Störungen.

Bei null Fehlern in 1.000 unabhängigen gleichartigen Versuchen liegt die einseitige 95-%-Obergrenze der unbekannten Fehlerrate noch bei rund 0,299 %. Für eine entsprechende Obergrenze von 0,1 % wären bei null Fehlern mindestens 2.995 unabhängige Versuche erforderlich (`1 - 0.05^(1/n)`). Gemeinsame Last-, Wetter- und Spalteneffekte können die Unabhängigkeit verletzen. Ein 1.000er-Speichertest liefert keine Aussage über die Motorfehlerrate.

## Diagnose bei einem neuen Feldfehler

App-/Firmwareversion, Laufnummer, letzten bestätigten Bildindex, Fehlermeldung und unveränderte Originaldaten sichern. Zunächst SQLite (bei Windows vor 27.5.0 das Journal) und Diagnose exportieren; eine manuelle Dateibereinigung darf nicht die einzige Rekonstruktionsquelle entfernen. Vor Neustart des Kopfes die Bedeutung von Boot-/Tilt-Referenz für die Wiederaufnahme beachten. Fehlende Istwerte als unbekannt belassen.
