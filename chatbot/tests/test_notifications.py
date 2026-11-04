import unittest

from fornitori_chatbot.backend import BackendUnavailable, LookupExpired, LookupResult, MissionCreated, Notification
from fornitori_chatbot.engine import ChatEngine, InboundMessage
from fornitori_chatbot.fake_backend import FakeBackend
from fornitori_chatbot.http_backend import _map_status
from fornitori_chatbot.notifications import NotificationDispatcher


class Queue:
    """Backend minimo: consegna le notifiche ricevute e registra gli esiti."""

    def __init__(self, *batches):
        self.batches = list(batches)
        self.reports = []
        self.report_error = None

    def claim_notifications(self, channel):
        return self.batches.pop(0) if self.batches else []

    def report_notification(self, notification_id, attempt, sent, error=None):
        if self.report_error:
            raise self.report_error
        self.reports.append((notification_id, attempt, sent))


def accepted(attempt=0):
    return Notification(7, "MISSION_ACCEPTED", "chat-1", "M-1", "Autista Demo", attempt)


class DispatcherTests(unittest.TestCase):
    def test_outcome_carries_the_attempt_of_the_reservation(self):
        queue = Queue([accepted(attempt=2)])
        sent = []
        NotificationDispatcher(queue, "telegram", lambda chat, text: sent.append((chat, text))).dispatch_once()
        self.assertEqual(sent[0][0], "chat-1")
        self.assertIn("Autista Demo", sent[0][1])
        self.assertEqual(queue.reports, [(7, 2, True)])

    def test_failed_send_is_reported_as_failed(self):
        def broken(chat, text):
            raise RuntimeError("HTTP 500")

        queue = Queue([accepted(attempt=1)])
        with self.assertLogs("fornitori_chatbot.notifications", "WARNING"):
            NotificationDispatcher(queue, "telegram", broken).dispatch_once()
        self.assertEqual(queue.reports, [(7, 1, False)])

    def test_lost_report_is_repeated_with_the_new_attempt_without_sending_again(self):
        queue = Queue([accepted(attempt=0)], [accepted(attempt=1)])
        sent = []
        dispatcher = NotificationDispatcher(queue, "telegram", lambda chat, text: sent.append(text))
        queue.report_error = BackendUnavailable()
        with self.assertRaises(BackendUnavailable):
            dispatcher.dispatch_once()
        queue.report_error = None
        dispatcher.dispatch_once()
        self.assertEqual(len(sent), 1)
        self.assertEqual(queue.reports, [(7, 1, True)])


class ExpiredLookupTests(unittest.TestCase):
    def dialog(self, backend):
        engine = ChatEngine(backend)
        replies = []
        for text in ("AB123CD", "Via Roma 1, Molfetta", "1", "Non parte"):
            replies = engine.handle(InboundMessage("telegram", "chat-1", text=text))
        return replies

    def test_expired_lookup_is_renewed_once_and_the_request_continues(self):
        backend = FakeBackend()
        real = backend.create_mission
        calls = []

        def create(request):
            calls.append(request.request_id)
            if len(calls) == 1:
                raise LookupExpired()
            return real(request)

        backend.create_mission = create
        self.assertIn("M-DEMO-00001", self.dialog(backend)[0])
        self.assertEqual(len(calls), 2)
        self.assertEqual(calls[0], calls[1])

    def test_plate_no_longer_valid_stops_the_request(self):
        backend = FakeBackend()

        def create(request):
            raise LookupExpired()

        real_lookup = backend.lookup_targa
        answers = [real_lookup, lambda session, plate: LookupResult(verificato=False)]
        backend.create_mission = create
        backend.lookup_targa = lambda session, plate: answers.pop(0)(session, plate)
        self.assertNotIn("presa in carico", self.dialog(backend)[0])

    def test_http_status_mapping(self):
        self.assertIsInstance(_map_status(403), LookupExpired)
        self.assertNotIsInstance(_map_status(401), LookupExpired)


if __name__ == "__main__":
    unittest.main()
