#!/usr/bin/env python3
"""Dependency-free version, release-note, and packaged-artifact checks."""
import argparse
import io
import json
import re
import subprocess
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION = r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)"
PLUGIN_XML = Path("intellij/src/main/resources/META-INF/plugin.xml")


def validate_version(version):
    if not re.fullmatch(VERSION, version):
        raise ValueError("Use a stable version in X.Y.Z format (for example, 0.4.1)")
    return version


def current_version():
    text = (ROOT / "gradle.properties").read_text()
    match = re.search(r"^pluginVersion\s*=\s*(\S+)\s*$", text, re.M)
    if not match:
        raise ValueError("pluginVersion is missing from gradle.properties")
    return validate_version(match[1])


def release_notes(version, path=None):
    text = (path or ROOT / "CHANGELOG.md").read_text()
    match = re.search(rf"^## \[{re.escape(version)}\][^\n]*\n(.*?)(?=^## |\Z)", text, re.M | re.S)
    if not match or not match[1].strip():
        raise ValueError(f"Changelog needs release notes for {version}")
    return match[0].strip() + "\n"


def check(tag=None):
    version = current_version()
    if tag is not None and tag != f"v{version}":
        raise ValueError(f"Tag {tag!r} must match v{version}")
    versions = {
        str(PLUGIN_XML): ET.parse(ROOT / PLUGIN_XML).getroot().findtext("version"),
        "vscode/package.json": json.loads((ROOT / "vscode/package.json").read_text())["version"],
    }
    lock = json.loads((ROOT / "vscode/package-lock.json").read_text())
    versions["vscode/package-lock.json"] = lock["version"]
    versions["vscode/package-lock.json packages['']"] = lock["packages"][""]["version"]
    for path, actual in versions.items():
        if actual != version:
            raise ValueError(f"{path}: {actual!r} differs from pluginVersion {version}")
    release_notes(version)
    if (ROOT / "CHANGELOG.md").read_bytes() != (ROOT / "vscode/CHANGELOG.md").read_bytes():
        raise ValueError("Root and VS Code changelogs must match")
    return version


def bump(version):
    validate_version(version)
    previous = current_version()
    if tuple(map(int, version.split('.'))) <= tuple(map(int, previous.split('.'))):
        raise ValueError(f"New version must be higher than {previous}")
    # Check notes before changing any files.
    release_notes(version)
    path = ROOT / "gradle.properties"
    path.write_text(re.sub(r"^pluginVersion\s*=.*$", f"pluginVersion={version}", path.read_text(), flags=re.M))
    path = ROOT / PLUGIN_XML
    path.write_text(re.sub(r"<version>[^<]*</version>", f"<version>{version}</version>", path.read_text(), count=1))
    for filename in ("package.json", "package-lock.json"):
        path = ROOT / "vscode" / filename
        data = json.loads(path.read_text())
        data["version"] = version
        if filename == "package-lock.json":
            data["packages"][""]["version"] = version
        path.write_text(json.dumps(data, indent=2) + "\n")
    (ROOT / "vscode/CHANGELOG.md").write_bytes((ROOT / "CHANGELOG.md").read_bytes())
    subprocess.run([
        "python3", str(ROOT / ".github/workflows/script/update_plugin_change_notes.py"),
        "--plugin-xml", str(ROOT / PLUGIN_XML), "--changelog", str(ROOT / "CHANGELOG.md"),
        "--version", version,
    ], check=True)
    check()
    print(f"Updated {previous} -> {version}. Review and commit, then run Release in GitHub Actions.")


def verify_artifacts(directory):
    version = check()
    expected = {f"copy4lm-intellij-{version}.zip", f"copy4lm-vscode-{version}.vsix"}
    actual = {p.name for p in directory.iterdir() if p.suffix in (".zip", ".vsix")}
    if actual != expected:
        raise ValueError(f"Expected exactly {sorted(expected)}, found {sorted(actual)}")
    with zipfile.ZipFile(directory / f"copy4lm-vscode-{version}.vsix") as archive:
        package = json.loads(archive.read("extension/package.json"))
        if package["version"] != version or package["name"] != "copy4lm-vscode":
            raise ValueError("VSIX manifest has the wrong name/version")
        if not archive.read("extension/dist/extension.js"):
            raise ValueError("VSIX is missing the extension bundle")
    descriptors = []
    with zipfile.ZipFile(directory / f"copy4lm-intellij-{version}.zip") as archive:
        for name in archive.namelist():
            if name.endswith(".jar"):
                with zipfile.ZipFile(io.BytesIO(archive.read(name))) as jar:
                    if "META-INF/plugin.xml" in jar.namelist():
                        descriptors.append(ET.fromstring(jar.read("META-INF/plugin.xml")))
    if len(descriptors) != 1:
        raise ValueError("Expected one IntelliJ plugin descriptor")
    if descriptors[0].findtext("version") != version or descriptors[0].findtext("id") != "io.github.ceracharlescc.copy4lm":
        raise ValueError("IntelliJ plugin descriptor has the wrong id/version")
    print(f"Verified IntelliJ ZIP and VSIX contents for {version}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("bump", help="Update all version files and plugin change notes; never commit or push").add_argument("version")
    commands.add_parser("check", help="Check version and changelog consistency").add_argument("--tag")
    commands.add_parser("notes", help="Print the current version's release notes").add_argument("--changelog", type=Path)
    commands.add_parser("verify-artifacts", help="Check packaged manifests and artifact names").add_argument("directory", type=Path)
    args = parser.parse_args()
    try:
        if args.command == "bump":
            bump(args.version)
        elif args.command == "check":
            print(check(args.tag))
        elif args.command == "notes":
            print(release_notes(check(), args.changelog), end="")
        else:
            verify_artifacts(args.directory)
    except (ValueError, KeyError, FileNotFoundError, zipfile.BadZipFile, ET.ParseError) as error:
        parser.exit(1, f"Release validation failed: {error}\n")


if __name__ == "__main__":
    main()
