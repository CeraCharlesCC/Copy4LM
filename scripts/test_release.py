"""Release safety checks, using isolated metadata and synthetic plugin packages."""
import io
import json
import shutil
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

import release


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.version = release.current_version()
        for filename in (
            "gradle.properties", release.PLUGIN_XML, "vscode/package.json",
            "vscode/package-lock.json", "CHANGELOG.md", "vscode/CHANGELOG.md",
            ".github/workflows/script/update_plugin_change_notes.py",
        ):
            target = self.root / filename
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(release.ROOT / filename, target)
        self.root_patch = patch.object(release, "ROOT", self.root)
        self.root_patch.start()
        self.addCleanup(self.root_patch.stop)

    def packages(self, embedded_version=None):
        version = embedded_version or self.version
        directory = self.root / "artifacts"
        directory.mkdir(exist_ok=True)
        with zipfile.ZipFile(directory / f"copy4lm-vscode-{self.version}.vsix", "w") as archive:
            archive.writestr("extension/package.json", json.dumps({"version": version, "name": "copy4lm-vscode"}))
            archive.writestr("extension/dist/extension.js", "module.exports = {};")
        jar = io.BytesIO()
        with zipfile.ZipFile(jar, "w") as archive:
            archive.writestr("META-INF/plugin.xml", f"<idea-plugin><version>{version}</version>"
                             "<id>io.github.ceracharlescc.copy4lm</id></idea-plugin>")
        with zipfile.ZipFile(directory / f"copy4lm-intellij-{self.version}.zip", "w") as archive:
            archive.writestr("copy4lm/lib/copy4lm.jar", jar.getvalue())
        return directory

    def test_consistent_release(self):
        self.assertEqual(release.check(f"v{self.version}"), self.version)
        release.verify_artifacts(self.packages())

    def test_wrong_tag_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "must match"):
            release.check("v99.0.0")

    def test_lockfile_mismatch_is_rejected(self):
        path = self.root / "vscode/package-lock.json"
        lock = json.loads(path.read_text())
        lock["packages"][""]["version"] = "0.0.0"
        path.write_text(json.dumps(lock))
        with self.assertRaisesRegex(ValueError, "differs"):
            release.check()

    def test_missing_notes_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "needs release notes"):
            release.release_notes("99.0.0")

    def test_notes_stop_before_next_version(self):
        notes = release.release_notes(self.version)
        self.assertEqual(notes.count("\n## ["), 0)
        self.assertTrue(notes.startswith(f"## [{self.version}]"))

    def test_old_packages_cannot_be_released_accidentally(self):
        directory = self.packages()
        (directory / "copy4lm-vscode-0.0.1.vsix").write_bytes(b"stale")
        with self.assertRaisesRegex(ValueError, "Expected exactly"):
            release.verify_artifacts(directory)

    def test_mislabeled_vsix_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "VSIX manifest"):
            release.verify_artifacts(self.packages("0.0.1"))

    def test_mislabeled_intellij_zip_is_rejected(self):
        directory = self.packages()
        jar = io.BytesIO()
        with zipfile.ZipFile(jar, "w") as archive:
            archive.writestr("META-INF/plugin.xml", "<idea-plugin><version>0.0.1</version>"
                             "<id>io.github.ceracharlescc.copy4lm</id></idea-plugin>")
        with zipfile.ZipFile(directory / f"copy4lm-intellij-{self.version}.zip", "w") as archive:
            archive.writestr("copy4lm/lib/copy4lm.jar", jar.getvalue())
        with self.assertRaisesRegex(ValueError, "IntelliJ plugin descriptor"):
            release.verify_artifacts(directory)

    def test_bump_updates_all_metadata_without_git(self):
        path = self.root / "CHANGELOG.md"
        path.write_text("## [99.0.0] - 2026-10-25\n\n### Fixed\n- Example.\n\n" + path.read_text())
        release.bump("99.0.0")
        self.assertEqual(release.check("v99.0.0"), "99.0.0")
        self.assertIn("Version 99.0.0", (self.root / release.PLUGIN_XML).read_text())
        self.assertFalse((self.root / ".git").exists())

    def test_invalid_bump_leaves_files_unchanged(self):
        before = (self.root / "gradle.properties").read_bytes()
        for version in ("0.0.0", self.version, "v1.2.3", "01.2.3", "1.2.3; echo unsafe"):
            with self.subTest(version=version), self.assertRaises(ValueError):
                release.bump(version)
        with self.assertRaisesRegex(ValueError, "needs release notes"):
            release.bump("99.0.0")
        self.assertEqual((self.root / "gradle.properties").read_bytes(), before)


if __name__ == "__main__":
    unittest.main()
