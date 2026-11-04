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
        self.buttons = []
        self.answered = []
        self.answer_error = None
        self.send_error = None

    def get_updates(self, offset):
        self.offsets.append(offset)
        return self.batches.pop(0) if self.batches else []

    def send_message(self, chat_id, text, buttons=()):
        if self.send_error:
            raise self.send_error
        self.sent.append((chat_id, text))
        self.buttons.append(buttons)

    def answer_callback(self, callback_id):
        if self.answer_error:
            raise self.answer_error
        self.answered.append(callback_id)


def update(update_id, chat_id=10, chat_type="private", **fields):
    message = {"message_id": update_id * 100, "chat": {"id": chat_id, "type": chat_type}}
    message.update(fields)
    return {"update_id": update_id, "message": message}


def press(update_id, data, chat_id=10, chat_type="private"):
    return {
        "update_id": update_id,
        "callback_query": {
            "id": f"cb-id-{update_id}",
            "data": data,
            "message": {"message_id": 1, "chat": {"id": chat_id, "type": chat_type}},
        },
    }


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
        self.deliver(update(2, text="AB 123 CD"))
        self.deliver(update(3, location={"latitude": 41.2, "longitude": 16.6}))
        self.assertEqual(
            self.api.buttons[-1],
            (("Riparazione sul posto", "riparazione"), ("Traino", "traino")),
        )
        self.deliver(press(4, "traino"))
        self.assertEqual(self.api.answered, ["cb-id-4"])
        self.assertEqual(self.texts()[-1], msg.ASK_PROBLEM)
        self.deliver(update(5, text="Motore in panne"))
        self.assertIn("M-TEST-00001", self.texts()[-1])

    def test_start_restarts_an_open_dialog(self):
        self.deliver(update(1, text="AB 123 CD"))
        self.deliver(update(2, text="/start"))
        self.assertEqual(self.texts()[-1], msg.WELCOME)

    def test_repeated_button_press_is_ignored(self):
        self.deliver(update(1, text="AB 123 CD"))
        self.deliver(update(2, text="Via Roma 1, Molfetta"))
        self.deliver(press(3, "traino"), {"update_id": 4, "callback_query": press(3, "traino")["callback_query"]})
        self.assertEqual(self.texts().count(msg.ASK_PROBLEM), 1)

    def test_button_in_group_is_ignored(self):
        self.deliver(press(1, "traino", chat_type="group"))
        self.assertEqual(self.api.sent, [])

    def test_callback_confirmation_failure_does_not_stop_dialog(self):
        self.deliver(update(1, text="AB 123 CD"))
        self.deliver(update(2, text="Via Roma 1, Molfetta"))
        self.api.answer_error = TelegramError("answerCallbackQuery: HTTP 500")
        with self.assertLogs("fornitori_chatbot.telegram_channel", logging.WARNING):
            self.deliver(press(3, "traino"))
        self.assertEqual(self.texts()[-1], msg.ASK_PROBLEM)

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
        self.deliver(update(1, text="AB 123 CD"))
        self.deliver(update(2, photo=[{"file_id": "x"}]))
        self.assertEqual(self.texts()[-1], msg.INVALID_LOCATION)

    def test_chats_are_separate(self):
        self.deliver(update(1, chat_id=10, text="AB 123 CD"))
        self.deliver(update(2, chat_id=20, text="ciao"))
        self.assertEqual(self.api.sent[-1], (20, msg.WELCOME))

    def test_send_failure_does_not_stop_next_update(self):
        self.api.send_error = TelegramError("sendMessage: HTTP 500")
        with self.assertLogs("fornitori_chatbot.telegram_channel", logging.WARNING):
            self.deliver(update(1, text="ciao"))
        self.api.send_error = None
        self.deliver(update(2, text="stato"))
        self.assertEqual(self.texts(), [msg.NO_OPEN_REQUESTS])

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
        self.assertEqual(sent["json"]["allowed_updates"], ["message", "callback_query"])

    def test_send_message_request(self):
        self.reply(200, {"ok": True, "result": {}})
        self.api.send_message(10, "ciao")
        sent = self.stub.requests[0]
        self.assertEqual(sent["path"], "/bot123:SEGRETO/sendMessage")
        self.assertEqual(sent["json"], {"chat_id": 10, "text": "ciao"})

    def test_send_message_with_buttons(self):
        self.reply(200, {"ok": True, "result": {}})
        self.api.send_message(10, "scegli", (("Traino", "traino"),))
        markup = self.stub.requests[0]["json"]["reply_markup"]
        self.assertEqual(markup["inline_keyboard"], [[{"text": "Traino", "callback_data": "traino"}]])

    def test_answer_callback_request(self):
        self.reply(200, {"ok": True, "result": True})
        self.api.answer_callback("abc")
        sent = self.stub.requests[0]
        self.assertEqual(sent["path"], "/bot123:SEGRETO/answerCallbackQuery")
        self.assertEqual(sent["json"], {"callback_query_id": "abc"})

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
