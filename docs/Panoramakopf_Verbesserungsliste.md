# Panoramakopf – Verbesserungsliste

Stand: 15.11.2026

## 1. Aufnahmebereich vollständig erfassen

### Beobachtung
Der Benutzer fährt eine Startposition an und speichert sie, danach eine Endposition. Die an diesen Positionen aufgenommenen Bilder definieren die Randbilder des gewünschten Panoramas. Bei einer bisherigen Aufnahme war offenbar insbesondere das untere Endbild nur teilweise enthalten.

### Anforderungen
- Das Startbild und das Endbild müssen **vollständig** im fertigen Panorama enthalten sein.
- Die gespeicherten Positionen sind als **Bildmittelpunkte**, nicht als äußere Bildgrenzen zu interpretieren.
- Der tatsächlich aufgenommene Bereich muss die vollständigen Bildfelder beider Randpositionen abdecken.
- Die Berechnung der Anzahl von Aufnahmepositionen und der Schrittweite darf den gewünschten Bereich nicht durch Rundung verkleinern.
- Auch der spätere Zuschnitt bzw. das Stitching darf die Randbilder nicht unbeabsichtigt beschneiden.

### Zu prüfen
Rundung, Schrittzahl, Positionierung der letzten Aufnahme sowie Beschnitt beim Zusammenfügen.

### Akzeptanzkriterium
Die Bildfelder der beiden gespeicherten Randpositionen sind im Ergebnis vollständig enthalten; es fehlen keine erwarteten Randinformationen.

## 2. Logischen Nullpunkt und Bewegungsbereich korrekt initialisieren

### Beobachtung
Beim heutigen Start befand sich die Pan-Achse offenbar am äußersten rechten Rand des softwareseitig zulässigen Bereichs, möglicherweise an der Endposition eines früheren Panoramas. Manuelle Fahrten per Pfeiltasten waren zeitweise nur als kleine „Hopser“ möglich. Die Ursache ist noch ungeklärt; denkbar sind absolute Encoderwerte oder übernommene Software-Grenzen.

### Anforderungen
- Den logischen Nullpunkt per Software setzen können.
- Prüfen, ob die aktuelle Achsposition beim Einschalten oder spätestens vor dem ersten Auftrag automatisch als logischer Nullpunkt gesetzt werden soll.
- Absolute Encoderposition, logischen Nullpunkt und tatsächliche mechanische Grenzen sauber unterscheiden.
- Ein alter softwareseitiger Bewegungsbereich darf die neue manuelle Ausrichtung nicht unbeabsichtigt einschränken.
- Es darf nicht erforderlich sein, zunächst vom vermeintlichen rechten Rand zur Mitte zu fahren, um das Stativ auszurichten.
- Mechanische und sicherheitsrelevante Endanschläge bleiben geschützt.

### Zu prüfen
Persistierte Positionen, Software-Endschalter, Encoderreferenzierung, Grenzprüfungen und Steuerung der Pfeiltasten.

### Akzeptanzkriterium
Nach dem Start ist eine nachvollziehbare Referenzierung möglich und die Pan-Achse lässt sich im zulässigen mechanischen Bereich ohne unerklärliche „Hopser“ manuell positionieren.

## 3. Häufige Aufnahmeabbrüche untersuchen und reduzieren

### Beobachtung
Beim aktuellen Panoramalauf traten mindestens vier Unterbrechungen auf. Es war relativ kalt; möglicherweise laufen die Motoren dadurch schwergängiger. Ein ursächlicher Zusammenhang ist bisher nicht nachgewiesen.

### Anforderungen
- Fehler- und Abbruchbedingungen überprüfen.
- Zwischen kritischen Fehlern, behebbaren Störungen und reinen Warnungen unterscheiden.
- Unnötige Aufnahmeabbrüche vermeiden.
- Ereignisse mit Zeitstempel und relevantem Bewegungs-/Aufnahmekontext protokollieren.

### Akzeptanzkriterium
Abbrüche erfolgen nur bei tatsächlich erforderlichen Bedingungen; die Ursachen verbleibender Unterbrechungen sind nachvollziehbar protokolliert.

## 4. Restbewegung im Bild nur als Warnung behandeln

### Beobachtung
Eine Panoramaaufnahme wurde abgebrochen, weil sich das Bild bei der Aufnahme angeblich noch bewegte.

### Anforderungen
- Eine erkannte Restbewegung darf **für sich genommen keinen Abbruch** des Panoramaauftrags auslösen.
- Stattdessen eine Warnung erzeugen und im Hintergrund protokollieren.
- Die Warnung soll die Aufnahme weder anhalten noch einen manuellen Eingriff erfordern.
- Warnung möglichst mit Zeitstempel, Bild-/Positionsnummer und gemessenem Bewegungsindikator speichern.

### Akzeptanzkriterium
Bei erkannter Restbewegung läuft die Panoramaaufnahme weiter; der Vorfall ist im Log auffindbar.

## 5. Settling-Zeit während laufender Aufnahme ändern

### Ausgangssituation
Nach einer Achsfahrt und dem Stillstand wartet das Programm eine Settling-Zeit ab, bevor das nächste Bild aufgenommen wird.

### Anforderungen
- Die Settling-Zeit **on the fly** ändern können, ohne das Panorama anzuhalten oder neu zu starten.
- Bedienung mit möglichst nur einer Aktion, ohne Zahleneingabe.
- Gut erreichbares Dropdown mit zwei oder drei vordefinierten Zeiten.
- Die Änderung soll spätestens beim nächsten geeigneten Bewegungs-/Aufnahmezyklus wirksam werden.
- Die tatsächlichen Auswahlwerte sind noch festzulegen; beispielsweise kurz, mittel, lang.

### Akzeptanzkriterium
Während einer laufenden Aufnahme kann die Wartezeit mit einer einzigen Auswahl umgestellt werden; die Aufnahme läuft ohne Unterbrechung weiter.

## 6. Manuelle Pfeiltastenfahrten beschleunigen

### Beobachtung
Die Standardgeschwindigkeit manueller Achsfahrten ist subjektiv ungefähr um Faktor zwei zu niedrig.

### Anforderungen
- Die Default-Geschwindigkeit für manuelle Fahrten auf ungefähr **200 %** des bisherigen Werts erhöhen.
- Längere Fahrten sollen flüssig und ohne unnötige Unterbrechungen möglich sein.
- Beschleunigung, Verzögerung sowie zulässige Hardwaregrenzen berücksichtigen.

### Akzeptanzkriterium
Die manuelle Positionierung ist ungefähr doppelt so schnell wie bisher und erlaubt kontrollierte, längere Fahrten.

---

## Separate, dringende Diagnoseaufgabe (nicht Teil der Verbesserungsliste)

**Aktuelles Log laden und mindestens vier Unterbrechungen des heutigen Panoramalaufs untersuchen.** Für jede Unterbrechung Zeitstempel, unmittelbare Ereignisfolge, auslösende Bedingung und möglichst die tatsächliche Ursache rekonstruieren. Prüfen, ob alle Vorfälle dieselbe Ursache haben und ob statt eines Abbruchs eine Warnung genügt hätte. Insbesondere Achsbewegung, Stillstandserkennung, Settling-Zeit und Bildaufnahme betrachten.

**Status:** Logdatei noch nicht vorliegend; Ursachen nicht bestätigt.
