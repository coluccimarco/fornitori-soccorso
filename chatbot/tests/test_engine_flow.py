import unittest
from unittest.mock import patch

from fornitori_chatbot import messages as msg
from fornitori_chatbot.backend import (
    AddressNotFound, BackendUnavailable, LookupExpired, LookupResult, MissionStatus, Notification,
)
from fornitori_chatbot.engine import ChatEngine, DIALOG_TTL_SECONDS, InboundMessage, PROBLEM_TYPES, State
from fornitori_chatbot.fake_backend import FakeBackend
from fornitori_chatbot.notifications import NotificationDispatcher, render
from fornitori_chatbot.telegram_channel import TelegramApi, TelegramBot


class FlowTests(unittest.TestCase):
    def setUp(self):
        self.backend = FakeBackend()
        self.requests = []
        create = self.backend.create_mission

        def capture(request):
            self.requests.append(request)
            return create(request)

        self.backend.create_mission = capture
        self.now = 0
        self.engine = ChatEngine(self.backend, clock=lambda: self.now)

    def say(self, text="", **kwargs):
        return self.engine.handle(InboundMessage("telegram", "chat", text=text, **kwargs))

    def prepare(self, plate="AB123CD"):
        self.assertEqual(self.say(plate), [msg.ASK_PHONE])
        replies = self.say("+39 333 1234567")
        question = self.say("Via Roma 1, Molfetta")[0]
        self.assertEqual(str(question), "Qual è il problema?")
        self.assertEqual(question.buttons, tuple((label, code) for code, label in PROBLEM_TYPES))
        self.assertNotIn("Che intervento ti serve?", " ".join(replies + [question]))
        return replies

    def test_verified_flow_exact_text_and_request(self):
        self.prepare("ab 123 cd")
        self.assertEqual(self.say("3"), [
            "Non appena un autista sarà disponibile riceverai un messaggio.",
            "Codice richiesta M-DEMO-00001. Scrivi STATO per aggiornamenti.",
        ])
        request = self.requests[0]
        self.assertEqual((request.targa, request.telefono_sul_posto, request.tipo_problema),
                         ("AB123CD", "+393331234567", "BATTERIA_SCARICA"))
        self.assertEqual(request.cliente_id, 1)

    def test_unverified_flow(self):
        self.assertEqual(self.prepare("ZZ999ZZ"), [msg.POLICY_PENDING])
        self.assertEqual(self.say("5"), [
            "Attenda... La centrale sta verificando la polizza e la ricontatterà al numero indicato. Codice richiesta M-DEMO-00001."
        ])
        self.assertIsNone(self.requests[0].cliente_id)
        self.assertIn("stiamo verificando la copertura assicurativa", self.say("STATO")[0])

    def test_phone_attempts_and_recovery(self):
        self.say("AB123CD")
        for invalid in ("abc", "12345"):
            self.assertEqual(self.say(invalid), [msg.INVALID_PHONE])
        self.assertIn("Targa riconosciuta", self.say("333 1234567")[0])

    def test_phone_three_invalid_attempts_reset(self):
        self.say("AB123CD")
        for _ in range(2):
            self.say("abc")
        self.assertEqual(self.say("abc"), [msg.TOO_MANY_ATTEMPTS])
        self.assertEqual(self.say("AB123CD"), [msg.ASK_PHONE])

    def test_all_problem_buttons_and_numbers(self):
        for number, (code, _) in enumerate(PROBLEM_TYPES, 1):
            for choice in (str(number), code):
                with self.subTest(choice=choice):
                    self.say("ANNULLA")
                    self.prepare()
                    self.say(choice)
                    self.assertEqual(self.requests[-1].tipo_problema, code)

    def test_free_text_problem_is_rejected(self):
        self.prepare()
        self.assertEqual(str(self.say("Non parte")[0]), msg.ASK_PROBLEM_TYPE)
        self.assertEqual(self.requests, [])

    def test_cancel_and_status_in_every_state(self):
        for state in State:
            with self.subTest(state=state):
                self.say("ciao")
                session = self.engine._sessions[("telegram", "chat")]
                session.state = state
                self.assertEqual(self.say("STATO"), [msg.NO_OPEN_REQUESTS])
                self.assertEqual(session.state, state)
                self.assertEqual(self.say("ANNULLA"), [msg.CANCELLED])
                self.assertEqual(session.state, State.START)
                self.assertIsNone(session.phone)
                self.assertIsNone(session.plate)

    def test_expiry_in_every_active_state(self):
        for steps in (("AB123CD",), ("AB123CD", "3331234567"),
                      ("AB123CD", "3331234567", "Via Roma 1")):
            for step in steps:
                self.say(step)
            self.now += DIALOG_TTL_SECONDS
            self.assertEqual(self.say("ciao"), [msg.EXPIRED, msg.WELCOME])

    def test_status_privacy_and_new_states(self):
        for state in ("accettata", "verifica_polizza", "gestione_manuale", "annullata"):
            with patch.object(self.backend, "current_status", return_value=MissionStatus("M-1", state, "Mario Segreto")):
                reply = self.say("STATO")[0]
                self.assertIn("M-1", reply)
                self.assertNotIn("Mario Segreto", reply)

    def test_address_error_returns_to_location(self):
        self.prepare()
        with patch.object(self.backend, "create_mission", side_effect=AddressNotFound):
            self.assertEqual(self.say("1"), [msg.ADDRESS_NOT_FOUND])
        self.assertEqual(str(self.say("Via Milano 2")[0]), msg.ASK_PROBLEM_TYPE)
        self.say("2")
        self.assertEqual(self.requests[-1].origine_indirizzo, "Via Milano 2")

    def test_network_retry_keeps_request_id(self):
        self.prepare()
        original = self.backend.create_mission
        calls = []

        def retry(request):
            calls.append(request.request_id)
            if len(calls) == 1:
                raise BackendUnavailable()
            return original(request)

        self.backend.create_mission = retry
        self.assertEqual(self.say("1"), [msg.UNAVAILABLE])
        self.say("1")
        self.assertEqual(calls[0], calls[1])

    def test_unverified_lookup_can_be_renewed(self):
        self.prepare("ZZ999ZZ")
        original = self.backend.create_mission
        calls = []

        def create(request):
            calls.append(request)
            if len(calls) == 1:
                raise LookupExpired()
            return original(request)

        self.backend.create_mission = create
        self.assertIn("La centrale sta verificando", self.say("1")[0])
        self.assertEqual(len(calls), 2)
        self.assertEqual(calls[0].request_id, calls[1].request_id)
        self.assertIsNone(calls[1].cliente_id)

    def test_renewal_can_move_verified_plate_to_policy_review(self):
        self.prepare()
        original = self.backend.create_mission
        calls = []

        def create(request):
            calls.append(request)
            if len(calls) == 1:
                raise LookupExpired()
            return original(request)

        self.backend.create_mission = create
        with patch.object(self.backend, "lookup_targa", return_value=LookupResult(False)):
            self.assertIn("La centrale sta verificando", self.say("1")[0])
        self.assertEqual(calls[0].cliente_id, 1)
        self.assertIsNone(calls[1].cliente_id)

    def test_shared_location_and_duplicate_callback(self):
        self.say("AB123CD")
        self.say("3331234567")
        self.say(latitude=41.2, longitude=16.6)
        self.say("INCIDENTE", message_id="cb-1")
        self.assertEqual(self.say("INCIDENTE", message_id="cb-1"), [])
        self.assertEqual(len(self.requests), 1)
        self.assertEqual((self.requests[0].origine_lat, self.requests[0].origine_lng), (41.2, 16.6))


