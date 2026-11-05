"""Backend finto per sviluppo e demo da console: dati inventati, nessuna rete."""
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple

from .backend import (
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
    FakeCustomer(1, "Mario Prova", "Compagnia Demo", "AB123CD"),
    FakeCustomer(2, "Laura Prova", "Compagnia Demo", "EF456GH"),
)
OPEN_STATES = {"nuova", "proposta", "accettata", "non_accettata", "scaduta",
               "verifica_polizza", "gestione_manuale"}


class FakeBackend:
    def __init__(self, customers=DEMO_CUSTOMERS):
        self._customers = {c.targa_norm: c for c in customers}
        self._missions: Dict[int, MissionStatus] = {}
        self._by_request: Dict[str, MissionCreated] = {}
        self._recipients: Dict[int, Tuple[str, str]] = {}

    def lookup_targa(self, session_id: str, targa: str) -> LookupResult:
        customer = self._customers.get(targa)
        if customer is None:
            return LookupResult(verificato=False)
        return LookupResult(True, customer.id, customer.nome, customer.compagnia)

    def create_mission(self, request: MissionRequest) -> MissionCreated:
        existing = self._by_request.get(request.request_id)
        if existing is not None:
            return existing
        mission_id = len(self._missions) + 1
        in_verifica = request.cliente_id is None
        stato = "verifica_polizza" if in_verifica else "nuova"
        created = MissionCreated(mission_id, f"M-DEMO-{mission_id:05d}", stato, in_verifica)
        self._by_request[request.request_id] = created
        self._missions[mission_id] = MissionStatus(created.codice, stato)
        self._recipients[mission_id] = (request.canale_origine, request.destinatario)
        return created

    def current_status(self, channel: str, recipient: str) -> Optional[MissionStatus]:
        for mission_id in sorted(self._missions, reverse=True):
            status = self._missions[mission_id]
            if self._recipients[mission_id] == (channel, recipient) and status.stato in OPEN_STATES:
                return status
        return None

    def claim_notifications(self, channel: str) -> List[Notification]:
        return []

    def report_notification(self, notification_id: int, attempt: int, sent: bool, error: Optional[str] = None) -> None:
        pass
