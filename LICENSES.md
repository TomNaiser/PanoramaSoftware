# Lizenzzuordnung

Die eigenen Inhalte dieses Hauptrepositorys stehen unter der [MIT-Lizenz](LICENSE), Copyright (c) 2026 TomNaiser. Dies umfasst insbesondere die Android-App, eigene Tests und Diagnosewerkzeuge, selbst verfasste Dokumentation und synthetische Beispiele. Abweichend lizenzierte Bestandteile und vorhandene Rechtehinweise bleiben davon ausgenommen.

| Bereich | Lizenz / Geltungsbereich |
|---|---|
| `PanoramaAndroid/app/src/`, eigene Android-Projektdateien und Diagnosewerkzeuge | [MIT](PanoramaAndroid/LICENSE) |
| Eigene Inhalte in `docs/`, `tools/`, `examples/`, eigene Diagnose-Sketches und übrige eigene Hauptprojektdateien | [MIT](LICENSE) |
| Firmware-Unterprojekt `PanoduinoControlGitRepo/` | Bestehende [LGPL-2.1-Lizenzdatei](https://github.com/TomNaiser/PanoDuinoControl/blob/master/LICENSE); wird nicht durch die MIT-Lizenz des Hauptprojekts ersetzt |
| Windows-Unterprojekt `PanoramaControlGitRepo/` | Bestehende [MIT-Lizenzdatei](https://github.com/TomNaiser/PanoramaControl/blob/master/LICENSE) mit eigenen Copyright-Hinweisen; unverändert |
| Gradle Wrapper `PanoramaAndroid/gradle/wrapper/gradle-wrapper.jar` | Apache License 2.0; vollständiger [mitgelieferter Lizenztext](PanoramaAndroid/licenses/Apache-2.0.txt) |

Übernommene Codeauszüge, Lizenztexte und sonstige fremde Inhalte behalten die Rechte und Lizenzbedingungen ihrer jeweiligen Urheber. Verweise auf getrennte Unterprojekte lizenzieren diese nicht um. Bereits vorhandene Copyright- und Lizenzhinweise sind zu erhalten.

## Build- und Testabhängigkeiten

Der eingecheckte Gradle Wrapper enthält seine Apache-2.0-Lizenz zusätzlich unter `META-INF/LICENSE` in der JAR-Datei. Die unter `PanoramaAndroid/licenses/` abgelegte Kopie wurde daraus unverändert übernommen. Die [Gradle-Distribution 8.11.1](https://github.com/gradle/gradle/blob/v8.11.1/LICENSE) enthält weitere eigene Hinweise zu ihren Bestandteilen; sie wird beim Build gesondert geladen.

JUnit 4.13.2 wird über `testImplementation` als Testabhängigkeit geladen und steht unter der [Eclipse Public License 1.0](https://github.com/junit-team/junit4/blob/r4.13.2/LICENSE-junit.txt). Es ist keine Produktionsabhängigkeit der Android-App. Weitere beim Build oder Test geladene Werkzeuge und transitive Abhängigkeiten behalten ihre jeweiligen Lizenzen. Die MIT-Lizenz dieses Projekts ersetzt keine dieser Bedingungen.
