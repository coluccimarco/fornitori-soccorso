"""Canale Telegram in long polling: non richiede un indirizzo pubblico."""
import http.client
import json
import logging
import time
import urllib.error
import urllib.request
from typing import Any, Dict, List, Optional

from . import messages as msg
from .engine import ChatEngine, InboundMessage

logger = logging.getLogger(__name__)

API_BASE_URL = "https://api.telegram.org"
POLL_TIMEOUT_SECONDS = 30
RETRY_DELAY_SECONDS = 5


class TelegramError(Exception):
    """Errore di Telegram. Il messaggio non contiene mai l'URL, che include il token."""


class InvalidToken(TelegramError):
    """HTTP 401: il token del bot non e' valido."""


class TelegramApi:
    def __init__(self, token: str, base_url: str = API_BASE_URL):
        self._url = f"{base_url.rstrip('/')}/bot{token}"

    def get_updates(self, offset: Optional[int]) -> List[Dict[str, Any]]:
        payload: Dict[str, Any] = {
            "timeout": POLL_TIMEOUT_SECONDS,
            "allowed_updates": ["message", "callback_query"],
        }
        if offset is not None:
            payload["offset"] = offset
        result = self._call("getUpdates", payload, POLL_TIMEOUT_SECONDS + 10)
        if not isinstance(result, list):
            raise TelegramError("risposta getUpdates inattesa")
        return result

    def send_message(self, chat_id, text: str, buttons=()) -> None:
        payload: Dict[str, Any] = {"chat_id": chat_id, "text": text}
        if buttons:
            payload["reply_markup"] = {
                "inline_keyboard": [[{"text": label, "callback_data": data}] for label, data in buttons]
            }
        self._call("sendMessage", payload, 10)

    def answer_callback(self, callback_id: str) -> None:
        self._call("answerCallbackQuery", {"callback_query_id": callback_id}, 10)

    def _call(self, method: str, payload: Dict[str, Any], timeout: float) -> Any:
        request = urllib.request.Request(
            f"{self._url}/{method}",
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=timeout) as response:
                raw = response.read()
        except urllib.error.HTTPError as exc:
            exc.close()
            if exc.code == 401:
                raise InvalidToken("token non valido") from None
            raise TelegramError(f"{method}: HTTP {exc.code}") from None
        except (OSError, http.client.HTTPException) as exc:
            raise TelegramError(f"{method}: {type(exc).__name__}") from None
        try:
            body = json.loads(raw)
        except ValueError:
            raise TelegramError(f"{method}: risposta non JSON") from None
        if not isinstance(body, dict) or not body.get("ok"):
            raise TelegramError(f"{method}: risposta non valida")
        return body.get("result")


class TelegramBot:
    def __init__(self, api: TelegramApi, engine: ChatEngine):
        self._api = api
        self._engine = engine
        self._offset: Optional[int] = None

    def run(self) -> None:
        while True:
            try:
                self.poll_once()
            except InvalidToken:
                raise
            except TelegramError as exc:
                logger.warning("Telegram non raggiungibile: %s", exc)
                time.sleep(RETRY_DELAY_SECONDS)

    def poll_once(self) -> None:
        for update in self._api.get_updates(self._offset):
            # Si avanza comunque: un messaggio che fallisce non deve bloccare i successivi.
            self._offset = update["update_id"] + 1
            callback = update.get("callback_query")
            self._process(self._from_callback(callback) if callback else update.get("message"))

    def _from_callback(self, callback: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """La pressione di un tasto entra nel dialogo come se il cliente avesse scritto il suo valore."""
        try:
            self._api.answer_callback(callback["id"])
        except InvalidToken:
            raise
        except TelegramError as exc:
            logger.warning("Tasto non confermato: %s", exc)
        message = callback.get("message")
        if not message:
            return None
        return {"chat": message.get("chat", {}), "text": callback.get("data"), "message_id": f"cb-{callback['id']}"}

    def _process(self, message: Optional[Dict[str, Any]]) -> None:
        if not message or message.get("chat", {}).get("type") != "private":
            return
        chat_id = message["chat"]["id"]
        try:
            replies = self._replies_for(message, chat_id)
        except Exception:
            logger.exception("Errore nel dialogo")
            replies = [msg.UNAVAILABLE]
        for reply in replies:
            try:
                self._api.send_message(chat_id, reply, getattr(reply, "buttons", ()))
            except InvalidToken:
                raise
            except TelegramError as exc:
                logger.warning("Risposta non inviata: %s", exc)

    def _replies_for(self, message: Dict[str, Any], chat_id: int) -> List[str]:
        chat = str(chat_id)
        text = (message.get("text") or "").strip()
        if text == "/start":
            # Telegram lo invia all'apertura del bot: riparte sempre dal menu.
            self._engine.handle(InboundMessage("telegram", chat, text="annulla"))
            text = "ciao"
        location = message.get("location") or {}
        return self._engine.handle(
            InboundMessage(
                channel="telegram",
                chat_id=chat,
                text=text,
                message_id=str(message.get("message_id")),
                latitude=location.get("latitude"),
                longitude=location.get("longitude"),
            )
        )
