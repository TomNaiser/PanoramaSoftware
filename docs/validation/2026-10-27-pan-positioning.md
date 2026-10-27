# Pan-Korrektur und aktueller Prüfstand

**27. Oktober 2026.** Die Pan-Abweichung wurde durch eine gezielte Änderung
des gespeicherten Motorprofils verringert. In **96 realen Aufnahmen** lag der
größte Pan-Fehler bei **0,236°**, der Median bei **0,089°**. Die Korrektur
wirkt im Kopf und gilt deshalb für die Bedienung mit Android und Windows.

## Aktueller Stand

| Komponente | Geprüfter Stand |
|---|---|
| Firmware | Reguläre Nano-R4-Bluetooth-Firmware **2.4.75** |
| Android | **0.6.27**, VersionCode 40 |
| Windows | **27.5.4**, .NET Framework 4.8 |
| Gespeichertes Motorprofil | **Revision 40**, vollständig zurückgelesen |
| Rückfallprofil | Revision 39, weiterhin erhalten |

Der Git-Tag **`referenz-2026-10-27-gut-laufend`** kennzeichnet diesen am
Gerät geprüften Gesamtstand. Sein Firmware-Unterprojekt verweist auf
2.4.75, sein Windows-Unterprojekt auf 27.5.4. Der separate Versuch 2.4.76
ist in einem eigenen Firmware-Commit gesichert und gehört nicht zu dieser
Laufreferenz.

Die Pan-Korrektur benötigt keine neue Firmware oder Android-Version. Der
lokale Firmwareversuch 2.4.76 gehört nicht zu diesem geprüften Stand.

## Was korrigiert wurde

Das gespeicherte Pan-Ankunftsfenster war zu groß. Mit der bisherigen Toleranz
akzeptiert der Regler beispielsweise eine ruhende Restabweichung von 0,282°,
ohne weiter zu korrigieren. Die Toleranz beeinflusst außerdem den Bremspunkt.

| Motorparameter | Bisher: Revision 39 | Jetzt: Revision 40 |
|---|---:|---:|
| Pan-Ankunft: `pan.positionToleranceDeg` | 0,30° | **0,15°** |
| Pan-Haltehysterese: `pan.holdExitToleranceDeg` | 0,40° | **0,25°** |

Die übrigen **127 Motorwerte bleiben unverändert**. Dazu gehören der bereits
korrigierte Tilt-Startbremsvorhalt von 0,110 s und die ausgeschaltete
Tilt-Feinannäherung. Es wurde kein vollständiger Werksreset durchgeführt.

Die größeren Pan-Werte waren bereits am 22. Oktober in Revision 29
gespeichert. Sie wurden nicht durch Windows 27.5.3 eingeführt. Ihr genauer
ursprünglicher Schreibzeitpunkt ist aus den Sicherungen nicht bestimmbar.
Android und Windows zeigen dieselben aufgezeichneten Encoderpositionen;
ein unterschiedliches Anzeigevorzeichen erklärt die Abweichung nicht.

## Gemessene Ergebnisse

Die Reihen liefen am selben Kopf über den Windows-Produktionscontroller und
Bluetooth-COM. Erfasst wurden die tatsächlichen Positionen beim bestätigten
Aufnahmeabschluss, keine aus dem Plan rekonstruierten Positionen.

| Reihe | Aufnahmen | Pan maximal | Pan Median | Tilt maximal |
|---|---:|---:|---:|---:|
| Vorher, Revision 39, längerer Anlauf | 12 | 0,436° | 0,285° | 0,205° |
| Vorher, Revision 39, Wiederholung | 12 | 0,348° | 0,174° | 0,190° |
| Revision 40, gleiches 2×6-Rezept | 12 | **0,088°** | 0,031° | 0,247° |
| Revision 40, 12×6 mit elf Spaltenrückfahrten | 72 | **0,236°** | 0,106° | 0,315° |
| Revision 40, 2×6 in Gegenrichtung | 12 | **0,182°** | 0,075° | 0,190° |

Alle **96 Aufnahmen mit Revision 40** wurden vollständig gespeichert. Kein
Pan-Wert lag über 0,3°. Der 72er-Lauf endete ohne Pause, Wiederholungsversuch
oder Neustart. Die Rezepte verwendeten Tempo 1700/1700 sowie
500/30/300 ms für Beruhigung, Auslösepuls und Belichtungswartezeit.

**Verbleibende Warnungen:** Der 72er-Lauf meldete 13 Zielverfehlungen beim
engeren internen Ankunftsfenster. Ein Tilt-Wert betrug 0,315°. Die beiden
kurzen Reihen meldeten keine Warnungen. Die Korrektur ist damit als
verbesserte Pan-Positionierung belegt; eine allgemein warnungsfreie oder
lastunabhängige Aufnahmefolge ist daraus nicht abzuleiten.

## Gesicherte Profile und Windows-Paket

- [Aktuelles vollständiges Profil, Revision 40](../../PanoramaAndroid/config/MotionConfig.precise-pan-2.4.75-20261027.json)
- [Vorheriges vollständiges Profil, Revision 39](../../PanoramaAndroid/config/MotionConfig.precise-2.4.75-20261027.json)
- [Ursprüngliche Android-/Tilt-Korrektur](2026-10-27-positioning.md)

Am geprüften Kopf ist Revision 40 bereits gespeichert. Das separate Paket
`PanoramaControl-27.5.4-pan-r40.zip` enthält dieselbe Windows-Anwendung 27.5.4
und das neue Profil. `REFERENCE.json` nennt die Profilrevision und Prüfsumme.
Der App-Start überträgt das Profil nicht automatisch. Die Dateirevision
beschreibt die gesicherte Konfiguration dieses Kopfs; beim Übertragen gelten
die aktuelle Geräterevision und die vollständige Parameterprüfung.

Der frühere Referenzstand bleibt im Hauptprojekt-Commit `665f0dc` erhalten.
Wie das Paket gebaut wird, beschreibt die [Buildanleitung](../10-build-tests-und-wartung.md#windows).

## Softwareprüfung und offene Nachweise

Der Paketbuild bestand **1.633 Softwareprüfungen**: 1.432 Host-/Controller-,
132 Windows-UI-, 16 Vorschau-, 34 Navigations- und 19 Touchprüfungen.
Alle 129 Motorwerte wurden abschließend zurückgelesen; genau die beiden
aufgeführten Pan-Werte unterscheiden sich vom Rückfallprofil.

Noch offen sind die vollständige neue Surface-Bedienfolge einschließlich
Wiederaufnahme nach einem realen Fehler sowie die Prüfung unter wechselnder
Last und Wind. Bestätigte Trigger und Encoderpositionen belegen weder
gespeicherte Kameradateien noch Bildschärfe oder eine allgemeine optische
Mindestüberlappung. Weitere Punkte stehen in der [Roadmap](../09-probleme-und-roadmap.md).
