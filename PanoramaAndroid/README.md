# PanoramaControl für Android

Android-Bedienung für einen motorisierten Pan-/Tilt-Panoramakopf. Aktueller Quellstand: **0.6.25, VersionCode 38**, Protokoll V2; zuletzt geprüfte normale Firmware **2.4.75**.

## Dokumentation

- [Gesamthandbuch](../docs/README.md)
- [Android-Architektur und Funktionen](../docs/04-android.md)
- [Bedienung und Wiederaufnahme](../docs/02-bedienung-und-acquise.md)
- [Prüfstand und Grenzen](../docs/validation/README.md)
- [English quick manual](../docs/en/manual.md)

## Funktionen

Raster- und sphärische Planung, getrenntes Übertragen/Starten, manuelle Ziel- und Haltefahrt, Pause, Spaltennavigation, Timelapse, Tilt-Referenz und Grenzen. Fehler können nach Prüfung des Gerätestands über verknüpfte Restaufträge fortgesetzt werden; sphärische Originalbänder bleiben erhalten.

Aufnahmen liegen dauerhaft in SQLite. Papywizard-XML wird während der Acquise im Hintergrund als vollständig geschlossene Datei atomar ersetzt. Nach Prozessneustart wird aus SQLite ergänzt. Unter Extras stehen Sicherungsstatus, erneute Sicherung sowie XML-/CSV-Export.

## Bauen

JDK 17 beziehungsweise kompatible Android-Studio-JBR, Android SDK 35; minSdk 26. Vom Gesamtprojekt aus:

```powershell
./PanoramaAndroid/gradlew.bat -p PanoramaAndroid testDebugUnitTest lintDebug assembleDebug
```

Die APK liegt anschließend unter `app/build/outputs/apk/debug/`. Hardwarefreie Pixel-Integration mit separater Validation-App und Hinweise zur produktiven Paketkennung: [Build- und Testanleitung](../docs/10-build-tests-und-wartung.md).

## Quellen und Diagnose

`app/src/main/java/` enthält die Produktklassen, `app/src/test/` JVM-Tests und `app/src/androidTest/` Integrations-/Hardwareprüfer. Hardwareprüfer sind ausdrücklich von hardwarefreien Tests zu unterscheiden. `diagnostics/` enthält wiederverwendbare Werkzeuge; erzeugte Messdaten und Telefonbackups werden lokal gehalten.

Vollständige frühere Entwicklungsnotizen bleiben lokal unter `docs/history/`, Messbelege unter `abnahme/` und im übergeordneten `analysis/`. Ausgelesene Motorprofile unter `config/`, gespeicherte Diagnoseergebnisse und APK-Pakete unter `releases/` bleiben ebenfalls lokal. Sie werden nicht für einen normalen öffentlichen Checkout benötigt. Die öffentlich zusammengefassten Tests beweisen keine allgemeine Motorfehlerrate oder Bildschärfe.

## Lizenz

Die eigene Android-App einschließlich eigener Tests und Diagnosewerkzeuge steht unter [MIT](LICENSE), Copyright (c) 2026 TomNaiser. Der Gradle Wrapper und weitere Fremdkomponenten behalten ihre eigenen Lizenzen; siehe [Lizenzzuordnung](../LICENSES.md) und [Apache-2.0-Lizenztext des Wrappers](licenses/Apache-2.0.txt).
