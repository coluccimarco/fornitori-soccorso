"""Invia al cliente le notifiche accodate dal backend e ne riporta l'esito."""
import logging
import threading
import time
from typing import Callable, Dict, Optional, Set

from . import messages as msg
from .backend import BackendClient, BackendError, Notification

logger = logging.getLogger(__name__)

POLL_INTERVAL_SECONDS = 5
RETRY_DELAY_SECONDS = 30
MAX_ERROR_LENGTH = 200


def render(notification: Notification) -> Optional[str]:
    if notification.tipo == "MISSION_ACCEPTED":
        return msg.NOTIFY_ACCEPTED.format(
            codice=notification.codice, autista=notification.autista_nome or "in arrivo"
        )
    if notification.tipo == "MISSION_COMPLETED":
        return msg.NOTIFY_COMPLETED.format(codice=notification.codice)
    return None


class NotificationDispatcher:
    def __init__(
        self,
        backend: BackendClient,
        channel: str,
        send: Callable[[str, str], None],
        clock: Callable[[], float] = time.time,
    ):
        self._backend = backend
        self._channel = channel
        self._send = send
        self._clock = clock
        self._retry_at: Dict[int, float] = {}
        # Inviate ma con esito non ancora registrato: non vanno spedite una seconda volta.
        self._sent_unreported: Set[int] = set()

    def dispatch_once(self) -> None:
        for notification in self._backend.pending_notifications(self._channel):
            if notification.id not in self._sent_unreported:
                if self._clock() < self._retry_at.get(notification.id, 0):
                    continue
                error = self._deliver(notification)
                if error is not None:
                    self._retry_at[notification.id] = self._clock() + RETRY_DELAY_SECONDS
                    self._backend.report_notification(notification.id, False, error)
                    continue
                self._retry_at.pop(notification.id, None)
                self._sent_unreported.add(notification.id)
            self._backend.report_notification(notification.id, True)
            self._sent_unreported.discard(notification.id)

    def _deliver(self, notification: Notification) -> Optional[str]:
        text = render(notification)
        if text is None:
            return f"tipo non gestito: {notification.tipo}"[:MAX_ERROR_LENGTH]
        try:
            self._send(notification.destinatario, text)
        except Exception as exc:
            logger.warning("Notifica %s non inviata: %s", notification.id, type(exc).__name__)
            return str(exc)[:MAX_ERROR_LENGTH]
        return None

    def start(self) -> threading.Thread:
        thread = threading.Thread(target=self._run, name="notifiche", daemon=True)
        thread.start()
        return thread

    def _run(self) -> None:
        while True:
            try:
                self.dispatch_once()
            except BackendError as exc:
                logger.warning("Notifiche non disponibili: %s", type(exc).__name__)
            except Exception:
                logger.exception("Errore nell'invio delle notifiche")
            time.sleep(POLL_INTERVAL_SECONDS)
