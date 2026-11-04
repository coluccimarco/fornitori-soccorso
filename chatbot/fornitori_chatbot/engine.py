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
    AddressNotFound,
    BackendClient,
    BackendError,
    Conflict,
    InvalidRequest,
    MissionRequest,
    MissionStatus,
    NotFound,
    RateLimited,
    Unauthorized,
)

MAX_INVALID_INPUTS = 3
DIALOG_TTL_SECONDS = 30 * 60
MAX_REMEMBERED_MESSAGE_IDS = 50

REPAIR = "riparazione_sul_posto"
TOWING = "traino"
INTERVENTION_WORDS = {
    "1": REPAIR,
    "riparazione": REPAIR,
    "riparazione sul posto": REPAIR,
    "2": TOWING,
    TOWING: TOWING,
}

CANCEL_WORDS = {"annulla", "cancel", "stop"}
HELP_WORDS = {"aiuto", "help"}
PLATE_TEXT_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9 .\-]*$")
PLATE_RE = re.compile(r"^(?=.*[A-Z])(?=.*[0-9])[A-Z0-9]{5,10}$")
CHANNELS = {"telegram", "whatsapp"}


class State(str, Enum):
    START = "START"
    WAIT_LOCATION = "WAIT_LOCATION"
    WAIT_INTERVENTION = "WAIT_INTERVENTION"
    WAIT_PROBLEM = "WAIT_PROBLEM"


ACTIVE_DIALOG_STATES = {State.WAIT_LOCATION, State.WAIT_INTERVENTION, State.WAIT_PROBLEM}


class Reply(str):
    """Risposta testuale con tasti opzionali: (etichetta, testo inviato alla pressione)."""

    buttons: Tuple[Tuple[str, str], ...] = ()

    def __new__(cls, text: str, buttons: Tuple[Tuple[str, str], ...] = ()):
        reply = super().__new__(cls, text)
        reply.buttons = buttons
        return reply


INTERVENTION_QUESTION = Reply(
    msg.ASK_INTERVENTION,
    ((msg.BUTTON_REPAIR, "riparazione"), (msg.BUTTON_TOWING, TOWING)),
)


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
    chat_id: str
    last_activity: float
    state: State = State.START
    invalid_inputs: int = 0
    customer_id: Optional[int] = None
    location: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    intervention: Optional[str] = None
    request_id: Optional[str] = None
    seen_message_ids: Deque[str] = field(
        default_factory=lambda: deque(maxlen=MAX_REMEMBERED_MESSAGE_IDS)
    )

    def reset(self) -> None:
        self.state = State.START
        self.invalid_inputs = 0
        self.customer_id = None
        self.location = None
        self.latitude = None
        self.longitude = None
        self.intervention = None
        self.request_id = None


def normalize_plate(text: str) -> str:
    return re.sub(r"[^A-Z0-9]", "", text.upper())


def looks_like_plate(text: str) -> bool:
    return bool(PLATE_TEXT_RE.match(text)) and bool(PLATE_RE.match(normalize_plate(text)))


