# PanoramaControl für Android

Android-Bedienung für einen motorisierten Pan-/Tilt-Panoramakopf. Aktueller Quellstand: **0.6.27, VersionCode 40**, Protokoll V2; zuletzt geprüfte normale Firmware **2.4.75**.

## Dokumentation

- [Gesamthandbuch](../docs/README.md)
- [Android-Architektur und Funktionen](../docs/04-android.md)
- [Bedienung und Wiederaufnahme](../docs/02-bedienung-und-acquise.md)
- [Prüfstand und Grenzen](../docs/validation/README.md)
- [English quick manual](../docs/en/manual.md)

## Funktionen

Raster- und sphärische Planung, getrenntes Übertragen/Starten, manuelle Ziel- und Haltefahrt, Pause, Spaltennavigation, Timelapse, Tilt-Referenz und Grenzen. Fehler können nach Prüfung des Gerätestands über verknüpfte Restaufträge fortgesetzt werden; sphärische Originalbänder bleiben erhalten.

0.6.27 korrigiert die Wiederaufnahme eines pausierten Auftrags: Der vor dem Restauftrag erforderliche `STOP` enthält jetzt die ursprüngliche Laufnummer. Seit 0.6.24 fehlte sie in diesem Pfad; die Firmware lehnte den Befehl mit `BAD_FIELD_COUNT` ab. Der Integrationstest prüft jetzt dieselben Stop-Argumente wie das Gerät sowie den separaten Start des bestätigten Restauftrags.

Unter **Extras → Sprache / Language** stehen **Deutsch** und **English** zur Wahl. Die Auswahl gilt für Oberfläche, Dialoge, Status- und Fehlermeldungen und bleibt nach einem Neustart gespeichert. Deutsch ist die Voreinstellung. Der Wechsel erneuert die Oberfläche; Geräteverbindung, geladener Auftrag, Eingaben und Diagrammausschnitt bleiben erhalten. Historische Protokolle und vor 0.6.26 gespeicherte Fehlerdetails behalten ihre ursprüngliche Sprache.

Aufnahmen liegen dauerhaft in SQLite. Papywizard-XML wird während der Acquise im Hintergrund als vollständig geschlossene Datei atomar ersetzt. Nach Prozessneustart wird aus SQLite ergänzt. Unter Extras stehen Sicherungsstatus, erneute Sicherung sowie XML-/CSV-Export.

## Bauen

JDK 17 beziehungsweise kompatible Android-Studio-JBR, Android SDK 35; minSdk 26. Vom Gesamtprojekt aus:

```powershell
./PanoramaAndroid/gradlew.bat -p PanoramaAndroid testDebugUnitTest lintDebug assembleDebug
```

Prüfstand 0.6.26 (25. Oktober 2026): 141 Unit-Tests erfolgreich, zusätzlich 18 gezielte Sprachwechselprüfungen mit echter Activity und Service in einer getrennten Emulator-App; abschließender Build, Lint und Sprachtests erfolgreich. Auf dem Pixel 7 Pro wurde die vorhandene App mit `adb install -r` auf 0.6.26 / Code 39 aktualisiert und die installierte Version über den Android-Paketmanager bestätigt. Die Sprachwechselprüfungen liefen im Emulator.

Die APK liegt anschließend unter `app/build/outputs/apk/debug/`. Hardwarefreie Pixel-Integration mit separater Validation-App und Hinweise zur produktiven Paketkennung: [Build- und Testanleitung](../docs/10-build-tests-und-wartung.md).

Prüfstand 0.6.27 (27. Oktober 2026): Der Wiederaufnahmefehler wurde am echten Service mit protokolltreuem simuliertem Gerät vor der Korrektur reproduziert. Danach bestanden im isolierten Emulator 85 Wiederaufnahme-/Startprüfungen und 21 Speicherprüfungen, einschließlich des aufgezeichneten 43×6-Rasterplans und seines normalen Erststarts. Das ist keine reale Motorabnahme und erklärt keinen Stillstand trotz bereits ausgegebener Motor-PWM.

Reale Positionierungsprüfung vom selben Tag: Mit regulärer Firmware 2.4.75 wurde der gespeicherte Start-Bremsvorhalt von 0,200 auf 0,110 s korrigiert, alle anderen 128 Parameter blieben unverändert. Alle 39 Testbilder mit diesem Wert lagen bei Tilt unter 0,3°; die Gegenprobe mit 0,200 s reproduzierte 0,665°. [Geprüfter Stand, vollständiges Motorprofil und Grenzen](../docs/validation/2026-10-27-positioning.md).

## Quellen und Diagnose

`app/src/main/java/` enthält die Produktklassen, `app/src/test/` JVM-Tests und `app/src/androidTest/` Integrations-/Hardwareprüfer. Hardwareprüfer sind ausdrücklich von hardwarefreien Tests zu unterscheiden. `diagnostics/` enthält wiederverwendbare Werkzeuge; erzeugte Messdaten und Telefonbackups werden lokal gehalten.

Vollständige frühere Entwicklungsnotizen bleiben lokal unter `docs/history/`, Messbelege unter `abnahme/` und im übergeordneten `analysis/`. Ausgelesene Motorprofile unter `config/`, gespeicherte Diagnoseergebnisse und APK-Pakete unter `releases/` bleiben ebenfalls lokal. Sie werden nicht für einen normalen öffentlichen Checkout benötigt. Die öffentlich zusammengefassten Tests beweisen keine allgemeine Motorfehlerrate oder Bildschärfe.

Ausnahme: `config/MotionConfig.precise-2.4.75-20261027.json` ist als vollständiges Profil des am 27. Oktober geprüften Kopfs mitversioniert, damit dessen Gerätekonfiguration zusammen mit dem App-Stand wiederherstellbar ist.

## Lizenz

Die eigene Android-App einschließlich eigener Tests und Diagnosewerkzeuge steht unter [MIT](LICENSE), Copyright (c) 2026 TomNaiser. Der Gradle Wrapper und weitere Fremdkomponenten behalten ihre eigenen Lizenzen; siehe [Lizenzzuordnung](../LICENSES.md) und [Apache-2.0-Lizenztext des Wrappers](licenses/Apache-2.0.txt).
