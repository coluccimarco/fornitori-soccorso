"""Backend finto per sviluppo e test: dati inventati, nessuna rete."""
import secrets
from dataclasses import dataclass
from typing import Dict, Optional

from .backend import (
    Conflict,
    LookupResult,
    MissionCreated,
    MissionRequest,
    MissionStatus,
    NotFound,
    Unauthorized,
)


@dataclass(frozen=True)
class FakeCustomer:
    id: int
    nome_mascherato: str
    targa_norm: str
    polizza: str


DEMO_CUSTOMERS = (
    FakeCustomer(1, "M*** R***", "AB123CD", "POL-TEST-001"),
    FakeCustomer(2, "L*** B***", "EF456GH", "POL-TEST-002"),
)


class FakeBackend:
    def __init__(self, customers=DEMO_CUSTOMERS):
        self._customers = {c.targa_norm: c for c in customers}
        self._missions: Dict[int, MissionStatus] = {}
        self._by_request: Dict[str, MissionCreated] = {}
        self._tokens: Dict[str, int] = {}
        self._owners: Dict[int, int] = {}
        self.create_calls = 0

    def lookup_targa(self, session_id: str, targa: str, challenge: str) -> LookupResult:
        customer = self._customers.get(targa)
        if customer is None or not secrets.compare_digest(
            customer.polizza.encode(), challenge.strip().upper().encode()
        ):
            return LookupResult(verificato=False)
        token = secrets.token_urlsafe(24)
        self._tokens[token] = customer.id
        return LookupResult(True, customer.id, customer.nome_mascherato, token)

    def create_mission(self, request: MissionRequest) -> MissionCreated:
        self.create_calls += 1
        existing = self._by_request.get(request.request_id)
        if existing is not None:
            return existing
        if request.tipo_intervento == "traino" and request.officina_id is None:
            raise Conflict("traino senza officina")
        mission_id = len(self._missions) + 1
        created = MissionCreated(mission_id, f"M-TEST-{mission_id:05d}", "nuova")
        self._by_request[request.request_id] = created
        self._missions[mission_id] = MissionStatus(created.codice, "nuova")
        self._owners[mission_id] = request.cliente_id
        return created

    def get_status(self, mission_id: int, verification_token: str) -> MissionStatus:
        customer_id = self._tokens.get(verification_token)
        if customer_id is None:
            raise Unauthorized()
        if mission_id not in self._missions or self._owners[mission_id] != customer_id:
            raise NotFound()
        return self._missions[mission_id]

    def set_status(self, mission_id: int, stato: str, autista_nome: Optional[str] = None) -> None:
        """Simula l'avanzamento della missione da parte della centrale."""
        codice = self._missions[mission_id].codice
        self._missions[mission_id] = MissionStatus(codice, stato, autista_nome)

    def revoke_tokens(self) -> None:
        """Simula la scadenza dei token di verifica."""
        self._tokens.clear()
