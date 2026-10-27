# Geprüfter Stand: Android 0.6.27 und Positionierungsprofil

Am 27. Oktober 2026 auf dem Pixel 7 Pro mit regulärer Firmware **2.4.75**
geprüft. Der Benutzer bestätigt anschließend präzises und schnelles Anfahren.

## Wiederherstellbarer Stand

- Android **0.6.27 / VersionCode 40**, Paket `de.tomnaiser.panoramaandroid`.
- Firmware **2.4.75**, Submodul-Commit
  `55ff4bb9e74085bacb71bb94783856b67a685f55`.
- Gespeicherte Motorkonfiguration **Revision 39**, alle **129 Werte**:
  [vollständiges geprüftes Profil](../../PanoramaAndroid/config/MotionConfig.precise-2.4.75-20261027.json).
- Installierte APK SHA-256:
  `AB122A11B90E279EDB375887F4C7F94A167A8F583E6F34D2A874C8C1BD2E4839`.

Die Genauigkeitskorrektur liegt im gespeicherten Geräteprofil. Ein Neubau
der App allein stellt dieses Profil nicht wieder her. Der archivierte
Datensatz enthält auch die übrigen gerätespezifischen Parameter und Grenzen;
er dokumentiert den geprüften Kopf und ist keine allgemeine Voreinstellung.
Die `baseRevision` bezeichnet den damaligen Gerätestand. Beim späteren
Übertragen gelten die aktuelle Revision, Parameterprüfung und ausdrückliche
Anwendung der Motorkonfiguration.

## Zwei getrennte Fehler

Seit Android 0.6.24 fehlte beim Ersetzen eines pausierten Auftrags die
Laufnummer im STOP-Befehl. Die Firmware antwortete mit BAD_FIELD_COUNT.
0.6.27 übermittelt die ursprüngliche Laufnummer. Der Integrationstest
verlangt protokollgerechte STOP-Argumente, erhält bei Ablehnung den alten
Auftrag und prüft den gesonderten START des bestätigten Restauftrags.
Der normale Erststart desselben 43×6-Rasters ist ebenfalls abgedeckt.

Die großen Tilt-Unterschreitungen stammen dagegen vom gespeicherten
`control.travelStartBrakeLeadSeconds`. Dieser Wert wurde am **22.10.2026**
von **0,075 auf 0,200 s** erhöht und blieb über die Versionswechsel erhalten.
Der große Start-Bremsvorhalt kann kurze Abwärtsschritte vor dem Ziel
abbremsen. Ohne Feinannäherung bleibt der Restfehler bestehen; nach
STEP_TARGET_MISSED kann der Aufnahmeablauf an dieser Istposition auslösen.

Die Reparatur setzt ausschließlich diesen Wert auf **0,110 s**. Alle anderen
**128 Parameter** wurden unverändert zurückgelesen; Feinannäherung bleibt
ausgeschaltet. Die Firmware-Standardwerte wurden nicht geändert.

## Reale Gegenversuche

Insgesamt **57 von 100** freigegebenen Kameraauslösungen verwendet, alle
bestätigt und gespeichert. Eine dauerhafte Reservierung vor START begrenzte
die Summe und verhinderte erneut gestartete Testlauf-IDs. Die Pläne endeten
selbst; der Prüfpfad bestätigte danach Stopp und einen freien Geräteauftrag.
Der Produktionsservice und seine SQLite-Aufnahmespeicherung wurden verwendet.

| Reihe | Start-Bremsvorhalt | Tilt-Tempo | Raster | Bilder | Max. Tilt | Max. Pan |
|---|---:|---:|---|---:|---:|---:|
| Ausgangswert | 0,200 s | 1700 | 1×3 | 3 | 0,556° | 0,043° |
| Nur langsamer | 0,200 s | 500 | 1×3 | 3 | 0,820° | 0,022° |
| Korrektur | 0,110 s | 1700 | 1×3 | 3 | 0,095° | 0,000° |
| Korrektur | 0,110 s | 1700 | 2×3 | 6 | 0,145° | 0,062° |
| Korrektur, langsamer | 0,110 s | 500 | 2×3 | 6 | 0,233° | 0,308° |
| Korrektur, volle Höhe | 0,110 s | 1700 | 2×6 | 12 | 0,203° | 0,282° |
| Alter Wert als Gegenprobe | 0,200 s | 1700 | 2×6 | 12 | **0,665°** | 0,352° |
| Korrektur erneut | 0,110 s | 1700 | 2×6 | 12 | **0,291°** | **0,242°** |

Die letzten drei Rezepte sind bis auf die Lauf-ID identisch: Pan-Start
127,375°, Tilt-Start 4,614°, Schritte 2,062°/−3,083°, Tempo 1700/1700,
Beruhigung/Puls/Belichtung 500/30/300 ms, Heartbeat-Stopp nach 5000 ms.
Alle **39 Aufnahmen mit 0,110 s** blieben bei Tilt unter 0,3°.
Die letzte Reihe war ohne Fehlermeldung. Frühere Reihen mit verbessertem
Profil meldeten vereinzelt STEP_MOVE_TIMEOUT beziehungsweise eine kleine
Pan-Zielverfehlung. Ein vollständiger 43-Spalten-Lauf und andere Lasten sind
damit noch nicht abgenommen. Pan wurde nicht separat abgestimmt.

## Softwareprüfung und Abschluss

Vor der STOP-Korrektur reproduzierte der Integrationstest die Ablehnung.
Danach bestanden **85 Wiederaufnahme-/Startprüfungen**, **21 Speicherprüfungen**,
**141 JVM-Tests**, Lint und APK-Build. Die ursprünglichen App-Daten blieben
beim Update erhalten. Die Hardwareprüfung ist von diesen simulierten
Service-Prüfungen zu unterscheiden.

Die temporären Hardwareprüfer sind aus dem aktiven Quellbaum entfernt,
die Test-App vom Handy deinstalliert. Produktions-App 0.6.27 blieb installiert
und wurde wieder geöffnet. Die rohen Diagnoseläufe, Prüfwerkzeuge und der
vollständige Gegenversuchsbericht bleiben lokal unter
`analysis/2026-10-27-positioning/`. Das hier verlinkte Motorprofil ist bewusst
als Teil dieses wiederherstellbaren Stands versioniert.
