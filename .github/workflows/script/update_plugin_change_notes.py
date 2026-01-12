#!/usr/bin/env python3
import argparse
import html
import re
from pathlib import Path

def extract_changelog_section(changelog_text: str, version: str) -> str:
    """
    Extract markdown content under header like: ## [0.1.0]
    until the next ## [ ... ] header or EOF.
    """
    # Match exact version header
    pat = re.compile(rf'(?m)^\#\#\s+\[{re.escape(version)}\].*$')
    m = pat.search(changelog_text)
    if not m:
        return ""

    start = m.end()
    # Find next version header
    next_pat = re.compile(r'(?m)^\#\#\s+\[.+\].*$')
    m2 = next_pat.search(changelog_text, start)
    end = m2.start() if m2 else len(changelog_text)

    section = changelog_text[start:end].strip("\n").strip()
    return section

def md_to_simple_html(md: str, version: str) -> str:
    """
    Minimal markdown -> HTML suitable for plugin.xml change-notes.
    Supports:
      - ### headings -> <h4>
      - bullet lists (-, *) -> <ul><li>
      - paragraphs -> <p>
    Escapes HTML in text.
    """
    lines = [ln.rstrip() for ln in md.splitlines()]
    out = [f"<h3>Version {html.escape(version)} Changes:</h3>"]

    i = 0
    in_ul = False

    def close_ul():
        nonlocal in_ul
        if in_ul:
            out.append("</ul>")
            in_ul = False

    def open_ul():
        nonlocal in_ul
        if not in_ul:
            out.append("<ul>")
            in_ul = True

    def is_bullet(s: str) -> bool:
        return bool(re.match(r'^\s*[-*]\s+.+', s))

    while i < len(lines):
        s = lines[i].strip()

        if not s:
            close_ul()
            i += 1
            continue

        # ### Heading
        if s.startswith("### "):
            close_ul()
            out.append(f"<h4>{html.escape(s[4:].strip())}</h4>")
            i += 1
            continue

        # Bullet list
        if is_bullet(s):
            open_ul()
            item = re.sub(r'^\s*[-*]\s+', '', s).strip()
            out.append(f"<li>{html.escape(item)}</li>")
            i += 1
            continue

        # Paragraph (collect consecutive non-empty, non-bullet, non-heading lines)
        close_ul()
        para = [s]
        j = i + 1
        while j < len(lines):
            t = lines[j].strip()
            if not t or t.startswith("### ") or is_bullet(t):
                break
            para.append(t)
            j += 1
        out.append(f"<p>{html.escape(' '.join(para))}</p>")
        i = j

    close_ul()
    return "\n".join(out)

def replace_change_notes(plugin_xml_text: str, new_html: str) -> str:
    """
    Replace content inside <change-notes>...</change-notes> with a CDATA block containing new_html.
    Preserves indentation based on the <change-notes> line.
    """
    m = re.search(r'(?m)^([ \t]*)<change-notes>\s*$', plugin_xml_text)
    if not m:
        raise RuntimeError("Could not find <change-notes> start tag on its own line.")
    indent = m.group(1)

    start_tag_pos = m.start()
    end_tag_match = re.search(r'(?s)</change-notes>', plugin_xml_text[m.end():])
    if not end_tag_match:
        raise RuntimeError("Could not find </change-notes> end tag.")

    content_start = m.end()
    content_end = m.end() + end_tag_match.start()

    inner_indent = indent + "    "
    html_lines = new_html.splitlines() or [""]
    indented_html = "\n".join(inner_indent + ln for ln in html_lines)

    replacement = (
        f"{indent}<change-notes>\n"
        f"{inner_indent}<![CDATA[\n"
        f"{indented_html}\n"
        f"{inner_indent}]]>\n"
        f"{indent}</change-notes>"
    )

    # Replace the whole block from <change-notes> line to </change-notes>
    block_end = m.end() + end_tag_match.end()
    return plugin_xml_text[:start_tag_pos] + replacement + plugin_xml_text[block_end:]

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--plugin-xml", required=True)
    ap.add_argument("--changelog", required=True)
    ap.add_argument("--version", required=True, help="Version without 'v' prefix (e.g. 0.1.0)")
    args = ap.parse_args()

    plugin_path = Path(args.plugin_xml)
    changelog_path = Path(args.changelog)

    plugin_xml = plugin_path.read_text(encoding="utf-8")
    changelog = changelog_path.read_text(encoding="utf-8")

    section = extract_changelog_section(changelog, args.version)
    if not section:
        # Fallback: still write something deterministic
        new_html = f"<h3>Version {html.escape(args.version)} Changes:</h3>\n<p>No release notes were found in CHANGELOG.md.</p>"
    else:
        new_html = md_to_simple_html(section, args.version)

    updated = replace_change_notes(plugin_xml, new_html)
    plugin_path.write_text(updated, encoding="utf-8")

if __name__ == "__main__":
    main()
