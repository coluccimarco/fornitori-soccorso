"""Backend finto per sviluppo e test: dati inventati, nessuna rete."""
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple

from .backend import (
    Conflict,
    LookupResult,
    MissionCreated,
    MissionRequest,
    MissionStatus,
    Notification,
)


@dataclass(frozen=True)
class FakeCustomer:
    id: int
    nome: str
    compagnia: str
    targa_norm: str


DEMO_CUSTOMERS = (
    FakeCustomer(1, "Mario Prova", "Compagnia Test", "AB123CD"),
    FakeCustomer(2, "Laura Prova", "Compagnia Test", "EF456GH"),
)
OPEN_STATES = {"nuova", "proposta", "accettata", "non_accettata", "scaduta"}


class FakeBackend:
    def __init__(self, customers=DEMO_CUSTOMERS):
        self._customers = {c.targa_norm: c for c in customers}
        self._missions: Dict[int, MissionStatus] = {}
        self._by_request: Dict[str, MissionCreated] = {}
        self._recipients: Dict[int, Tuple[str, str]] = {}
        self._notifications: Dict[int, Notification] = {}
        self.notification_reports: List[Tuple[int, bool, Optional[str]]] = []
        self.requests: List[MissionRequest] = []

    def lookup_targa(self, session_id: str, targa: str) -> LookupResult:
        customer = self._customers.get(targa)
        if customer is None:
            return LookupResult(verificato=False)
        return LookupResult(True, customer.id, customer.nome, customer.compagnia)

    def create_mission(self, request: MissionRequest) -> MissionCreated:
        self.requests.append(request)
        existing = self._by_request.get(request.request_id)
        if existing is not None:
            return existing
        if request.tipo_intervento == "traino" and request.officina_id is None:
            raise Conflict("traino senza officina")
        mission_id = len(self._missions) + 1
        created = MissionCreated(mission_id, f"M-TEST-{mission_id:05d}", "nuova")
        self._by_request[request.request_id] = created
        self._missions[mission_id] = MissionStatus(created.codice, "nuova")
        self._recipients[mission_id] = (request.canale_origine, request.destinatario)
        return created

    def current_status(self, channel: str, recipient: str) -> Optional[MissionStatus]:
        for mission_id in sorted(self._missions, reverse=True):
            status = self._missions[mission_id]
            if self._recipients[mission_id] == (channel, recipient) and status.stato in OPEN_STATES:
                return status
        return None

    def pending_notifications(self, channel: str) -> List[Notification]:
        return list(self._notifications.values())

    def report_notification(self, notification_id: int, sent: bool, error: Optional[str] = None) -> None:
        self.notification_reports.append((notification_id, sent, error))
        if sent:
            self._notifications.pop(notification_id, None)

    def set_status(self, mission_id: int, stato: str, autista_nome: Optional[str] = None) -> None:
        """Simula l'avanzamento della missione da parte della centrale."""
        codice = self._missions[mission_id].codice
        self._missions[mission_id] = MissionStatus(codice, stato, autista_nome)
        tipo = {"accettata": "MISSION_ACCEPTED", "eseguita": "MISSION_COMPLETED"}.get(stato)
        if tipo:
            notification_id = len(self.notification_reports) + len(self._notifications) + 1
            self._notifications[notification_id] = Notification(
                notification_id, tipo, self._recipients[mission_id][1], codice, autista_nome
            )
