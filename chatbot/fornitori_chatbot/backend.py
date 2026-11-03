"""Contratto del chatbot verso il backend (api/openapi.yaml, tag Chat)."""
from dataclasses import dataclass
from typing import Optional, Protocol


class BackendError(Exception):
    """Errore generico del backend."""


class BackendUnavailable(BackendError):
    """Backend irraggiungibile, 5xx o risposta illeggibile: si puo' riprovare."""


class RateLimited(BackendError):
    """HTTP 429."""


class Unauthorized(BackendError):
    """HTTP 401/403 sullo stato: token di verifica scaduto o non valido."""


class NotFound(BackendError):
    """HTTP 404."""


class Conflict(BackendError):
    """HTTP 409 sulla creazione missione."""


class InvalidRequest(BackendError):
    """HTTP 400."""


@dataclass(frozen=True)
class LookupResult:
    verificato: bool
    cliente_id: Optional[int] = None
    nome_mascherato: Optional[str] = None
    verification_token: Optional[str] = None


@dataclass(frozen=True)
class MissionRequest:
    session_id: str
    request_id: str
    cliente_id: int
    tipo_intervento: str
    descrizione_problema: str
    origine_indirizzo: str
    canale_origine: str
    origine_lat: Optional[float] = None
    origine_lng: Optional[float] = None
    officina_id: Optional[int] = None


@dataclass(frozen=True)
class MissionCreated:
    id: int
    codice: str
    stato: str


@dataclass(frozen=True)
class MissionStatus:
    codice: str
    stato: str
    autista_nome: Optional[str] = None
    aggiornata_il: Optional[str] = None


class BackendClient(Protocol):
    def lookup_targa(self, session_id: str, targa: str, challenge: str) -> LookupResult:
        ...

    def create_mission(self, request: MissionRequest) -> MissionCreated:
        ...

    def get_status(self, mission_id: int, verification_token: str) -> MissionStatus:
        ...