def is_status_request(text: str) -> bool:
    word = text.strip().lower().rstrip("?!. ")
    return word == "stato" or "a che punto" in word


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
                session = Session(uuid.uuid4().hex, message.channel, message.chat_id, now)
                self._sessions[key] = session
            if message.message_id is not None:
                if message.message_id in session.seen_message_ids:
                    return []
                session.seen_message_ids.append(message.message_id)
            session.last_activity = now
            replies = self._dispatch(session, message)
            return [msg.EXPIRED] + replies if key in interrupted else replies

    def _expire_sessions(self, now: float) -> Set[Tuple[str, str]]:
        """Chiude le conversazioni inattive e restituisce le chat con un dialogo interrotto."""
        interrupted = set()
        for key, session in list(self._sessions.items()):
            if now - session.last_activity >= DIALOG_TTL_SECONDS:
                if session.state in ACTIVE_DIALOG_STATES:
                    interrupted.add(key)
                del self._sessions[key]
        return interrupted

    def _dispatch(self, session: Session, message: InboundMessage) -> List[str]:
        word = message.text.strip().lower()
        if word in HELP_WORDS:
            return [msg.WELCOME] if session.state == State.START else [msg.HELP]
        if word in CANCEL_WORDS:
            session.reset()
            return [msg.CANCELLED]
        if is_status_request(message.text):
            return self._show_status(session)
        if session.state == State.WAIT_LOCATION:
            return self._on_location(session, message)
        if session.state == State.WAIT_INTERVENTION:
            return self._on_intervention(session, message)
        if session.state == State.WAIT_PROBLEM:
            return self._on_problem(session, message)
        return self._on_start(session, message)

    def _on_start(self, session: Session, message: InboundMessage) -> List[str]:
        text = message.text.strip()
        if not looks_like_plate(text):
            return [msg.WELCOME]
        try:
            result = self._backend.lookup_targa(session.session_id, normalize_plate(text))
        except RateLimited:
            return [msg.RATE_LIMITED]
        except InvalidRequest:
            return [msg.PLATE_NOT_FOUND]
        except BackendError:
            return [msg.UNAVAILABLE]
        if not result.verificato or result.cliente_id is None:
            return [msg.PLATE_NOT_FOUND]
        session.customer_id = result.cliente_id
        session.state = State.WAIT_LOCATION
        return [msg.PLATE_FOUND.format(nome=result.cliente_nome, compagnia=result.compagnia_nome)]

    def _on_location(self, session: Session, message: InboundMessage) -> List[str]:
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
        session.state = State.WAIT_INTERVENTION
        return [INTERVENTION_QUESTION]

    def _on_intervention(self, session: Session, message: InboundMessage) -> List[str]:
        intervention = INTERVENTION_WORDS.get(message.text.strip().lower())
        if intervention is None:
            return self._invalid(session, INTERVENTION_QUESTION)
        session.intervention = intervention
        session.invalid_inputs = 0
        session.state = State.WAIT_PROBLEM
        return [msg.ASK_PROBLEM]

    def _on_problem(self, session: Session, message: InboundMessage) -> List[str]:
        problem = message.text.strip()
        if not 3 <= len(problem) <= 1000:
            return self._invalid(session, msg.INVALID_PROBLEM)
        session.invalid_inputs = 0
        # Stabile tra i tentativi: un retry dopo un errore di rete non crea una seconda missione.
        if session.request_id is None:
            session.request_id = uuid.uuid4().hex
        request = MissionRequest(
            session_id=session.session_id,
            request_id=session.request_id,
            cliente_id=session.customer_id,
            tipo_intervento=session.intervention,
            descrizione_problema=problem,
            origine_indirizzo=session.location,
            canale_origine=session.channel,
            destinatario=session.chat_id,
            origine_lat=session.latitude,
            origine_lng=session.longitude,
        )
        try:
            created = self._backend.create_mission(request)
        except AddressNotFound:
            session.state = State.WAIT_LOCATION
            return [msg.ADDRESS_NOT_FOUND]
        except RateLimited:
            return [msg.RATE_LIMITED]
        except Conflict:
            session.reset()
            return [msg.CREATE_CONFLICT]
        except (InvalidRequest, Unauthorized, NotFound):
            session.reset()
            return [msg.CREATE_REJECTED]
        except BackendError:
            return [msg.UNAVAILABLE]
        session.reset()
        return [msg.CREATED.format(codice=created.codice)]

    def _show_status(self, session: Session) -> List[str]:
        try:
            status = self._backend.current_status(session.channel, session.chat_id)
        except RateLimited:
            return [msg.RATE_LIMITED]
        except BackendError:
            return [msg.UNAVAILABLE]
        if status is None:
            return [msg.NO_OPEN_REQUESTS]
        return [_status_text(status)]

    def _invalid(self, session: Session, reply: str) -> List[str]:
        session.invalid_inputs += 1
        if session.invalid_inputs >= MAX_INVALID_INPUTS:
            session.reset()
            return [msg.TOO_MANY_ATTEMPTS]
        return [reply]


def _status_text(status: MissionStatus) -> str:
    text = msg.STATUS.format(codice=status.codice, stato=status.stato.replace("_", " "))
    if status.autista_nome:
        text += msg.STATUS_DRIVER.format(autista=status.autista_nome)
    return text + "."
