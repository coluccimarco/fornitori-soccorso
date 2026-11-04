import unittest

from fornitori_chatbot import messages as msg
from fornitori_chatbot.backend import AddressNotFound, LookupExpired, LookupResult, BackendUnavailable, Conflict, RateLimited
from fornitori_chatbot.engine import (
    DIALOG_TTL_SECONDS,
    INTERVENTION_QUESTION,
    ChatEngine,
    InboundMessage,
    State,
    looks_like_plate,
    normalize_plate,
)
from fornitori_chatbot.fake_backend import FakeBackend, FakeCustomer


class DialogTestCase(unittest.TestCase):
    def setUp(self):
        self.backend = FakeBackend(
            customers=(
                FakeCustomer(1, "Lucia Gallo", "Polizza Sicura Assicurazioni", "PW660SW"),
                FakeCustomer(5, "Maddalena Lombardi", "Assicurazione Demo S.p.A.", "EB821TZ"),
            )
        )
        self.now = 1000.0
        self.engine = ChatEngine(self.backend, clock=lambda: self.now)

    def say(self, text="", chat="chat-1", **kwargs):
        return self.engine.handle(InboundMessage("telegram", chat, text=text, **kwargs))

    def session(self, chat="chat-1"):
        return self.engine._sessions[("telegram", chat)]

    def create_mission(self, chat="chat-1"):
        self.say("PW 660 SW", chat=chat)
        self.say("Via Roma 1, Molfetta", chat=chat)
        self.say("riparazione", chat=chat)
        return self.say("La macchina non parte", chat=chat)


class TeacherDialogueTests(DialogTestCase):
    """I dialoghi 1-9 del foglio DialoghiChatbot; il 10 (Gemini) e' facoltativo."""

    def test_01_plate_is_recognized(self):
        self.assertEqual(
            self.say("PW 660 SW"),
            ["Targa riconosciuta (Lucia Gallo, Polizza Sicura Assicurazioni). "
             "Condividi la posizione o scrivi l'indirizzo."],
        )
        self.assertEqual(self.session().state, State.WAIT_LOCATION)

    def test_02_shared_position_is_saved_then_asks_intervention(self):
        self.say("PW 660 SW")
        self.assertEqual(self.say(latitude=41.2010, longitude=16.6000), [INTERVENTION_QUESTION])
        self.assertEqual(self.say("riparazione"), ["Descrivi in poche parole il problema."])
        self.assertEqual((self.session().latitude, self.session().longitude), (41.2010, 16.6000))

    def test_03_problem_creates_mission_without_further_confirmation(self):
        self.say("PW 660 SW")
        self.say(latitude=41.2010, longitude=16.6000)
        self.say("riparazione")
        self.assertEqual(
            self.say("La macchina non parte, credo sia la batteria"),
            ["Richiesta M-TEST-00001 presa in carico. Ti avviseremo appena un autista accetta. "
             "Scrivi STATO per aggiornamenti."],
        )
        request = self.backend.requests[0]
        self.assertEqual(request.cliente_id, 1)
        self.assertEqual(request.tipo_intervento, "riparazione_sul_posto")
        self.assertEqual(request.origine_lat, 41.2010)
        self.assertEqual(request.descrizione_problema, "La macchina non parte, credo sia la batteria")

    def test_04_textual_address(self):
        self.assertIn("Condividi la posizione", self.say("EB 821 TZ")[0])
        self.assertEqual(self.say("Via Roma 10, Molfetta"), [INTERVENTION_QUESTION])
        self.say("2")
        self.say("Foratura")
        self.assertEqual(self.backend.requests[0].origine_indirizzo, "Via Roma 10, Molfetta")
        self.assertIsNone(self.backend.requests[0].origine_lat)

    def test_05_unknown_plate(self):
        self.assertEqual(
            self.say("ZZ 000 ZZ"),
            ["Non trovo la targa tra i nostri assicurati. Controlla e riprova oppure chiama la centrale."],
        )
        self.assertEqual(self.backend.requests, [])
        self.assertEqual(self.session().state, State.START)

    def test_06_badly_written_plate(self):
        self.assertIn("Targa riconosciuta (Lucia Gallo", self.say("pw660sw")[0])

    def test_07_status_with_open_mission(self):
        self.create_mission()
        self.assertEqual(
            self.say("STATO"),
            ["La tua richiesta M-TEST-00001 è stata ricevuta: stiamo cercando un autista disponibile."],
        )
        self.backend.set_status(1, "accettata", "Autista Prova")
        self.assertEqual(
            self.say("STATO"),
            ["La tua richiesta M-TEST-00001 è stata presa in carico; autista assegnato: Autista Prova."],
        )

    def test_08_status_without_missions_is_case_insensitive(self):
        self.assertEqual(
            self.say("stato"),
            ["Non risultano richieste aperte per questo numero. Scrivi la targa per attivare un soccorso."],
        )

    def test_09_out_of_flow_message_gets_help(self):
        self.assertEqual(
            self.say("Ciao, quanto costa il servizio?"),
            ["Ciao, sono l'assistente FORNITORI. Per attivare un soccorso scrivi la targa del veicolo; "
             "per conoscere lo stato scrivi STATO."],
        )


