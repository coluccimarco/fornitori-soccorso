"""Real browser -> Spring Boot -> PostgreSQL tests. Only for an isolated synthetic DB.

Requires playwright, psycopg[binary], a Playwright Chromium installation, and environment:
E2E_BASE_URL, E2E_PASSWORD, PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD, E2E_ISOLATED=1.
The caller owns startup/teardown. Never run against a personal or shared database.
"""
import os
from pathlib import Path
import unittest
from decimal import Decimal
import psycopg
from playwright.sync_api import sync_playwright, expect


class BrowserDatabaseTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if os.environ.get("E2E_ISOLATED") != "1" or not os.environ["PGDATABASE"].startswith("fornitori_e2e"):
            raise RuntimeError("Explicit isolated database required")
        cls.db = psycopg.connect(autocommit=True)
        cls.playwright = sync_playwright().start()
        cls.browser = cls.playwright.chromium.launch(headless=True)
        cls.base = os.environ["E2E_BASE_URL"]

    @classmethod
    def tearDownClass(cls):
        cls.browser.close()
        cls.playwright.stop()
        cls.db.close()

    def setUp(self):
        self.contexts = []
        self.errors = []

    def tearDown(self):
        for context in self.contexts:
            context.close()
        self.assertEqual([], self.errors, "Unexpected browser JS errors")

    def login(self, email):
        context = self.browser.new_context()
        self.contexts.append(context)
        page = context.new_page()
        page.on("pageerror", lambda error: self.errors.append(str(error)))
        page.goto(self.base)
        page.locator('[data-show-login]:visible').first.click()
        page.locator('#loginEmail').fill(email)
        page.locator('#loginPassword').fill(os.environ['E2E_PASSWORD'])
        page.locator('#loginButton').click()
        expect(page.locator('#appView')).to_be_visible()
        return page

    def scalar(self, sql, *params):
        return self.db.execute(sql, params).fetchone()[0]

    def api(self, page, path, method='GET', body=None, csrf=True):
        return page.evaluate('''async ({path, method, body, csrf}) => {
          const headers = {};
          if (csrf) {
            const token = await (await fetch('/api/v1/auth/csrf')).json();
            headers[token.headerName] = token.token;
          }
          if (body !== null) headers['Content-Type'] = 'application/json';
          const response = await fetch(path, {method, headers, credentials:'same-origin',
            ...(body !== null ? {body:JSON.stringify(body)} : {})});
          return {status:response.status, body:response.status === 204 ? null : await response.json()};
        }''', dict(path=path, method=method, body=body, csrf=csrf))

    def create_ui(self, manager, description):
        manager.locator('button[data-view-link="missioni"]').first.click()
        manager.locator('#newMissionButton').click()
        manager.locator('#missionCustomer').select_option('1')
        manager.locator('#missionType').select_option('riparazione_sul_posto')
        manager.locator('#missionOrigin').fill('Via Roma 1, Molfetta')
        manager.locator('#missionProblem').fill(description)
        with manager.expect_response(lambda r: r.url.endswith('/api/v1/missioni') and r.request.method == 'POST') as response:
            manager.locator('#saveMissionButton').click()
        self.assertEqual(201, response.value.status)
        mission = response.value.json()
        expect(manager.locator('#missionModal')).not_to_be_visible()
        self.assertEqual('nuova', self.scalar('select stato from missions where id=%s', mission['id']))
        return mission['id']

    def available_driver(self, manager):
        drivers = self.api(manager, '/api/v1/autisti')['body']
        driver = next((item for item in drivers if item['disponibilita'] == 'disponibile'), None)
        self.assertIsNotNone(driver, 'No driver is available for the isolated E2E mission')
        email = self.scalar(
            'select u.email from app_users u join drivers d on d.user_id=u.id where d.id=%s',
            driver['id'])
        return driver['id'], email

    def other_driver_email(self, driver_id):
        return self.scalar(
            'select u.email from app_users u join drivers d on d.user_id=u.id '
            'where d.id<>%s order by d.id limit 1',
            driver_id)

    def propose_ui(self, manager, mission_id, driver_id):
        driver_option = manager.locator(f'#driver-{mission_id} option[value="{driver_id}"]')
        expect(driver_option).to_be_enabled()
        manager.locator(f'#driver-{mission_id}').select_option(str(driver_id))
        with manager.expect_response(lambda r: r.url.endswith(f'/{mission_id}/proposte')) as response:
            manager.locator(f'[data-propose="{mission_id}"]').click()
        self.assertEqual(200, response.value.status)
        self.assertEqual('proposta', self.scalar('select stato from missions where id=%s', mission_id))

    def test_a_responsabile_autista_committed_round_trip(self):
        fixtures = self.scalar('select count(*) from missions where is_fixture')
        manager = self.login('responsabile@demo.it')
        expect(manager.locator('#customerCount')).to_have_text('40')
        manager.locator('button[data-view-link="clienti"]').first.click()
        manager.locator('#registrySearch').fill('PW660SW')
        expect(manager.locator('#registryBody')).to_contain_text('Lucia Gallo')
        manager.locator('#addCustomerButton').click()
        for selector, value in {'customerName':'Browser Test', 'customerPhone':'123456789',
                                'customerPlate':'BROWSER1', 'customerPolicy':'E2E', 'customerExpiry':'2030-12-31'}.items():
            manager.locator('#' + selector).fill(value)
        manager.locator('#customerCompany').select_option('1')
        manager.locator('#saveCustomerButton').click()
        expect(manager.locator('#customerModal')).not_to_be_visible()
        self.assertEqual('Browser Test', self.scalar('select nome from customers where targa_norm=%s', 'BROWSER1'))
        mission_id = self.create_ui(manager, 'E2E browser complete')
        driver_id, driver_email = self.available_driver(manager)
        self.propose_ui(manager, mission_id, driver_id)
        driver = self.login(driver_email)
        customer_name, customer_phone, customer_plate = self.db.execute(
            'select nome, telefono, targa from customers where id=1').fetchone()
        pending_detail = self.api(driver, f'/api/v1/autista/missioni/{mission_id}')
        self.assertEqual(200, pending_detail['status'])
        self.assertIsNone(pending_detail['body']['cliente']['nome'])
        self.assertIsNone(pending_detail['body']['cliente']['telefono'])
        self.assertIsNone(pending_detail['body']['cliente']['targa'])
        expect(driver.locator('#driverMissions')).to_contain_text("visibili dopo l'accettazione")
        expect(driver.locator('#driverMissions')).not_to_contain_text(customer_name)
        expect(driver.locator('#driverMissions')).not_to_contain_text(customer_phone)
        with driver.expect_response(
                lambda r: r.url.endswith(f'/api/v1/missioni/{mission_id}/accept')) as response:
            driver.locator(f'[data-mission-id="{mission_id}"][data-driver-action="accept"]').click()
        self.assertEqual(200, response.value.status)
        expect(driver.locator('#activeMissionDetail')).to_be_visible()
        expect(driver.locator('#activeMissionDetail')).to_contain_text(customer_name)
        expect(driver.locator('#activeMissionDetail')).to_contain_text(customer_phone)
        accepted_detail = self.api(driver, f'/api/v1/autista/missioni/{mission_id}')
        self.assertEqual(200, accepted_detail['status'])
        self.assertEqual(customer_name, accepted_detail['body']['cliente']['nome'])
        self.assertEqual(customer_phone, accepted_detail['body']['cliente']['telefono'])
        self.assertEqual(customer_plate, accepted_detail['body']['cliente']['targa'])
        other_driver = self.login(self.other_driver_email(driver_id))
        self.assertEqual(
            403, self.api(other_driver, f'/api/v1/autista/missioni/{mission_id}')['status'])
        self.assertEqual(403, self.api(other_driver, f'/api/v1/missioni/{mission_id}')['status'])
        other_missions = self.api(other_driver, '/api/v1/autista/missioni')['body']
        self.assertNotIn(mission_id, [mission['id'] for mission in other_missions])
        self.assertEqual('accettata', self.scalar('select stato from missions where id=%s', mission_id))
        driver.locator('#missionNoteText').fill('Arrivato - browser')
        driver.locator('#missionNoteForm button').click()
        expect(driver.locator('#missionNotes')).to_contain_text('Arrivato - browser')
        self.assertEqual(1, self.scalar('select count(*) from mission_notes where mission_id=%s', mission_id))
        driver.locator('#actualKm').fill('18.25')
        driver.locator('#completeMissionForm button').click()
        expect(driver.locator('#driverAlert')).to_contain_text('chiusa correttamente')
        row = self.db.execute('select stato, km_effettivi, costo_finale, notturno from missions where id=%s', (mission_id,)).fetchone()
        self.assertEqual(('eseguita', Decimal('18.25')), row[:2])
        self.assertEqual((Decimal('71.90') * (Decimal('1.25') if row[3] else 1)).quantize(Decimal('.01')), row[2])
        self.assertEqual(2, self.scalar('select count(*) from notifications_outbox where mission_id=%s', mission_id))
        driver.locator('button[data-view-link="storico"]').click()
        expect(driver.locator('#driverHistory')).to_contain_text('18.25')
        manager.locator('button[data-view-link="missioni"]').first.click()
        expect(manager.locator('#missionDoneCount')).to_have_text('1')
        self.assertEqual(fixtures, self.scalar('select count(*) from missions where is_fixture'))
        output = Path(os.environ.get('E2E_OUTPUT', 'target/e2e'))
        output.mkdir(parents=True, exist_ok=True)
        manager.screenshot(path=str(output / 'manager-completed.png'), full_page=True)
        driver.screenshot(path=str(output / 'driver-history.png'), full_page=True)
        driver.locator('#logoutButton').click()
        expect(driver.locator('#publicView')).to_be_visible()
        self.assertEqual(401, self.api(driver, '/api/v1/auth/me', csrf=False)['status'])

    def test_b_rejection_expiry_reproposal_and_security(self):
        manager = self.login('responsabile@demo.it')
        mission_id = self.create_ui(manager, 'E2E reject expire')
        driver_id, driver_email = self.available_driver(manager)
        self.propose_ui(manager, mission_id, driver_id)
        driver = self.login(driver_email)
        other = self.login(self.other_driver_email(driver_id))
        self.assertEqual(403, self.api(other, f'/api/v1/missioni/{mission_id}')['status'])
        self.assertEqual(403, self.api(driver, '/api/v1/clienti')['status'])
        self.assertEqual(403, self.api(driver, f'/api/v1/missioni/{mission_id}/accept', 'POST', csrf=False)['status'])
        driver.locator(f'[data-mission-id="{mission_id}"][data-driver-action="reject"]').click()
        expect(driver.locator('#driverMissions')).to_contain_text('Nessuna proposta')
        self.assertEqual('non_accettata', self.scalar('select stato from missions where id=%s', mission_id))
        manager.locator('button[data-view-link="missioni"]').first.click()
        retry_driver_id, _ = self.available_driver(manager)
        self.propose_ui(manager, mission_id, retry_driver_id)
        # Isolated test only: emulate elapsed time, never wait ten minutes.
        self.db.execute("update mission_proposals set proposta_il=now()-interval '11 minutes', scadenza_il=now()-interval '1 minute' where mission_id=%s and stato='proposta'", (mission_id,))
        self.db.execute("update missions set proposta_il=now()-interval '11 minutes', scadenza_il=now()-interval '1 minute' where id=%s", (mission_id,))
        result = self.api(driver, f'/api/v1/missioni/{mission_id}/accept', 'POST')
        self.assertEqual(409, result['status'])
        self.assertEqual('scaduta', self.scalar('select stato from missions where id=%s', mission_id))
        self.assertEqual(2, self.scalar('select count(*) from mission_proposals where mission_id=%s', mission_id))
        self.assertEqual(400, self.api(manager, '/api/v1/missioni/not-a-number')['status'])
        self.assertEqual(404, self.api(manager, '/api/v1/missioni/999999999')['status'])
        # Lose the session cookie while the UI still shows an authenticated user.
        driver.context.clear_cookies()
        driver.locator('#driverRefreshButton').click()
        expect(driver.locator('#loginView')).to_be_visible()
        expect(driver.locator('#loginAlert')).to_contain_text('sessione')


if __name__ == '__main__':
    if os.environ.get('E2E_REVERSE_TEST_ORDER') == '1':
        unittest.defaultTestLoader.sortTestMethodsUsing = lambda first, second: (
            (first < second) - (first > second))
    unittest.main(verbosity=2)
