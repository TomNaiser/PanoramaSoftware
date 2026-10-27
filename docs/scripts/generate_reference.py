"""Generate code-backed documentation. Standard library only; never changes product sources."""
from pathlib import Path
from datetime import date
from urllib.parse import quote
import hashlib
import json
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs/referenz"

def text(path):
    return path.read_text(encoding="utf-8-sig")

def write(name, value):
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / name).write_text(value, encoding="utf-8", newline="\n")

def clean_code(code):
    pattern = r'//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\''
    return re.sub(pattern, lambda m: re.sub(r"[^\n]", " ", m.group()), code)

def source_files():
    firmware = ROOT / "PanoduinoControlGitRepo/PanoDuinoControl"
    android = ROOT / "PanoramaAndroid/app/src/main/java"
    windows = ROOT / "PanoramaControlGitRepo/PanoramaControl"
    groups = [("Firmware", sorted(p for p in firmware.iterdir() if p.suffix in {".h", ".cpp", ".ino"})),
              ("Android", sorted(android.rglob("*.java")))]
    project = ET.parse(windows / "PanoramaControl Version 27.csproj")
    files = []
    for node in project.findall(".//{*}Compile"):
        include = node.get("Include", "").replace("\\", "/")
        if not include.endswith(".cs") or include.endswith(".Designer.cs") or include.startswith("Properties/"):
            continue
        path = windows / include
        if path.is_file():
            files.append(path)
    groups.append(("Windows", sorted(set(files))))
    return groups

SIGNATURE = re.compile(
    r"(?m)^[ \t]*(?P<prefix>(?:[A-Za-z_][\w:.<>\[\],?*&]*[ \t]+)*)"
    r"[*&]*"
    r"(?P<name>(?:[A-Za-z_]\w*::)*~?[A-Za-z_]\w*)[ \t]*"
    r"\((?P<args>[^;{}]*?)\)[ \t\r\n]*"
    r"(?:(?:const|override|noexcept)[ \t\r\n]*)*"
    r"(?:throws[ \t]+[\w., \t]+[ \t\r\n]*)?"
    r"(?::[^;{}]*?)?(?:=[ \t]*0[ \t]*)?(?P<end>\{|;|=>)"
)
MODIFIERS = {"public", "private", "protected", "internal", "static", "final", "synchronized", "virtual", "explicit", "inline", "override", "abstract", "async", "sealed", "native", "extern"}
CONTROL = {"if", "for", "while", "switch", "catch", "return", "throw", "new", "using", "else", "delete", "sizeof", "assert", "await", "lock", "checked", "unchecked", "fixed", "nameof", "typeof"}

def declarations(raw):
    clean = clean_code(raw)
    types = sorted(set(re.findall(r"\b(?:class|struct|enum|interface)\s+([A-Za-z_]\w*)", clean)))
    found = []
    for match in SIGNATURE.finditer(clean):
        name = match["name"]
        prefix = match["prefix"].strip().split()
        if name in CONTROL or any(word in CONTROL for word in prefix):
            continue
        returns = [part for part in prefix if part not in MODIFIERS]
        scoped = name.split("::")
        constructor = len(scoped)>1 and scoped[-1].lstrip("~")==scoped[-2]
        if not returns and not constructor and scoped[-1].lstrip("~") not in types:
            continue
        line = raw.count("\n", 0, match.start()) + 1
        signature = " ".join(raw[match.start():match.start("end")].split()).split(" : ",1)[0]
        found.append((line, signature))
    return types, found

def source_link(path, line=None):
    relative = path.relative_to(ROOT).as_posix()
    for prefix, repo in [("PanoduinoControlGitRepo/", "PanoDuinoControl"), ("PanoramaControlGitRepo/", "PanoramaControl")]:
        if relative.startswith(prefix):
            return "https://github.com/TomNaiser/" + repo + "/blob/master/" + quote(relative[len(prefix):], safe="/") + (f"#L{line}" if line else "")
    return "../../" + quote(relative, safe="/") + (f"#L{line}" if line else "")

def parameters():
    source = ROOT / "PanoduinoControlGitRepo/PanoDuinoControl/MotionParameters.h"
    pattern = re.compile(r'^MOTION_PARAMETER\((\w+),\s*"([^"]+)",\s*"([^"]+)",\s*(\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+)\)', re.M)
    rows = pattern.findall(text(source))
    assert len(rows) == text(source).count("MOTION_PARAMETER("), "Unparsed parameter declaration"
    lines = ["# Vollständiger Motorparameterkatalog", "", "[Inhalt](../README.md) · [Bedeutung der Variablen](../07-variablen-und-parameter.md)", "",
             "Automatisch aus `PanoDuinoControl/MotionParameters.h` generiert. Indexreihenfolge entspricht `MotionParameterId`. **Code-Defaults, keine gelesenen Gerätewerte.** Die gültige Gesamtkonfiguration benötigt zusätzlich `MotionSettings::validate()` und passende Revision.", "",
             f"**{len(rows)} Parameter**, Schema 1. Zahlen in den drei letzten Spalten sind rohe Festkomma-Ganzzahlen. Fachwert = Rohwert / Skalierung. Beispiel: 40.000 bei scale 1.000 bedeutet PWM 40, nicht 40 Prozent.", "",
             "| Index | Enum-ID | Konfigurationsname | Einheit | Skalierung | Default roh | Minimum roh | Maximum roh |", "|---:|---|---|---|---:|---:|---:|---:|"]
    for index, (symbol, name, unit, scale, default, minimum, maximum) in enumerate(rows):
        assert int(minimum) <= int(default) <= int(maximum) and int(scale) > 0
        lines.append(f"| {index} | `MP_{symbol}` | `{name}` | {unit} | {scale} | {default} | {minimum} | {maximum} |")
    lines += ["", "Die Katalogreihenfolge ist Teil des gespeicherten Profils. Neue Parameter dürfen nicht unbemerkt bestehende Indizes umsortieren. Firmware kann ältere Präfix-Profile migrieren; aktuelle Werte und Revision immer über den Gerätevertrag lesen.", ""]
    write("motorparameter.md", "\n".join(lines))
    return len(rows)

