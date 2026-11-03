import unittest

from fornitori_chatbot import messages as msg
from fornitori_chatbot.backend import BackendUnavailable, Conflict, RateLimited
from fornitori_chatbot.engine import (
    DIALOG_TTL_SECONDS,
    MISSION_TTL_SECONDS,
    VERIFICATION_LOCK_SECONDS,
    ChatEngine,
    InboundMessage,
    State,
    normalize_plate,
)
from fornitori_chatbot.fake_backend import FakeBackend


class FakeClock:
    def __init__(self):
        self.now = 1000.0

    def __call__(self):
        return self.now


class DialogTestCase(unittest.TestCase):
    def setUp(self):
        self.backend = FakeBackend()
        self.clock = FakeClock()
        self.engine = ChatEngine(self.backend, clock=self.clock)

    def say(self, text="", chat="chat-1", **kwargs):
        return self.engine.handle(InboundMessage("telegram", chat, text=text, **kwargs))

    def session(self, chat="chat-1"):
        return self.engine._sessions[("telegram", chat)]

    def reach_confirm(self, chat="chat-1"):
        self.say("ciao", chat=chat)
        self.say("1", chat=chat)
        self.say("ab 123-cd", chat=chat)
        self.say("POL-TEST-001", chat=chat)
        self.say("Via Roma 1, Molfetta", chat=chat)
        return self.say("Motore in panne", chat=chat)


class HappyPathTests(DialogTestCase):
    def test_full_request_creates_mission(self):
        self.assertEqual(self.say("ciao"), [msg.WELCOME])
        self.assertEqual(self.say("1"), [msg.ASK_PLATE])
        self.assertEqual(self.say("ab 123-cd"), [msg.ASK_CHALLENGE])
        replies = self.say("POL-TEST-001")
        self.assertIn("M*** R***", replies[0])
        self.assertEqual(replies[1], msg.ASK_LOCATION)
        self.assertEqual(self.say("Via Roma 1, Molfetta"), [msg.ASK_PROBLEM])
        summary = self.say("Motore in panne")[0]
        self.assertIn("Via Roma 1, Molfetta", summary)
        self.assertIn("Motore in panne", summary)
        created = self.say("si")[0]
        self.assertIn("M-TEST-00001", created)
        self.assertEqual(self.session().state, State.SHOW_STATUS)

    def test_status_shows_driver_only_when_accepted(self):
        self.reach_confirm()
        self.say("si")
        self.assertIn("in attesa", self.say("stato")[0])
        self.backend.set_status(1, "proposta", "Autista Prova")
        self.assertNotIn("Autista Prova", self.say("stato")[0])
        self.backend.set_status(1, "accettata", "Autista Prova")
        self.assertIn("Autista Prova", self.say("a che punto è il mio soccorso?")[0])

    def test_shared_position_is_saved_with_coordinates(self):
        self.say("ciao")
        self.say("1")
        self.say("AB123CD")
        self.say("POL-TEST-001")
        self.assertEqual(self.say(latitude=41.2, longitude=16.6), [msg.ASK_PROBLEM])
        session = self.session()
        self.assertEqual((session.latitude, session.longitude), (41.2, 16.6))

    def test_collected_data_is_erased_after_creation(self):
        self.reach_confirm()
        self.say("si")
        session = self.session()
        self.assertIsNone(session.plate)
        self.assertIsNone(session.location)
        self.assertIsNone(session.problem)
        self.assertIsNone(session.customer_id)

    def test_plate_normalization(self):
        self.assertEqual(normalize_plate(" ab 123-cd "), "AB123CD")


class ErrorPathTests(DialogTestCase):
    def test_unknown_plate_and_wrong_challenge_give_same_answer(self):
        self.say("1")
        self.say("ZZ999ZZ")
        unknown = self.say("POL-TEST-001")
        self.say("AB123CD")
        wrong = self.say("POLIZZA-SBAGLIATA")
        self.assertEqual(unknown, wrong)
        self.assertEqual(wrong, [msg.VERIFICATION_FAILED, msg.ASK_PLATE])

    def test_repeated_verification_failures_lock_until_timeout(self):
        self.say("1")
        for _ in range(2):
            self.say("AB123CD")
            self.say("SBAGLIATA")
        self.say("AB123CD")
        self.assertEqual(self.say("SBAGLIATA"), [msg.LOCKED])
        self.assertEqual(self.say("1"), [msg.LOCKED])
        self.clock.now += VERIFICATION_LOCK_SECONDS + 1
        self.assertEqual(self.say("1"), [msg.ASK_PLATE])

    def test_cancel_does_not_reset_verification_lock(self):
        self.say("1")
        for _ in range(3):
            self.say("AB123CD")
            self.say("SBAGLIATA")
        self.say("annulla")
        self.assertEqual(self.say("1"), [msg.LOCKED])

    def test_invalid_inputs_end_after_three_attempts(self):
        self.say("1")
        self.assertEqual(self.say("!!"), [msg.INVALID_PLATE])
        self.assertEqual(self.say("??!"), [msg.INVALID_PLATE])
        self.assertEqual(self.say("#"), [msg.TOO_MANY_ATTEMPTS])
        self.assertEqual(self.session().state, State.END)

    def test_cancel_works_in_every_active_state(self):
        for steps in ([], ["AB123CD"], ["AB123CD", "POL-TEST-001"]):
            self.say("1")
            for step in steps:
                self.say(step)
            self.assertEqual(self.say("ANNULLA"), [msg.CANCELLED])
            self.assertEqual(self.session().state, State.START)
            self.assertIsNone(self.session().plate)

    def test_help_keeps_the_current_state(self):
        self.say("1")
        self.assertEqual(self.say("aiuto"), [msg.HELP])
        self.assertEqual(self.session().state, State.WAIT_PLATE)

    def test_edit_returns_to_location(self):
        self.reach_confirm()
        self.assertEqual(self.say("modifica"), [msg.ASK_LOCATION])
        self.assertEqual(self.backend.create_calls, 0)

    def test_invalid_location_is_rejected(self):
        self.say("1")
        self.say("AB123CD")
        self.say("POL-TEST-001")
        self.assertEqual(self.say("123"), [msg.INVALID_LOCATION])
        self.assertEqual(self.say(latitude=200.0, longitude=10.0), [msg.INVALID_LOCATION])

    def test_status_without_mission_does_not_ask_for_data(self):
        self.assertEqual(self.say("2"), [msg.NO_MISSION])


