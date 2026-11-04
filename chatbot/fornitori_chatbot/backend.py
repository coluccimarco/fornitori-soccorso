"""Contratto del chatbot verso il backend (api/openapi.yaml, tag Chat)."""
from dataclasses import dataclass
from typing import List, Optional, Protocol


class BackendError(Exception):
    """Errore generico del backend."""


class BackendUnavailable(BackendError):
    """Backend irraggiungibile, 5xx o risposta illeggibile: si puo' riprovare."""


class RateLimited(BackendError):
    """HTTP 429."""


class Unauthorized(BackendError):
    """HTTP 401/403: token di servizio mancante o errato."""


class LookupExpired(Unauthorized):
    """HTTP 403 sulla creazione: il riconoscimento della targa è assente o scaduto."""


class NotFound(BackendError):
    """HTTP 404."""


class Conflict(BackendError):
    """HTTP 409 sulla creazione missione."""


class InvalidRequest(BackendError):
    """HTTP 400."""


class AddressNotFound(BackendError):
    """HTTP 422: indirizzo non convertibile in coordinate."""


@dataclass(frozen=True)
class LookupResult:
    verificato: bool
    cliente_id: Optional[int] = None
    cliente_nome: Optional[str] = None
    compagnia_nome: Optional[str] = None


@dataclass(frozen=True)
class MissionRequest:
    session_id: str
    request_id: str
    cliente_id: int
    tipo_intervento: str
    descrizione_problema: str
    origine_indirizzo: str
    canale_origine: str
    destinatario: str
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


@dataclass(frozen=True)
class Notification:
    id: int
    tipo: str
    destinatario: str
    codice: str
    autista_nome: Optional[str] = None
    tentativo: int = 0


class BackendClient(Protocol):
    def lookup_targa(self, session_id: str, targa: str) -> LookupResult:
        ...

    def create_mission(self, request: MissionRequest) -> MissionCreated:
        ...

    def current_status(self, channel: str, recipient: str) -> Optional[MissionStatus]:
        ...

    def claim_notifications(self, channel: str) -> List[Notification]:
        ...

    def report_notification(self, notification_id: int, attempt: int, sent: bool, error: Optional[str] = None) -> None:
        ...