class InterventionChoiceTests(DialogTestCase):
    def reach_choice(self):
        self.say("PW 660 SW")
        return self.say("Via Roma 1, Molfetta")[0]

    def test_question_offers_both_buttons(self):
        question = self.reach_choice()
        self.assertEqual(
            question.buttons,
            (("Riparazione sul posto", "riparazione"), ("Traino", "traino")),
        )

    def test_towing_button_creates_towing_mission(self):
        self.reach_choice()
        self.assertEqual(self.say("traino"), [msg.ASK_PROBLEM])
        self.say("Motore in panne")
        self.assertEqual(self.backend.requests[0].tipo_intervento, "traino")
        self.assertIsNone(self.backend.requests[0].officina_id)

    def test_typed_numbers_work_without_buttons(self):
        self.reach_choice()
        self.say("1")
        self.say("Motore in panne")
        self.assertEqual(self.backend.requests[0].tipo_intervento, "riparazione_sul_posto")

    def test_other_text_repeats_question_with_buttons(self):
        self.reach_choice()
        reply = self.say("non so")[0]
        self.assertEqual(reply, msg.ASK_INTERVENTION)
        self.assertTrue(reply.buttons)
        self.assertEqual(self.backend.requests, [])

    def test_unknown_address_asks_position_again_keeping_request(self):
        self.reach_choice()
        self.say("traino")
        real_create = self.backend.create_mission
        attempts = []

        def not_found(request):
            attempts.append(request.request_id)
            if len(attempts) == 1:
                raise AddressNotFound()
            return real_create(request)

        self.backend.create_mission = not_found
        self.assertEqual(self.say("Motore in panne"), [msg.ADDRESS_NOT_FOUND])
        self.assertEqual(self.session().state, State.WAIT_LOCATION)
        self.assertEqual(self.say("Corso Umberto 5, Bari"), [INTERVENTION_QUESTION])
        self.say("traino")
        self.assertIn("M-TEST-00001", self.say("Motore in panne")[0])
        self.assertEqual(attempts[0], attempts[1])
        self.assertEqual(self.backend.requests[-1].origine_indirizzo, "Corso Umberto 5, Bari")


class PlateRecognitionTests(unittest.TestCase):
    def test_plate_formats(self):
        for text in ("PW 660 SW", "pw660sw", "PW-660-SW", " pw 660 sw "):
            self.assertTrue(looks_like_plate(text.strip()), text)
        self.assertEqual(normalize_plate(" ab 123-cd "), "AB123CD")

    def test_other_text_is_not_a_plate(self):
        for text in ("ciao", "12345", "Ciao, quanto costa il servizio?", "stato", "", "AB1"):
            self.assertFalse(looks_like_plate(text), text)


class ErrorPathTests(DialogTestCase):
    def _expire_lookup_once(self):
        real = self.backend.create_mission
        calls = []

        def create(request):
            calls.append(request)
            if len(calls) == 1:
                raise LookupExpired()
            return real(request)

        self.backend.create_mission = create
        return calls

    def test_expired_lookup_is_renewed_and_mission_created(self):
        calls = self._expire_lookup_once()
        created = self.create_mission()
        self.assertEqual(len(calls), 2)
        self.assertEqual(calls[0].request_id, calls[1].request_id)
        self.assertIn("M-TEST-00001", created[0])

    def test_expired_lookup_with_plate_no_longer_valid_is_rejected(self):
        self._expire_lookup_once()
        self.say("PW 660 SW")
        self.say("Via Roma 1, Molfetta")
        self.say("riparazione")
        self.backend.lookup_targa = lambda session_id, targa: LookupResult(verificato=False)
        self.assertEqual(self.say("La macchina non parte"), [msg.CREATE_REJECTED])
        self.assertEqual(self.session().state, State.START)

    def test_internal_states_are_not_shown_to_the_customer(self):
        self.create_mission()
        for state in ("non_accettata", "scaduta"):
            self.backend.set_status(1, state)
            reply = self.say("STATO")[0]
            self.assertIn("altro autista", reply)
            self.assertNotIn(state.replace("_", " "), reply)

    def test_status_phrase_from_the_pdf(self):
        self.create_mission()
        self.assertIn("M-TEST-00001", self.say("a che punto è il mio soccorso?")[0])

    def test_status_does_not_interrupt_open_dialog(self):
        self.say("PW 660 SW")
        self.assertEqual(self.say("stato"), [msg.NO_OPEN_REQUESTS])
        self.assertEqual(self.session().state, State.WAIT_LOCATION)

    def test_closed_mission_is_not_open(self):
        self.create_mission()
        self.backend.set_status(1, "eseguita", "Autista Prova")
        self.assertEqual(self.say("stato"), [msg.NO_OPEN_REQUESTS])

    def test_invalid_inputs_restart_after_three_attempts(self):
        self.say("PW 660 SW")
        self.assertEqual(self.say("12"), [msg.INVALID_LOCATION])
        self.assertEqual(self.say(latitude=200.0, longitude=10.0), [msg.INVALID_LOCATION])
        self.assertEqual(self.say("#"), [msg.TOO_MANY_ATTEMPTS])
        self.assertEqual(self.session().state, State.START)
        self.assertIsNone(self.session().customer_id)

    def test_cancel_works_in_every_active_state(self):
        for steps in (["PW 660 SW"], ["PW 660 SW", "Via Roma 1, Molfetta"], ["PW 660 SW", "Via Roma 1, Molfetta", "1"]):
            for step in steps:
                self.say(step)
            self.assertEqual(self.say("ANNULLA"), [msg.CANCELLED])
            self.assertEqual(self.session().state, State.START)
            self.assertIsNone(self.session().location)

    def test_help_keeps_the_current_state(self):
        self.say("PW 660 SW")
        self.assertEqual(self.say("aiuto"), [msg.HELP])
        self.assertEqual(self.session().state, State.WAIT_LOCATION)

    def test_collected_data_is_erased_after_creation(self):
        self.create_mission()
        session = self.session()
        self.assertEqual(session.state, State.START)
        self.assertIsNone(session.customer_id)
        self.assertIsNone(session.location)

    def test_problem_too_short_is_rejected(self):
        self.say("PW 660 SW")
        self.say("Via Roma 1, Molfetta")
        self.say("1")
        self.assertEqual(self.say("x"), [msg.INVALID_PROBLEM])
        self.assertEqual(self.backend.requests, [])

    def test_customer_can_open_a_second_request(self):
        self.create_mission()
        self.assertIn("M-TEST-00002", self.create_mission()[0])


