"""Fetch the official MDK wrapper/license and record downloaded file hashes."""
import json
from pathlib import Path
import hashlib
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
REPO = 'NeoForgeMDKs/MDK-1.21.1-ModDevGradle'


def fetch(url):
    for attempt in range(3):
        try:
            request = urllib.request.Request(url, headers={'User-Agent': 'Better-Forbidden-Arcanus-setup'})
            with urllib.request.urlopen(request, timeout=30) as response:
                return response.read()
        except Exception:
            if attempt == 2:
                raise


def main():
    wanted = {'gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar',
              'gradle/wrapper/gradle-wrapper.properties', 'TEMPLATE_LICENSE.txt'}
    hashes = {}
    for relative in sorted(wanted):
        destination = ROOT / relative
        if not destination.exists():
            body = fetch(f'https://raw.githubusercontent.com/{REPO}/main/{relative}')
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(body)
        hashes[relative] = hashlib.sha256(destination.read_bytes()).hexdigest()
        print(f'Ready: {relative}', flush=True)
    docs = ROOT / 'docs'
    docs.mkdir(exist_ok=True)
    (docs / 'MDK_SOURCE.md').write_text(
        f'# Official MDK baseline\n\nRepository: https://github.com/{REPO}\n\n'
        'Source branch: `main` (GitHub API and Git access unavailable during setup).\n\n'
        'Wrapper and template license downloaded directly from official raw URLs. '
        'Exact downloaded bytes recorded by SHA-256 below.\n\n```json\n'
        + json.dumps(hashes, indent=2) + '\n```\n', encoding='utf-8')
    print((ROOT / 'gradle/wrapper/gradle-wrapper.properties').read_text())


if __name__ == '__main__':
    main()
