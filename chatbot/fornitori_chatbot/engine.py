"""Macchina a stati del dialogo: riceve un messaggio e restituisce le risposte da inviare."""
import re
import threading
import time
import uuid
from collections import deque
from dataclasses import dataclass, field
from enum import Enum
from typing import Callable, Deque, Dict, List, Optional, Set, Tuple

from . import messages as msg
from .backend import (
    BackendClient,
    BackendError,
    Conflict,
    InvalidRequest,
    MissionRequest,
    NotFound,
    RateLimited,
    Unauthorized,
)

MAX_INVALID_INPUTS = 3
MAX_VERIFICATION_FAILURES = 3
VERIFICATION_LOCK_SECONDS = 15 * 60
DIALOG_TTL_SECONDS = 30 * 60
MISSION_TTL_SECONDS = 24 * 60 * 60
MAX_REMEMBERED_MESSAGE_IDS = 50

# Il bot non conosce le officine: se serve il traino lo imposta la centrale dopo la creazione.
DEFAULT_TIPO_INTERVENTO = "riparazione_sul_posto"

CANCEL_WORDS = {"annulla", "cancel", "stop"}
HELP_WORDS = {"aiuto", "help", "?"}
YES_WORDS = {"si", "sì", "s", "ok", "conferma"}
EDIT_WORDS = {"modifica", "no"}
END_WORDS = {"fine", "esci", "chiudi"}
NEW_REQUEST_WORDS = {"1", "soccorso", "richiedi soccorso", "nuova richiesta"}
STATUS_WORDS = {"2", "stato", "a che punto è il mio soccorso", "a che punto e il mio soccorso"}
PLATE_RE = re.compile(r"^[A-Z0-9]{5,20}$")
CHANNELS = {"telegram", "whatsapp"}


class State(str, Enum):
    START = "START"
    WAIT_PLATE = "WAIT_PLATE"
    VERIFY_CUSTOMER = "VERIFY_CUSTOMER"
    WAIT_LOCATION = "WAIT_LOCATION"
    WAIT_PROBLEM = "WAIT_PROBLEM"
    CONFIRM = "CONFIRM"
    SHOW_STATUS = "SHOW_STATUS"
    END = "END"


ACTIVE_DIALOG_STATES = {
    State.WAIT_PLATE,
    State.VERIFY_CUSTOMER,
    State.WAIT_LOCATION,
    State.WAIT_PROBLEM,
    State.CONFIRM,
}


class Intent(str, Enum):
    NEW_REQUEST = "new_request"
    STATUS = "status"


@dataclass(frozen=True)
class InboundMessage:
    channel: str
    chat_id: str
    text: str = ""
    message_id: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None


@dataclass
class Session:
    session_id: str
    channel: str
    last_activity: float
    state: State = State.START
    intent: Optional[Intent] = None
    invalid_inputs: int = 0
    verification_failures: int = 0
    locked_until: float = 0.0
    plate: Optional[str] = None
    customer_id: Optional[int] = None
    verification_token: Optional[str] = None
    location: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    problem: Optional[str] = None
    request_id: Optional[str] = None
    mission_id: Optional[int] = None
    seen_message_ids: Deque[str] = field(
        default_factory=lambda: deque(maxlen=MAX_REMEMBERED_MESSAGE_IDS)
    )

    def clear_request_data(self) -> None:
        self.plate = None
        self.customer_id = None
        self.location = None
        self.latitude = None
        self.longitude = None
        self.problem = None
        self.request_id = None


def normalize_plate(text: str) -> str:
    return re.sub(r"[^A-Z0-9]", "", text.upper())


