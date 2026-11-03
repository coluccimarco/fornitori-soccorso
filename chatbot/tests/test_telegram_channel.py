import json
import logging
import unittest

from fornitori_chatbot import messages as msg
from fornitori_chatbot.engine import ChatEngine
from fornitori_chatbot.fake_backend import FakeBackend
from fornitori_chatbot.telegram_channel import (
    InvalidToken,
    TelegramApi,
    TelegramBot,
    TelegramError,
)
from tests.test_http_backend import StubServer


class FakeApi:
    def __init__(self):
        self.batches = []
        self.offsets = []
        self.sent = []
        self.send_error = None

    def get_updates(self, offset):
        self.offsets.append(offset)
        return self.batches.pop(0) if self.batches else []

    def send_message(self, chat_id, text):
        if self.send_error:
            raise self.send_error
        self.sent.append((chat_id, text))


def update(update_id, chat_id=10, chat_type="private", **fields):
    message = {"message_id": update_id * 100, "chat": {"id": chat_id, "type": chat_type}}
    message.update(fields)
    return {"update_id": update_id, "message": message}


class TelegramBotTests(unittest.TestCase):
    def setUp(self):
        self.api = FakeApi()
        self.bot = TelegramBot(self.api, ChatEngine(FakeBackend()))

    def deliver(self, *updates):
        self.api.batches.append(list(updates))
        self.bot.poll_once()

    def texts(self):
        return [text for _, text in self.api.sent]

    def test_start_shows_menu_and_offset_advances(self):
        self.deliver(update(5, text="/start"))
        self.assertEqual(self.api.sent, [(10, msg.WELCOME)])
        self.bot.poll_once()
        self.assertEqual(self.api.offsets, [None, 6])

    def test_full_dialog_with_shared_location(self):
        self.deliver(update(1, text="/start"))
        self.deliver(update(2, text="1"))
        self.deliver(update(3, text="AB 123 CD"))
        self.deliver(update(4, text="POL-TEST-001"))
        self.deliver(update(5, location={"latitude": 41.2, "longitude": 16.6}))
        self.deliver(update(6, text="Motore in panne"))
        self.deliver(update(7, text="si"))
        self.assertIn("M-TEST-00001", self.texts()[-1])

    def test_start_restarts_an_open_dialog(self):
        self.deliver(update(1, text="1"))
        self.deliver(update(2, text="/start"))
        self.assertEqual(self.texts()[-1], msg.WELCOME)

    def test_groups_are_ignored(self):
        self.deliver(update(1, chat_type="group", text="ciao"))
        self.assertEqual(self.api.sent, [])

    def test_update_without_message_only_advances(self):
        self.api.batches.append([{"update_id": 9}])
        self.bot.poll_once()
        self.bot.poll_once()
        self.assertEqual(self.api.offsets[-1], 10)
        self.assertEqual(self.api.sent, [])

    def test_unsupported_content_gets_a_reply(self):
        self.deliver(update(1, text="1"))
        self.deliver(update(2, photo=[{"file_id": "x"}]))
        self.assertEqual(self.texts()[-1], msg.INVALID_PLATE)

    def test_chats_are_separate(self):
        self.deliver(update(1, chat_id=10, text="1"))
        self.deliver(update(2, chat_id=20, text="ciao"))
        self.assertEqual(self.api.sent[-1], (20, msg.WELCOME))

    def test_send_failure_does_not_stop_next_update(self):
        self.api.send_error = TelegramError("sendMessage: HTTP 500")
        with self.assertLogs("fornitori_chatbot.telegram_channel", logging.WARNING):
            self.deliver(update(1, text="ciao"))
        self.api.send_error = None
        self.deliver(update(2, text="1"))
        self.assertEqual(self.texts(), [msg.ASK_PLATE])

    def test_dialog_error_replies_unavailable(self):
        def broken(message):
            raise RuntimeError("guasto")

        self.bot._engine.handle = broken
        with self.assertLogs("fornitori_chatbot.telegram_channel", logging.ERROR):
            self.deliver(update(1, text="ciao"))
        self.assertEqual(self.texts(), [msg.UNAVAILABLE])

    def test_invalid_token_stops_the_loop(self):
        def rejected(offset):
            raise InvalidToken()

        self.api.get_updates = rejected
        with self.assertRaises(InvalidToken):
            self.bot.run()


class TelegramApiTests(unittest.TestCase):
    def setUp(self):
        self.stub = StubServer()
        self.addCleanup(self.stub.close)
        self.api = TelegramApi("123:SEGRETO", base_url=self.stub.url)

    def reply(self, status, body):
        self.stub.status = status
        self.stub.body = body if isinstance(body, bytes) else json.dumps(body).encode()

    def test_get_updates_request(self):
        self.reply(200, {"ok": True, "result": [{"update_id": 1}]})
        self.assertEqual(self.api.get_updates(7), [{"update_id": 1}])
        sent = self.stub.requests[0]
        self.assertEqual(sent["path"], "/bot123:SEGRETO/getUpdates")
        self.assertEqual(sent["json"]["offset"], 7)
        self.assertEqual(sent["json"]["allowed_updates"], ["message"])

    def test_send_message_request(self):
        self.reply(200, {"ok": True, "result": {}})
        self.api.send_message(10, "ciao")
        sent = self.stub.requests[0]
        self.assertEqual(sent["path"], "/bot123:SEGRETO/sendMessage")
        self.assertEqual(sent["json"], {"chat_id": 10, "text": "ciao"})

    def test_401_is_invalid_token(self):
        self.reply(401, b"")
        with self.assertRaises(InvalidToken):
            self.api.get_updates(None)

    def test_errors_never_contain_the_token(self):
        for status, body in ((500, b""), (200, b"<html>"), (200, {"ok": False})):
            with self.subTest(status=status, body=body):
                self.reply(status, body)
                with self.assertRaises(TelegramError) as caught:
                    self.api.get_updates(None)
                self.assertNotIn("SEGRETO", str(caught.exception))

    def test_unreachable_server_does_not_expose_the_token(self):
        api = TelegramApi("123:SEGRETO", base_url="http://127.0.0.1:1")
        with self.assertRaises(TelegramError) as caught:
            api.send_message(10, "ciao")
        self.assertNotIn("SEGRETO", str(caught.exception))


if __name__ == "__main__":
    unittest.main()
