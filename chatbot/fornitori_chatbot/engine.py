"""Macchina a stati del dialogo: riceve un messaggio e restituisce le risposte da inviare."""
import re
import threading
import time
import uuid
from collections import deque
from dataclasses import dataclass, field, replace
from enum import Enum
from typing import Callable, Deque, Dict, List, Optional, Set, Tuple

from . import messages as msg
from .backend import (
    AddressNotFound,
    BackendClient,
    BackendError,
    Conflict,
    InvalidRequest,
    LookupExpired,
    MissionCreated,
    MissionRequest,
    MissionStatus,
    NotFound,
    RateLimited,
    Unauthorized,
)

MAX_INVALID_INPUTS = 3
DIALOG_TTL_SECONDS = 30 * 60
MAX_REMEMBERED_MESSAGE_IDS = 50

PROBLEM_TYPES = (
    ("RUOTA_CON_SCORTA", "Ruota forata con ruota di scorta"),
    ("RUOTA_SENZA_SCORTA", "Ruota forata senza ruota di scorta"),
    ("BATTERIA_SCARICA", "Batteria scarica"),
    ("GUASTO_MOTORE", "Guasto motore"),
    ("INCIDENTE", "Incidente"),
)
PROBLEM_CHOICES = {str(index): code for index, (code, _) in enumerate(PROBLEM_TYPES, 1)}
PROBLEM_CHOICES.update({code: code for code, _ in PROBLEM_TYPES})
PHONE_RE = re.compile(r"\+?[0-9][0-9 ]{5,19}")

CANCEL_WORDS = {"annulla", "cancel", "stop"}
HELP_WORDS = {"aiuto", "help"}
PLATE_TEXT_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9 .\-]*$")
PLATE_RE = re.compile(r"^(?=.*[A-Z])(?=.*[0-9])[A-Z0-9]{5,10}$")
CHANNELS = {"telegram", "whatsapp"}


class State(str, Enum):
    START = "START"
    WAIT_PHONE = "WAIT_PHONE"
    WAIT_LOCATION = "WAIT_LOCATION"
    WAIT_PROBLEM = "WAIT_PROBLEM"


ACTIVE_DIALOG_STATES = {State.WAIT_PHONE, State.WAIT_LOCATION, State.WAIT_PROBLEM}


class Reply(str):
    """Risposta testuale con tasti opzionali: (etichetta, testo inviato alla pressione)."""

    buttons: Tuple[Tuple[str, str], ...] = ()

    def __new__(cls, text: str, buttons: Tuple[Tuple[str, str], ...] = ()):
        reply = super().__new__(cls, text)
        reply.buttons = buttons
        return reply


PROBLEM_QUESTION = Reply(
    msg.ASK_PROBLEM_TYPE,
    tuple((label, code) for code, label in PROBLEM_TYPES),
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
    plate: Optional[str] = None
    location: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    phone: Optional[str] = None
    request_id: Optional[str] = None
    seen_message_ids: Deque[str] = field(
        default_factory=lambda: deque(maxlen=MAX_REMEMBERED_MESSAGE_IDS)
    )

    def reset(self) -> None:
        self.state = State.START
        self.invalid_inputs = 0
        self.customer_id = None
        self.plate = None
        self.location = None
        self.latitude = None
        self.longitude = None
        self.phone = None
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
        if session.state == State.WAIT_PHONE:
            return self._on_phone(session, message)
        if session.state == State.WAIT_LOCATION:
            return self._on_location(session, message)
        if session.state == State.WAIT_PROBLEM:
            return self._on_problem(session, message)
        return self._on_start(session, message)

    def _on_start(self, session: Session, message: InboundMessage) -> List[str]:
        text = message.text.strip()
        if not looks_like_plate(text):
            return [msg.WELCOME]
        session.plate = normalize_plate(text)
        session.state = State.WAIT_PHONE
        return [msg.ASK_PHONE]

    def _on_phone(self, session: Session, message: InboundMessage) -> List[str]:
        phone = message.text.strip()
        normalized = phone.replace(" ", "")
        if not PHONE_RE.fullmatch(phone) or not PHONE_RE.fullmatch(normalized):
            return self._invalid(session, msg.INVALID_PHONE)
        try:
            result = self._backend.lookup_targa(session.session_id, session.plate)
        except RateLimited:
            return [msg.RATE_LIMITED]
        except InvalidRequest:
            session.reset()
            return [msg.INVALID_PLATE]
        except BackendError:
            return [msg.UNAVAILABLE]
        verified = result.esito == "VERIFICATA"
        if verified and result.cliente_id is None:
            return [msg.UNAVAILABLE]
        session.phone = normalized
        session.customer_id = result.cliente_id if verified else None
        session.invalid_inputs = 0
        session.state = State.WAIT_LOCATION
        if not verified:
            return [msg.POLICY_PENDING]
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
        session.state = State.WAIT_PROBLEM
        return [PROBLEM_QUESTION]

    def _on_problem(self, session: Session, message: InboundMessage) -> List[str]:
        problem = PROBLEM_CHOICES.get(message.text.strip())
        if problem is None:
            return self._invalid(session, PROBLEM_QUESTION)
        session.invalid_inputs = 0
        # Stabile tra i tentativi: un retry dopo un errore di rete non crea una seconda missione.
        if session.request_id is None:
            session.request_id = uuid.uuid4().hex
        request = MissionRequest(
            session_id=session.session_id,
            request_id=session.request_id,
            cliente_id=session.customer_id,
            targa=session.plate,
            telefono_sul_posto=session.phone,
            tipo_problema=problem,
            origine_indirizzo=session.location,
            canale_origine=session.channel,
            destinatario=session.chat_id,
            origine_lat=session.latitude,
            origine_lng=session.longitude,
        )
        try:
            created = self._create_with_renewed_lookup(session, request)
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
        if created.in_verifica:
            return [msg.CREATED_PENDING.format(codice=created.codice)]
        return [msg.CREATED, msg.CREATED_CODE.format(codice=created.codice)]

    def _create_with_renewed_lookup(self, session: Session, request: MissionRequest) -> MissionCreated:
        """Il dialogo può durare più del riconoscimento (15 minuti): lo rinnova una volta e riprova."""
        try:
            return self._backend.create_mission(request)
        except LookupExpired:
            result = self._backend.lookup_targa(session.session_id, session.plate)
            if result.esito == "VERIFICATA" and result.cliente_id is None:
                raise InvalidRequest()
            session.customer_id = result.cliente_id if result.esito == "VERIFICATA" else None
            request = replace(request, cliente_id=session.customer_id)
            return self._backend.create_mission(request)

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
    text = msg.STATUS_TEXTS.get(status.stato, msg.STATUS_DEFAULT).format(codice=status.codice)
    return text + "."
