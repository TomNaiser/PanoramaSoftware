"""Check our Markdown, relative references, generated source hashes and diagram inventory."""
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import unquote, urlsplit
import hashlib
import json
import re
import subprocess
import sys

from generate_reference import ROOT, source_files, source_sha256

DOCS = ROOT / "docs"

def public_files():
    """Check working-copy publication candidates, including not-yet-committed docs."""
    result = set()
    for repo in (ROOT, ROOT / "PanoduinoControlGitRepo", ROOT / "PanoramaControlGitRepo"):
        raw = subprocess.check_output(["git", "-C", str(repo), "ls-files", "--cached",
                                       "--others", "--exclude-standard", "-z"])
        result.update((repo / name).resolve() for name in raw.decode("utf-8").split("\0") if name)
    return result

def markdown_files(published):
    return sorted(path for path in published if path.suffix == ".md" and path.is_file())

def scan_markdown(path):
    prose, diagrams = [], []
    fence = None
    code = []
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        match = re.match(r"^\s{0,3}(`{3,}|~{3,})(.*)$", line)
        if fence:
            if match and match[1][0] == fence[0] and len(match[1]) >= fence[1] and not match[2].strip():
                if fence[2] == "mermaid":
                    diagrams.append({"file": path.relative_to(ROOT).as_posix(), "line": fence[3], "source": "\n".join(code)})
                fence, code = None, []
            else:
                code.append(line)
        elif match:
            fence = (match[1][0], len(match[1]), match[2].strip(), number)
        else:
            prose.append((number, line))
    if fence:
        raise ValueError(f"Unclosed fence: {path}:{fence[3]}")
    return prose, diagrams

def diagram_digest(diagrams):
    return hashlib.sha256(json.dumps(diagrams, ensure_ascii=False, separators=(",", ":")).encode()).hexdigest()

def validate():
    published = public_files()
    files = markdown_files(published)
    errors, diagrams = [], []
    local_links = external_links = 0
    for path in files:
        raw = path.read_text(encoding="utf-8")
        if "\ufffd" in raw:
            errors.append(f"Replacement character in {path}")
        try:
            prose, blocks = scan_markdown(path)
        except ValueError as ex:
            errors.append(str(ex));continue
        diagrams.extend(blocks)
        for number, line in prose:
            for match in re.finditer(r"!?\[[^\]]*\]\((<[^>]+>|[^\s)]+)\)", line):
                target = match[1].strip("<>")
                url = urlsplit(target)
                if url.scheme or url.netloc:
                    external_links += 1;continue
                local_links += 1
                destination = (path.parent / unquote(url.path)).resolve() if url.path else path
                if not destination.is_file():
                    errors.append(f"Missing target {path.relative_to(ROOT)}:{number}: {target}")
                elif destination not in published:
                    errors.append(f"Target excluded from public repository {path.relative_to(ROOT)}:{number}: {target}")
                elif url.fragment.startswith("L") and destination.suffix in {".java", ".cs", ".h", ".cpp", ".ino"}:
                    try:
                        index = int(url.fragment[1:])
                        if not 1 <= index <= len(destination.read_text(encoding="utf-8-sig").splitlines()):
                            raise ValueError()
                    except ValueError:
                        errors.append(f"Invalid source line {target}")
    manifest = json.loads((DOCS / "referenz/stand.json").read_text(encoding="utf-8"))
    if manifest.get("sourceHashNormalization") != "CRLF-to-LF":
        errors.append("Unsupported or missing source hash normalization")
    actual = {p.relative_to(ROOT).as_posix() for _, paths in source_files() for p in paths}
    recorded = {source["path"] for source in manifest["sources"]}
    if actual != recorded:
        errors.append("Generated production file inventory is stale")
    for source in manifest["sources"]:
        path = ROOT / source["path"]
        if not path.is_file() or source_sha256(path) != source["sha256"]:
            errors.append("Changed source since generation: " + source["path"])
    parameter_source = (ROOT / "PanoduinoControlGitRepo/PanoDuinoControl/MotionParameters.h").read_text()
    count = len(re.findall(r"^MOTION_PARAMETER\(", parameter_source, re.M))
    table = (DOCS / "referenz/motorparameter.md").read_text(encoding="utf-8")
    indices = [int(value) for value in re.findall(r"^\| (\d+) \|", table, re.M)]
    if count != manifest["motorParameters"] or indices != list(range(count)):
        errors.append("Parameter catalog missing or out of order")
    digest = diagram_digest(diagrams)
    render_report = DOCS / "mermaid-pruefbericht.json"
    if render_report.exists():
        rendered = json.loads(render_report.read_text(encoding="utf-8"))
        if rendered.get("diagramManifestSha256") != digest or rendered.get("failures") != 0:
            errors.append("Mermaid rendering result is stale or failed")
    report = {"checkedAtUtc": datetime.now(timezone.utc).isoformat(), "markdownFiles": len(files),
              "relativeLinksChecked": local_links, "externalLinksListedNotNetworkChecked": external_links,
              "mermaidDiagrams": len(diagrams), "diagramManifestSha256": digest,
              "motorParameters": count, "productionFilesHashChecked": len(recorded),
              "errors": errors, "passed": not errors}
    (DOCS / "pruefbericht.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporary = DOCS / ".validation"
    temporary.mkdir(exist_ok=True)
    (temporary / "diagrams.json").write_text(json.dumps(diagrams, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return not errors

if __name__ == "__main__":
    sys.exit(0 if validate() else 1)
