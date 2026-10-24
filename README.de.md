# PanoramaSoftware

[English overview](README.md) · [English short manual](docs/en/manual.md)

Steuerung eines motorisierten Pan-/Tilt-Panoramakopfes: Arduino-Firmware, Android-App und Windows-App. Die Firmware führt übertragene Aufnahmepläne autonom aus; die Apps planen, bedienen, überwachen und archivieren die Aufnahmen.

**[Gesamtdokumentation öffnen](docs/README.md)** — Architektur, Bedienabläufe, Funktionen, Variablen, Motorparameter, Protokoll, Diagramme, Tests und bekannte Probleme.

| Komponente | Dokumentierter Quellstand am 24.10.2026 |
|---|---|
| Firmware PanoDuinoControl | 2.4.75, regulärer Nano-R4-Bluetooth-Build |
| Android PanoramaControl | 0.6.25, VersionCode 38 |
| Windows PanoramaControl | 27.4.4, WinForms / .NET Framework 4.8 |
| Drahtprotokoll | V2; Rasterrezept Version 1, sphärisches Rezept Version 2 |

Die Dokumentation beschreibt den angegebenen Softwarestand und seine Prüfgrenzen. Die Komponenten haben unterschiedliche Funktionsstände; [Funktionsmatrix](docs/08-funktionsstand.md) und [offene Probleme](docs/09-probleme-und-roadmap.md) gehören zur technischen Beschreibung.

```mermaid
flowchart LR
    A[Android-App] -->|Bluetooth SPP| F[Firmware]
    W[Windows-App] -->|Bluetooth COM| F
    F --> M[Pan- und Tilt-Antrieb]
    E[Positionsencoder] --> F
    F --> K[Kameraauslöser]
    A --> X[Papywizard XML und CSV]
    W --> X
```

Android und Windows sind alternative Bedienclients. Ein gleichzeitiger Zugriff beider Apps auf denselben Kopf ist kein unterstützter Betriebsablauf.

## Repository

- [Android](PanoramaAndroid/README.md) liegt im Hauptrepository.
- [Firmware-Repository](https://github.com/TomNaiser/PanoDuinoControl) ist als `PanoduinoControlGitRepo` eingebunden.
- [Windows-Repository](https://github.com/TomNaiser/PanoramaControl) ist als `PanoramaControlGitRepo` eingebunden.
- [Öffentliche Prüfergebnisse](docs/validation/README.md) fassen die Nachweise und ihre Grenzen zusammen.

Checkout mit Unterprojekten: `git clone --recurse-submodules <URL-dieses-Repositories>`.

GitHub stellt die eingebetteten Mermaid-Blöcke direkt dar. [Veröffentlichung und Pflege](docs/11-github-und-dokumentationspflege.md) erklären Querverweise, Unterprojekte und die Prüfung der Dokumentation.

## Lizenz

Android und die eigenen Inhalte dieses Hauptrepositorys stehen unter [MIT](LICENSE), Copyright (c) 2026 TomNaiser. Firmware, Windows und Fremdkomponenten behalten ihre jeweiligen Lizenzen; die [Lizenzzuordnung](LICENSES.md) beschreibt die Grenzen und Hinweise.
