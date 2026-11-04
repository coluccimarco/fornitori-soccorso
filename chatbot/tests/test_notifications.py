import unittest

from fornitori_chatbot import messages as msg
from fornitori_chatbot.backend import BackendUnavailable, MissionRequest
from fornitori_chatbot.engine import ChatEngine, InboundMessage
from fornitori_chatbot.fake_backend import FakeBackend
from fornitori_chatbot.notifications import RETRY_DELAY_SECONDS, NotificationDispatcher


class NotificationTests(unittest.TestCase):
    def setUp(self):
        self.backend = FakeBackend()
        self.sent = []
        self.now = 1000.0
        self.send_error = None
        self.dispatcher = NotificationDispatcher(self.backend, "telegram", self.send, clock=lambda: self.now)

        self.mission = self.backend.create_mission(
            MissionRequest(
                session_id="s" * 32,
                request_id="r" * 32,
                cliente_id=1,
                tipo_intervento="riparazione_sul_posto",
                descrizione_problema="Motore in panne",
                origine_indirizzo="Via Roma 1",
                canale_origine="telegram",
                destinatario="chat-1",
            )
        )

    def send(self, chat_id, text):
        if self.send_error:
            raise self.send_error
        self.sent.append((chat_id, text))

    def test_acceptance_sends_driver_name_to_the_chat(self):
        self.backend.set_status(self.mission.id, "accettata", "Autista Prova")
        self.dispatcher.dispatch_once()
        self.assertEqual(
            self.sent,
            [("chat-1", msg.NOTIFY_ACCEPTED.format(codice=self.mission.codice, autista="Autista Prova"))],
        )
        self.assertEqual(self.backend.notification_reports, [(1, True, None)])

    def test_completion_is_sent_once(self):
        self.backend.set_status(self.mission.id, "eseguita", "Autista Prova")
        self.dispatcher.dispatch_once()
        self.dispatcher.dispatch_once()
        self.assertEqual(self.sent, [("chat-1", msg.NOTIFY_COMPLETED.format(codice=self.mission.codice))])

    def test_failed_send_is_reported_and_retried_after_delay(self):
        self.backend.set_status(self.mission.id, "accettata", "Autista Prova")
        self.send_error = RuntimeError("sendMessage: HTTP 500")
        with self.assertLogs("fornitori_chatbot.notifications", "WARNING"):
            self.dispatcher.dispatch_once()
        self.assertEqual(self.backend.notification_reports, [(1, False, "sendMessage: HTTP 500")])

        self.send_error = None
        self.dispatcher.dispatch_once()
        self.assertEqual(self.sent, [])

        self.now += RETRY_DELAY_SECONDS
        self.dispatcher.dispatch_once()
        self.assertEqual(len(self.sent), 1)

    def test_lost_report_does_not_send_twice(self):
        self.backend.set_status(self.mission.id, "accettata", "Autista Prova")
        real_report = self.backend.report_notification

        def down(*args):
            raise BackendUnavailable()

        self.backend.report_notification = down
        with self.assertRaises(BackendUnavailable):
            self.dispatcher.dispatch_once()
        self.backend.report_notification = real_report
        self.dispatcher.dispatch_once()
        self.assertEqual(len(self.sent), 1)
        self.assertEqual(self.backend.pending_notifications("telegram"), [])

    def test_dialog_passes_chat_as_recipient(self):
        engine = ChatEngine(self.backend)
        for text in ("EF456GH", "Via Roma 1, Molfetta", "1", "Motore in panne"):
            engine.handle(InboundMessage("telegram", "chat-42", text=text))
        self.backend.set_status(2, "accettata", "Autista Prova")
        self.dispatcher.dispatch_once()
        self.assertEqual(self.sent[0][0], "chat-42")


if __name__ == "__main__":
    unittest.main()
