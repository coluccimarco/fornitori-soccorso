"""Start a NEW local PostgreSQL cluster and the built jar, run browser E2E, stop both.

Set PG_BIN and JAVA_HOME. Requires playwright and psycopg[binary]. Uses only a newly
created temp directory and loopback ports; never connects to localhost:5432.
Run Maven verify first. Logs/screenshots remain in the printed temporary directory.
"""
import os
from pathlib import Path
import secrets
import subprocess
import sys
import tempfile
import time
import urllib.request
import psycopg
from psycopg import sql

ROOT = Path(__file__).resolve().parents[1]


def main():
    work = Path(tempfile.mkdtemp(prefix='fornitori-e2e-'))
    print(f'Isolated evidence directory: {work}', flush=True)
    pg = Path(os.environ['PG_BIN'])
    exe = '.exe' if os.name == 'nt' else ''
    hidden = {'creationflags': subprocess.CREATE_NO_WINDOW} if os.name == 'nt' else {}
    password = secrets.token_urlsafe(32)
    pgpassword_file = work / 'init-password'
    pgpassword_file.write_text(password, encoding='utf-8')
    env = os.environ.copy()
    env.update(PGHOST='127.0.0.1', PGPORT='55438', PGUSER='audit_admin',
               PGDATABASE='fornitori_e2e', PGPASSWORD=password)
    started = False
    app = None
    with (work / 'setup.log').open('w', encoding='utf-8') as log, (work / 'application.log').open('w', encoding='utf-8') as app_log:
        def run(name, *args):
            subprocess.run([str(pg / (name + exe)), *map(str, args)], env=env,
                           stdout=log, stderr=subprocess.STDOUT, check=True, **hidden)
        try:
            run('initdb', '-D', work / 'data', '-U', 'audit_admin', '-A', 'scram-sha-256',
                '--pwfile=' + str(pgpassword_file), '--encoding=UTF8', '--locale=C')
            pgpassword_file.unlink()
            with (work / 'data' / 'postgresql.conf').open('a') as config:
                config.write("\nlisten_addresses='127.0.0.1'\nport=55438\n")
            run('pg_ctl', '-D', work / 'data', '-l', work / 'postgres.log', '-w', 'start')
            started = True
            run('createdb', 'fornitori_e2e')
            for script in sorted((ROOT / 'database').glob('*.sql')):
                run('psql', '-X', '-v', 'ON_ERROR_STOP=1', '-f', script)
            # Separate least-privilege runtime role; schema and seed owned by the temporary admin.
            with psycopg.connect(host=env['PGHOST'], port=env['PGPORT'], user=env['PGUSER'],
                                 password=password, dbname=env['PGDATABASE'], autocommit=True) as db:
                db.execute(sql.SQL('CREATE ROLE e2e_app LOGIN PASSWORD {}').format(sql.Literal(password)))
                db.execute('GRANT USAGE ON SCHEMA public TO e2e_app')
                db.execute('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO e2e_app')
                db.execute('GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO e2e_app')
                db.execute('CREATE EXTENSION IF NOT EXISTS pgcrypto')
                env['E2E_PASSWORD'] = secrets.token_urlsafe(24)
                db.execute("UPDATE app_users SET password_hash=crypt(%s, gen_salt('bf', 10))", (env['E2E_PASSWORD'],))
                assert db.execute("select rolsuper from pg_roles where rolname='e2e_app'").fetchone() == (False,)
            app_env = {k: v for k, v in env.items() if not k.startswith('PG') and not k.startswith('E2E')}
            app_env.update(SPRING_DATASOURCE_URL='jdbc:postgresql://127.0.0.1:55438/fornitori_e2e',
                           SPRING_DATASOURCE_USERNAME='e2e_app', SPRING_DATASOURCE_PASSWORD=password,
                           SERVER_ADDRESS='127.0.0.1', SERVER_PORT='58880', DEBUG='false')
            jar = ROOT / 'target' / 'fornitori-0.0.1-SNAPSHOT.jar'
            app = subprocess.Popen([str(Path(env['JAVA_HOME']) / 'bin' / ('java' + exe)), '-jar', str(jar)],
                                   cwd=ROOT, env=app_env, stdout=app_log, stderr=subprocess.STDOUT, **hidden)
            base = 'http://127.0.0.1:58880'
            for _ in range(60):
                if app.poll() is not None: raise RuntimeError('Application exited: inspect application.log')
                try:
                    with urllib.request.urlopen(base + '/api/v1/auth/csrf', timeout=1) as response:
                        if response.status == 200: break
                except (OSError, TimeoutError): time.sleep(1)
            else: raise RuntimeError('Application did not become ready')
            env.update(E2E_BASE_URL=base, E2E_ISOLATED='1', E2E_OUTPUT=str(work))
            result = subprocess.run([sys.executable, '-X', 'utf8', str(ROOT / 'tools/browser_e2e.py')],
                                    cwd=ROOT, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                    encoding='utf-8', **hidden)
            (work / 'browser-tests.log').write_text(result.stdout, encoding='utf-8')
            print(result.stdout, flush=True)
            return result.returncode
        finally:
            if app is not None:
                app.terminate()
                try: app.wait(timeout=20)
                except subprocess.TimeoutExpired: app.kill(); app.wait()
            if started: run('pg_ctl', '-D', work / 'data', '-m', 'fast', '-w', 'stop')
            pgpassword_file.unlink(missing_ok=True)


if __name__ == '__main__':
    raise SystemExit(main())