def source_sha256(path):
    """Hash source text consistently across Git LF and Windows CRLF checkouts."""
    return hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()


def generate():
    assert len(declarations("const char *PanoramaExecutor::start(uint32_t id) { return 0; }")[1])==1
    assert not declarations("await UploadPlanAsync(token);")[1]
    assert len(declarations("public static async Task UploadPlanAsync(CancellationToken t) { }")[1])==1
    count = parameters()
    catalog = ["# Quellcode-Referenz", "", "[Inhalt](../README.md) · [Funktionssignaturen](funktionen.md)", "",
               "Erfasst sind die aktuell ausgecheckten Firmware-Quellen, Android-Produktionsklassen und im Windows-Version-27-Projekt eingebundenen C#-Dateien; generierte Designer-/AssemblyInfo-Dateien und Tests ausgenommen. Der Katalog dokumentiert Quellen, keine zusätzliche Hardwareabnahme. Die Versions- und Messnachweise stehen im [Prüfstand](../validation/README.md). Ein eingebundener Legacy-Helfer ist nicht automatisch der aktive V2-Pfad.", "",
               "Windows/Firmware-Quelllinks zeigen auf ihre eigenen GitHub-Repositories (`master`). Der lokale Dateipfad bleibt im Text erkennbar. Für publizierte Releases Links auf erreichbare Commitstände fixieren; lokale unpublizierte Änderungen werden auf GitHub erst nach deren Veröffentlichung sichtbar.", ""]
    api = ["# Funktions- und Methodensignaturen", "", "[Inhalt](../README.md) · [Dateien und Typen](quellcode.md)", "",
           "Automatisch lexikalisch erfasste Deklarationen/Definitionen aus den Produktionsdateien. Die Listen dienen zum Auffinden im Code und ergänzen die erklärten Funktionen der Fachkapitel. Enthalten sind auch interne Helfer sowie durch Buildflags eingeschlossene Varianten. Headerdeklaration und Implementierung können beide erscheinen.", "",
           "Kein Compiler-API-Dump: komplexe Operatoren, manche Konstruktor-Initialisierungen, Properties ohne Parameter und anonyme/lambda-basierte Handler können fehlen. Es wird keine stabile öffentliche ABI behauptet. Variable-/Datenmodellreferenz und vollständiger Parameterkatalog sind getrennt dokumentiert.", ""]
    sources = []
    function_count = 0
    for group, paths in source_files():
        catalog += [f"## {group}", "", "| Datei | Deklarierte Typen | Erfasste Signaturen |", "|---|---|---:|"]
        api += [f"## {group}", ""]
        for path in paths:
            raw = text(path)
            types, functions = declarations(raw)
            relative = path.relative_to(ROOT).as_posix()
            label = path.name
            catalog.append(f"| [{label}]({source_link(path)}) | {', '.join('`'+t+'`' for t in types) or 'Freie Funktionen / Definitionen'} | {len(functions)} |")
            sources.append({"component": group, "path": relative, "sha256": source_sha256(path), "signatures": len(functions)})
            function_count += len(functions)
            if functions:
                api += [f"### {relative}", ""]
                for line, signature in functions:
                    safe = signature.replace("|", "&#124;").replace("`", "'")
                    api.append(f"- [L{line}]({source_link(path, line)}): `{safe}`")
                api.append("")
        catalog.append("")
    write("quellcode.md", "\n".join(catalog))
    write("funktionen.md", "\n".join(api))
    firmware = re.search(r'PANODUINO_FIRMWARE_VERSION\s+"([^"]+)"', text(ROOT / "PanoduinoControlGitRepo/PanoDuinoControl/VersionInfo.h"))[1]
    android = re.search(r"versionName '([^']+)'", text(ROOT / "PanoramaAndroid/app/build.gradle"))[1]
    project = ET.parse(ROOT / "PanoramaControlGitRepo/PanoramaControl/PanoramaControl Version 27.csproj")
    windows = project.find(".//{*}ApplicationVersion").text
    manifest = {"documentationDate": date.today().isoformat(), "firmware": firmware, "android": android, "windows": windows,
                "motorParameters": count, "productionFiles": len(sources), "lexicallyDetectedSignatures": function_count,
                "sourceHashNormalization": "CRLF-to-LF", "sources": sources}
    write("stand.json", json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")
    print(f"Generated: {count} parameters, {len(sources)} production files, {function_count} signatures")

if __name__ == "__main__":
    generate()