class IdempotencyTests(DialogTestCase):
    def test_duplicate_callback_is_ignored(self):
        self.say("1", message_id="m1")
        self.assertEqual(self.say("1", message_id="m1"), [])
        self.assertEqual(self.session().state, State.WAIT_PLATE)

    def test_second_confirmation_does_not_create_second_mission(self):
        self.reach_confirm()
        self.say("si")
        self.say("si")
        self.assertEqual(self.backend.create_calls, 1)

    def test_retry_after_outage_reuses_request_id(self):
        self.reach_confirm()
        requests = []
        real_create = self.backend.create_mission

        def flaky(request):
            requests.append(request)
            if len(requests) == 1:
                raise BackendUnavailable()
            return real_create(request)

        self.backend.create_mission = flaky
        self.assertEqual(self.say("si"), [msg.UNAVAILABLE])
        self.assertEqual(self.session().state, State.CONFIRM)
        self.assertIn("M-TEST-00001", self.say("si")[0])
        self.assertEqual(requests[0].request_id, requests[1].request_id)


class BackendFailureTests(DialogTestCase):
    def test_lookup_outage_does_not_count_as_failed_verification(self):
        self.say("1")
        self.say("AB123CD")
        real_lookup = self.backend.lookup_targa

        def down(*args):
            raise BackendUnavailable()

        self.backend.lookup_targa = down
        self.assertEqual(self.say("POL-TEST-001"), [msg.UNAVAILABLE])
        self.assertEqual(self.session().verification_failures, 0)
        self.backend.lookup_targa = real_lookup
        self.assertEqual(self.say("POL-TEST-001")[1], msg.ASK_LOCATION)

    def test_rate_limit_message(self):
        self.say("1")
        self.say("AB123CD")

        def limited(*args):
            raise RateLimited()

        self.backend.lookup_targa = limited
        self.assertEqual(self.say("POL-TEST-001"), [msg.RATE_LIMITED])

    def test_create_conflict_ends_and_erases_data(self):
        self.reach_confirm()

        def conflict(request):
            raise Conflict()

        self.backend.create_mission = conflict
        self.assertEqual(self.say("si"), [msg.CREATE_CONFLICT])
        self.assertEqual(self.session().state, State.END)
        self.assertIsNone(self.session().problem)

    def test_expired_token_asks_verification_then_shows_status(self):
        self.reach_confirm()
        self.say("si")
        self.backend.revoke_tokens()
        self.assertEqual(self.say("stato"), [msg.REVERIFY, msg.ASK_PLATE])
        self.say("AB123CD")
        replies = self.say("POL-TEST-001")
        self.assertIn("in attesa", replies[1])


class TimeoutAndIsolationTests(DialogTestCase):
    def test_inactive_dialog_expires(self):
        self.say("1")
        self.say("AB123CD")
        self.clock.now += DIALOG_TTL_SECONDS
        self.assertEqual(self.say("POL-TEST-001"), [msg.EXPIRED, msg.WELCOME])

    def test_session_with_mission_survives_dialog_ttl_then_expires(self):
        self.reach_confirm()
        self.say("si")
        self.clock.now += DIALOG_TTL_SECONDS + 1
        self.assertIn("in attesa", self.say("stato")[0])
        self.clock.now += MISSION_TTL_SECONDS
        self.assertEqual(self.say("stato"), [msg.NO_MISSION])

    def test_interrupted_dialog_on_session_with_mission_erases_data(self):
        self.reach_confirm()
        self.say("si")
        self.say("1")
        self.say("AB123CD")
        self.clock.now += DIALOG_TTL_SECONDS
        self.assertEqual(self.say("ciao"), [msg.EXPIRED, msg.WELCOME])
        self.assertIsNone(self.session().plate)

    def test_chats_do_not_share_missions(self):
        self.reach_confirm(chat="chat-a")
        self.say("si", chat="chat-a")
        self.assertEqual(self.say("2", chat="chat-b"), [msg.NO_MISSION])

    def test_same_chat_id_on_other_channel_is_another_session(self):
        self.reach_confirm()
        self.say("si")
        replies = self.engine.handle(InboundMessage("whatsapp", "chat-1", text="2"))
        self.assertEqual(replies, [msg.NO_MISSION])

    def test_other_customer_token_cannot_read_mission(self):
        self.reach_confirm()
        self.say("si")
        self.session().verification_token = None
        self.say("stato")
        self.say("EF456GH")
        replies = self.say("POL-TEST-002")
        self.assertIn(msg.NO_MISSION, replies)

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
