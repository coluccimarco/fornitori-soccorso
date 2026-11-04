"""Query OSV for the exact versions in `mvn dependency:list -DoutputFile=target/dependencies.txt`.

No credentials or source files are sent. An unavailable scanner fails explicitly.
Results are point-in-time advisory matches, not a security certification.
"""
import json
from pathlib import Path
import re
import urllib.request
from datetime import datetime, timezone


def main():
    packages = []
    for line in Path('target/dependencies.txt').read_text(encoding='utf-8').splitlines():
        match = re.match(r'\s+([^: ]+):([^: ]+):[^: ]+(?::[^: ]+)?:([^: ]+):(compile|runtime|test|provided)\b', line)
        if match:
            group, artifact, version, scope = match.groups()
            packages.append({'package': {'name': group + ':' + artifact, 'ecosystem': 'Maven'}, 'version': version})
    if not packages:
        raise RuntimeError('No resolved Maven dependencies found')
    request = urllib.request.Request('https://api.osv.dev/v1/querybatch',
        data=json.dumps({'queries': packages}).encode(), headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=45) as response:
        result = json.load(response)
    if len(result['results']) != len(packages):
        raise RuntimeError('Incomplete scanner response')
    findings = [{'package': package, 'vulns': response.get('vulns', [])}
                for package, response in zip(packages, result['results']) if response.get('vulns')]
    report = {'checkedAt': datetime.now(timezone.utc).isoformat(), 'dependencies': len(packages), 'findings': findings}
    Path('target/dependency-security.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report, indent=2))
    return 1 if findings else 0


if __name__ == '__main__':
    raise SystemExit(main())