class NotificationTextTests(unittest.TestCase):
    def test_acceptance_exact_and_private(self):
        self.assertEqual(render(Notification(1, "MISSION_ACCEPTED", "chat", "M-1", "Mario")),
                         "La tua richiesta M-1 è stata accettata. Entro 45 minuti arriverà l'autista.")

    def test_cancellation(self):
        self.assertEqual(render(Notification(1, "MISSION_CANCELLED", "chat", "M-1")),
                         "La richiesta M-1 è stata annullata. Per assistenza contatti la centrale operativa.")

    def test_unknown_is_ignored_without_retry(self):
        class Queue:
            def claim_notifications(self, channel):
                return [Notification(1, "FUTURE_TYPE", "chat", "M-1")]

            def report_notification(self, *args):
                self.report = args

        queue = Queue()
        sent = []
        NotificationDispatcher(queue, "telegram", lambda *args: sent.append(args)).dispatch_once()
        self.assertEqual(sent, [])
        self.assertEqual(queue.report, (1, 0, True))


class TelegramFlowTests(unittest.TestCase):
    def test_inline_callback_creates_selected_problem(self):
        backend = FakeBackend()
        engine = ChatEngine(backend)
        api = TelegramApi("test-token")
        bot = TelegramBot(api, engine)
        for text in ("AB123CD", "3331234567", "Via Roma 1"):
            engine.handle(InboundMessage("telegram", "1", text=text))
        with patch.object(api, "answer_callback"), patch.object(api, "send_message") as send:
            callback = bot._from_callback({"id": "callback-1", "data": "INCIDENTE",
                                          "message": {"chat": {"type": "private", "id": 1}}})
            bot._process(callback)
            self.assertEqual(send.call_args_list[0].args[1], msg.CREATED)
        self.assertEqual(backend.current_status("telegram", "1").stato, "nuova")
