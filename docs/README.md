# Gesamtdokumentation PanoramaSoftware

**Stand: 24. Oktober 2026. Sprache: Deutsch.** Referenz: Firmware 2.4.75, Android 0.6.25, Windows 27.4.4. Diese Dokumentation beschreibt Funktionen, Architektur und den jeweiligen Prüfstand. Für den englischen Einstieg: [Overview and features](../README.md) · [Short manual](en/manual.md).

## Lesen nach Fragestellung

| Kapitel | Inhalt |
|---|---|
| [01 Systemübersicht](01-systemuebersicht.md) | Hardware, Softwareblöcke, Verantwortungen, Koordinaten, Quellstruktur |
| [02 Bedienung und Acquise](02-bedienung-und-acquise.md) | Ausrichten, Planen, Auslösen, Pause, Stop, Wiederaufnahme; Ablaufdiagramme |
| [03 Firmware](03-firmware.md) | Hauptschleife, Regelung, Sicherheitsfunktionen, Speicherung, Modul- und Funktionsübersicht |
| [04 Android](04-android.md) | Oberfläche, Service, Bluetooth, Freigaben, SQLite, Wiederaufnahme, laufende XML |
| [05 Windows](05-windows.md) | WinForms, Sitzungscontroller, Protokoll, Journal, Export, Unterschiede zu Android |
| [06 Protokoll und Datenmodell](06-protokoll-und-datenmodell.md) | Rahmen, Kommandos, Identitäten, Planfelder, Meldungen, Datenherkunft |
| [07 Variablen und Parameter](07-variablen-und-parameter.md) | Einheiten, zentrale Zustandsvariablen, Zeitgrenzen, Abstimmparameter |
| [08 Funktionsstand](08-funktionsstand.md) | Umgesetzt, teilweise umgesetzt, fehlend und noch nicht nachgewiesen |
| [09 Probleme und Roadmap](09-probleme-und-roadmap.md) | Verbleibende Risiken, bekannte Unterschiede, konkrete nächste Nachweise |
| [10 Build, Tests und Wartung](10-build-tests-und-wartung.md) | Voraussetzungen, Befehle, Testebenen, Belegstand und Statistik |
| [11 GitHub und Dokumentationspflege](11-github-und-dokumentationspflege.md) | Mermaid, Veröffentlichung, Submodule, Aktualisierung und Prüfskripte |
| [Motorparameter-Referenz](referenz/motorparameter.md) | Vollständiger generierter Katalog mit ID, Name, Einheit, Skalierung, Default und Grenzen |
| [Quellcode-Referenz](referenz/quellcode.md) | Generiertes Verzeichnis der aktiven Produktionsdateien und ihrer deklarierten Typen |
| [Funktionssignaturen](referenz/funktionen.md) | Generierte Methodensignaturen und Quellverweise als zusätzliche Suchhilfe |
| [Prüfstand](validation/README.md) | Öffentliche Zusammenfassung der Software-/Hardware-Nachweise und ihrer Grenzen |
| [Lizenzzuordnung](../LICENSES.md) | MIT für eigene Hauptprojekt-/Android-Inhalte und abweichende Unterprojekt-/Fremdlizenzen |

## Geltungsbereich und Belegregeln

Die Kapitel erklären die aktiven V2-Pfade. Historische Firmware `PanoDuinoControlV181231.ino`, Windows-Version-26-Projekt, alte serielle Klassen, Sicherungen und Diagnose-Sketches sind Orientierung und Entwicklungswerkzeuge, keine gleichwertigen aktuellen Produktvarianten. Alte Klassen können aus Kompatibilitätsgründen noch im Projekt liegen.

„Implementiert“ bedeutet: Im aktuellen produktiven Code vorhanden. „Softwaregeprüft“ bezeichnet Tests mit Host, Simulator oder isolierter Android-App. „Am Gerät geprüft“ benötigt einen datierten realen Befund. Eine Implementierung ohne vollständigen Hardware-/Bildqualitätsnachweis wird ausdrücklich so bezeichnet. Frühere Testzahlen werden als datierte Nachweise angegeben und nicht als heute neu ausgeführte Tests ausgegeben.

Diese Dokumentationsarbeit verändert keine Motorparameter und startet keine Hardwaretests. Die bekannten Bilddaten von 264/300 Bildern sind ein gespeicherter Praxisfall; die 1.000 XML-Aktualisierungen sind ein Speicher-/Softwaretest, keine 1.000 Motorfahrten.

## Begriffe

| Begriff | Bedeutung |
|---|---|
| Acquise / Akquisition | Aufnahmefolge eines Panoramas einschließlich Bewegung, Ruheprüfung und Trigger |
| Entwurf | Lokale Vorschau; kann vom bereits geladenen Geräteauftrag abweichen |
| Plan / Rezept | Vollständiger, validierter und an die Firmware übertragener Aufnahmeauftrag |
| Ziel / Soll | Gewünschte Position, keine Bestätigung einer tatsächlichen Ankunft |
| Ist | Gemeldete Encoderposition in der jeweiligen Referenz |
| Capture | Vom Ablauf bestätigter Aufnahmeabschluss; kein Nachweis einer gespeicherten Kameradatei |
| Warnung | Qualitätsabweichung, die nicht automatisch ein Sicherheits-Fault ist |
| Fault | Sicherheits- oder Ablaufstörung; Motor-/Kameraausgabe wird beendet |
| Quittieren | Vorfall anerkennen und Gerätestand prüfen; startet keinen alten Auftrag |
| Fortsetzen | Entweder `RESUME` eines zulässig pausierten Auftrags oder bewusstes Starten eines neu geladenen Restauftrags |
| Segment | Zusammenhängender Abschnitt einer Planrevision; hält Wiederholungen und Sprünge auseinander |
| Rekonstruiert | Aus Plan/Zähler/Segment abgeleiteter Eintrag mit unbekanntem historischem Istwert |

[Zur Projektstartseite](../README.md)