class IdempotencyTests(DialogTestCase):
    def test_duplicate_callback_is_ignored(self):
        self.say("PW 660 SW", message_id="m1")
        self.assertEqual(self.say("PW 660 SW", message_id="m1"), [])
        self.assertEqual(self.session().state, State.WAIT_LOCATION)

    def test_retry_after_outage_reuses_request_id(self):
        self.say("PW 660 SW")
        self.say("Via Roma 1, Molfetta")
        self.say("1")
        real_create = self.backend.create_mission
        attempts = []

        def flaky(request):
            attempts.append(request.request_id)
            if len(attempts) == 1:
                raise BackendUnavailable()
            return real_create(request)

        self.backend.create_mission = flaky
        self.assertEqual(self.say("Motore in panne"), [msg.UNAVAILABLE])
        self.assertEqual(self.session().state, State.WAIT_PROBLEM)
        self.assertIn("M-TEST-00001", self.say("Motore in panne")[0])
        self.assertEqual(attempts[0], attempts[1])


class BackendFailureTests(DialogTestCase):
    def fail(self, name, error):
        def failing(*args):
            raise error

        setattr(self.backend, name, failing)

    def test_lookup_outage(self):
        self.fail("lookup_targa", BackendUnavailable())
        self.assertEqual(self.say("PW 660 SW"), [msg.UNAVAILABLE])
        self.assertEqual(self.session().state, State.START)

    def test_rate_limit_message(self):
        self.fail("lookup_targa", RateLimited())
        self.assertEqual(self.say("PW 660 SW"), [msg.RATE_LIMITED])

    def test_status_outage(self):
        self.fail("current_status", BackendUnavailable())
        self.assertEqual(self.say("stato"), [msg.UNAVAILABLE])

    def test_create_conflict_restarts_and_erases_data(self):
        self.say("PW 660 SW")
        self.say("Via Roma 1, Molfetta")
        self.say("1")
        self.fail("create_mission", Conflict())
        self.assertEqual(self.say("Motore in panne"), [msg.CREATE_CONFLICT])
        self.assertEqual(self.session().state, State.START)
        self.assertIsNone(self.session().location)


class TimeoutAndIsolationTests(DialogTestCase):
    def test_inactive_dialog_expires(self):
        self.say("PW 660 SW")
        self.now += DIALOG_TTL_SECONDS
        self.assertEqual(self.say("Via Roma 1, Molfetta"), [msg.EXPIRED, msg.WELCOME])

    def test_status_survives_session_expiry(self):
        self.create_mission()
        self.now += DIALOG_TTL_SECONDS * 100
        self.assertIn("M-TEST-00001", self.say("stato")[0])

    def test_chats_do_not_share_missions(self):
        self.create_mission(chat="chat-a")
        self.assertEqual(self.say("stato", chat="chat-b"), [msg.NO_OPEN_REQUESTS])

    def test_same_chat_id_on_other_channel_is_another_conversation(self):
        self.create_mission()
        replies = self.engine.handle(InboundMessage("whatsapp", "chat-1", text="stato"))
        self.assertEqual(replies, [msg.NO_OPEN_REQUESTS])

    def test_session_id_does_not_contain_chat_id(self):
        self.say("ciao", chat="+393331234567")
        session = self.session("+393331234567")
        self.assertNotIn("393331234567", session.session_id)
        self.assertGreaterEqual(len(session.session_id), 16)

    def test_unknown_channel_is_refused(self):
        with self.assertRaises(ValueError):
            self.engine.handle(InboundMessage("sms", "chat-1", text="ciao"))


if __name__ == "__main__":
    unittest.main()