class ChatEngine:
    def __init__(self, backend: BackendClient, clock: Callable[[], float] = time.time):
        self._backend = backend
        self._clock = clock
        self._sessions: Dict[Tuple[str, str], Session] = {}
        self._lock = threading.RLock()

    def handle(self, message: InboundMessage) -> List[str]:
        if message.channel not in CHANNELS:
            raise ValueError(f"Canale non supportato: {message.channel}")
        with self._lock:
            now = self._clock()
            key = (message.channel, message.chat_id)
            interrupted = self._expire_sessions(now)
            session = self._sessions.get(key)
            if session is None:
                session = Session(uuid.uuid4().hex, message.channel, now)
                self._sessions[key] = session
            if message.message_id is not None:
                if message.message_id in session.seen_message_ids:
                    return []
                session.seen_message_ids.append(message.message_id)
            session.last_activity = now
            replies = self._dispatch(session, message, now)
            return [msg.EXPIRED] + replies if key in interrupted else replies

    def _expire_sessions(self, now: float) -> Set[Tuple[str, str]]:
        """Interrompe i dialoghi inattivi e restituisce le chat da avvisare."""
        interrupted = set()
        for key, session in list(self._sessions.items()):
            idle = now - session.last_activity
            if session.state in ACTIVE_DIALOG_STATES and idle >= DIALOG_TTL_SECONDS:
                session.clear_request_data()
                session.intent = None
                session.invalid_inputs = 0
                session.state = State.START
                interrupted.add(key)
            if idle >= (MISSION_TTL_SECONDS if session.mission_id else DIALOG_TTL_SECONDS):
                del self._sessions[key]
        return interrupted

    def _dispatch(self, session: Session, message: InboundMessage, now: float) -> List[str]:
        word = message.text.strip().lower()
        if word in HELP_WORDS:
            if session.state in (State.START, State.END):
                return [msg.WELCOME]
            return [msg.HELP]
        if word in CANCEL_WORDS:
            session.clear_request_data()
            session.intent = None
            session.invalid_inputs = 0
            session.state = State.START
            return [msg.CANCELLED]
        handler = {
            State.START: self._on_start,
            State.END: self._on_start,
            State.WAIT_PLATE: self._on_plate,
            State.VERIFY_CUSTOMER: self._on_challenge,
            State.WAIT_LOCATION: self._on_location,
            State.WAIT_PROBLEM: self._on_problem,
            State.CONFIRM: self._on_confirm,
            State.SHOW_STATUS: self._on_show_status,
        }[session.state]
        return handler(session, message, now)

    def _on_start(self, session: Session, message: InboundMessage, now: float) -> List[str]:
        word = message.text.strip().lower().rstrip("?!. ")
        if word in NEW_REQUEST_WORDS:
            session.intent = Intent.NEW_REQUEST
            return self._begin_verification(session, now)
        if word in STATUS_WORDS or "a che punto" in word:
            return self._request_status(session, now)
        session.state = State.START
        return [msg.WELCOME]

    def _begin_verification(self, session: Session, now: float) -> List[str]:
        if now < session.locked_until:
            session.state = State.END
            return [msg.LOCKED]
        session.invalid_inputs = 0
        session.state = State.WAIT_PLATE
        return [msg.ASK_PLATE]

    def _on_plate(self, session: Session, message: InboundMessage, now: float) -> List[str]:
        plate = normalize_plate(message.text)
        if not PLATE_RE.match(plate):
            return self._invalid(session, msg.INVALID_PLATE)
        session.plate = plate
        session.invalid_inputs = 0
        session.state = State.VERIFY_CUSTOMER
        return [msg.ASK_CHALLENGE]

    def _on_challenge(self, session: Session, message: InboundMessage, now: float) -> List[str]:
        challenge = message.text.strip()
        if not challenge:
            return self._invalid(session, msg.ASK_CHALLENGE)
        try:
            result = self._backend.lookup_targa(session.session_id, session.plate, challenge)
        except RateLimited:
            return [msg.RATE_LIMITED]
        except InvalidRequest:
            result = None
        except BackendError:
            return [msg.UNAVAILABLE]
        if (
            result is None
            or not result.verificato
            or result.cliente_id is None
            or not result.verification_token
        ):
            return self._verification_failed(session, now)
        session.verification_failures = 0
        session.invalid_inputs = 0
        session.customer_id = result.cliente_id
        session.verification_token = result.verification_token
        session.plate = None
        verified = (
            msg.VERIFIED.format(nome=result.nome_mascherato)
            if result.nome_mascherato
            else msg.VERIFIED_NO_NAME
        )
        if session.intent == Intent.STATUS:
            session.customer_id = None
            return [verified] + self._show_status(session, now)
        session.state = State.WAIT_LOCATION
        return [verified, msg.ASK_LOCATION]

    def _verification_failed(self, session: Session, now: float) -> List[str]:
        session.plate = None
        session.verification_failures += 1
        if session.verification_failures >= MAX_VERIFICATION_FAILURES:
            session.verification_failures = 0
            session.locked_until = now + VERIFICATION_LOCK_SECONDS
            session.clear_request_data()
            session.state = State.END
            return [msg.LOCKED]
        session.state = State.WAIT_PLATE
        return [msg.VERIFICATION_FAILED, msg.ASK_PLATE]

    def _on_location(self, session: Session, message: InboundMessage, now: float) -> List[str]:
        lat, lng = message.latitude, message.longitude
        if lat is not None and lng is not None:
            if not (-90 <= lat <= 90 and -180 <= lng <= 180):
                return self._invalid(session, msg.INVALID_LOCATION)
            session.latitude, session.longitude = lat, lng
            session.location = msg.SHARED_LOCATION.format(lat=lat, lng=lng)
        else:
            text = message.text.strip()
            if not 5 <= len(text) <= 500 or not any(c.isalpha() for c in text):
                return self._invalid(session, msg.INVALID_LOCATION)
            session.latitude, session.longitude = None, None
            session.location = text
        session.invalid_inputs = 0
        session.state = State.WAIT_PROBLEM
        return [msg.ASK_PROBLEM]

    def _on_problem(self, session: Session, message: InboundMessage, now: float) -> List[str]:
        text = message.text.strip()
        if not 3 <= len(text) <= 1000:
            return self._invalid(session, msg.INVALID_PROBLEM)
        session.problem = text
        session.invalid_inputs = 0
        session.state = State.CONFIRM
        return [msg.CONFIRM.format(location=session.location, problem=session.problem)]

    def _on_confirm(self, session: Session, message: InboundMessage, now: float) -> List[str]:
        word = message.text.strip().lower()
        if word in EDIT_WORDS:
            session.invalid_inputs = 0
            session.state = State.WAIT_LOCATION
            return [msg.ASK_LOCATION]
        if word not in YES_WORDS:
            return self._invalid(session, msg.INVALID_CONFIRM)
        session.invalid_inputs = 0
        # Stabile tra i tentativi: un retry dopo un errore di rete non crea una seconda missione.
        if session.request_id is None:
            session.request_id = uuid.uuid4().hex
        return self._create_request(session)

    def _create_request(self, session: Session) -> List[str]:
        request = MissionRequest(
            session_id=session.session_id,
            request_id=session.request_id,
            verification_token=session.verification_token,
            cliente_id=session.customer_id,
            tipo_intervento=DEFAULT_TIPO_INTERVENTO,
            descrizione_problema=session.problem,
            origine_indirizzo=session.location,
            canale_origine=session.channel,
            origine_lat=session.latitude,
            origine_lng=session.longitude,
        )
        try:
            created = self._backend.create_mission(request)
        except RateLimited:
            return [msg.RATE_LIMITED]
        except Conflict:
            session.clear_request_data()
            session.state = State.END
            return [msg.CREATE_CONFLICT]
        except (InvalidRequest, Unauthorized, NotFound):
            session.clear_request_data()
            session.state = State.END
            return [msg.CREATE_REJECTED]
        except BackendError:
            return [msg.UNAVAILABLE]
        session.mission_id = created.id
        session.clear_request_data()
        session.state = State.SHOW_STATUS
        return [msg.CREATED.format(codice=created.codice)]

    def _on_show_status(self, session: Session, message: InboundMessage, now: float) -> List[str]:
        word = message.text.strip().lower().rstrip("?!. ")
        if word in END_WORDS:
            session.invalid_inputs = 0
            session.state = State.END
            return [msg.GOODBYE]
        if word in STATUS_WORDS or "a che punto" in word:
            session.invalid_inputs = 0
            return self._request_status(session, now)
        if word in NEW_REQUEST_WORDS:
            session.intent = Intent.NEW_REQUEST
            return self._begin_verification(session, now)
        return self._invalid(session, msg.AFTER_CREATE_INVALID)

    def _request_status(self, session: Session, now: float) -> List[str]:
        session.intent = Intent.STATUS
        if session.mission_id is None:
            session.state = State.END
            return [msg.NO_MISSION]
        if session.verification_token is None:
            return self._begin_verification(session, now)
        return self._show_status(session, now)

    def _show_status(self, session: Session, now: float) -> List[str]:
        try:
            status = self._backend.get_status(session.mission_id, session.verification_token)
        except Unauthorized:
            session.verification_token = None
            return [msg.REVERIFY] + self._begin_verification(session, now)
        except NotFound:
            session.mission_id = None
            session.state = State.END
            return [msg.NO_MISSION]
        except RateLimited:
            session.state = State.SHOW_STATUS
            return [msg.RATE_LIMITED]
        except BackendError:
            session.state = State.SHOW_STATUS
            return [msg.UNAVAILABLE]
        session.intent = None
        session.state = State.SHOW_STATUS
        return [_status_text(status), msg.AFTER_STATUS]

    def _invalid(self, session: Session, reply: str) -> List[str]:
        session.invalid_inputs += 1
        if session.invalid_inputs >= MAX_INVALID_INPUTS:
            session.invalid_inputs = 0
            session.clear_request_data()
            session.state = State.END
            return [msg.TOO_MANY_ATTEMPTS]
        return [reply]


def _status_text(status) -> str:
    template = msg.STATUS_TEXT.get(status.stato, msg.STATUS_UNKNOWN)
    text = template.format(codice=status.codice)
    if status.autista_nome and status.stato == "accettata":
        text += msg.DRIVER_ASSIGNED.format(autista=status.autista_nome)
    elif status.autista_nome and status.stato == "eseguita":
        text += msg.DRIVER_DONE.format(autista=status.autista_nome)
    return text
