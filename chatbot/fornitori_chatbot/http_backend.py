"""Client HTTP degli endpoint /api/v1/chat/* descritti in api/openapi.yaml."""
import json
import urllib.error
import urllib.request
from typing import Any, Dict, List, Optional
from urllib.parse import quote

from .backend import (
    AddressNotFound,
    BackendError,
    BackendUnavailable,
    Conflict,
    InvalidRequest,
    LookupResult,
    MissionCreated,
    MissionRequest,
    MissionStatus,
    NotFound,
    Notification,
    RateLimited,
    Unauthorized,
)


class HttpBackend:
    def __init__(self, base_url: str, service_token: str, timeout: float = 5.0):
        self._base_url = base_url.rstrip("/")
        self._service_token = service_token
        self._timeout = timeout

    def lookup_targa(self, session_id: str, targa: str) -> LookupResult:
        try:
            body = self._call(
                "POST",
                "/api/v1/chat/lookup-targa",
                {"sessionId": session_id, "targa": targa},
            )
        except NotFound:
            return LookupResult(verificato=False)
        return LookupResult(
            verificato=bool(body.get("verificato")),
            cliente_id=body.get("clienteId"),
            cliente_nome=body.get("clienteNome"),
            compagnia_nome=body.get("compagniaNome"),
        )

    def create_mission(self, request: MissionRequest) -> MissionCreated:
        payload = {
            "sessionId": request.session_id,
            "requestId": request.request_id,
            "clienteId": request.cliente_id,
            "tipoIntervento": request.tipo_intervento,
            "descrizioneProblema": request.descrizione_problema,
            "origineIndirizzo": request.origine_indirizzo,
            "origineLat": request.origine_lat,
            "origineLng": request.origine_lng,
            "officinaId": request.officina_id,
            "canaleOrigine": request.canale_origine,
            "destinatario": request.destinatario,
        }
        body = self._call("POST", "/api/v1/chat/missioni", payload)
        try:
            return MissionCreated(body["id"], body["codice"], body["stato"])
        except KeyError as exc:
            raise BackendUnavailable("risposta missione incompleta") from exc

    def current_status(self, channel: str, recipient: str) -> Optional[MissionStatus]:
        try:
            body = self._call("POST", "/api/v1/chat/stato", {"canale": channel, "destinatario": recipient})
        except NotFound:
            return None
        try:
            return MissionStatus(
                body["codice"], body["stato"], body.get("autistaNome"), body.get("aggiornataIl")
            )
        except KeyError as exc:
            raise BackendUnavailable("risposta stato incompleta") from exc

    def pending_notifications(self, channel: str) -> List[Notification]:
        body = self._call("GET", f"/api/v1/chat/notifiche?canale={quote(channel, safe='')}")
        try:
            return [
                Notification(
                    item["id"], item["tipo"], item["destinatario"], item["codice"], item.get("autistaNome")
                )
                for item in body["notifiche"]
            ]
        except (KeyError, TypeError) as exc:
            raise BackendUnavailable("risposta notifiche incompleta") from exc

    def report_notification(self, notification_id: int, sent: bool, error: Optional[str] = None) -> None:
        self._call(
            "POST",
            f"/api/v1/chat/notifiche/{quote(str(notification_id), safe='')}/esito",
            {"inviata": sent, "errore": error},
        )

    def _call(self, method: str, path: str, payload: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        headers = {"Authorization": f"Bearer {self._service_token}", "Accept": "application/json"}
        data = None
        if payload is not None:
            data = json.dumps(payload).encode("utf-8")
            headers["Content-Type"] = "application/json"
        request = urllib.request.Request(self._base_url + path, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=self._timeout) as response:
                raw = response.read()
        except urllib.error.HTTPError as exc:
            exc.close()
            raise _map_status(exc.code) from None
        except (urllib.error.URLError, TimeoutError, OSError) as exc:
            raise BackendUnavailable(type(exc).__name__) from None
        if not raw:
            return {}
        try:
            body = json.loads(raw)
        except ValueError:
            raise BackendUnavailable("risposta non JSON") from None
        if not isinstance(body, dict):
            raise BackendUnavailable("risposta JSON inattesa")
        return body


def _map_status(status: int) -> BackendError:
    if status == 400:
        return InvalidRequest()
    if status in (401, 403):
        return Unauthorized()
    if status == 404:
        return NotFound()
    if status == 409:
        return Conflict()
    if status == 422:
        return AddressNotFound()
    if status == 429:
        return RateLimited()
    return BackendUnavailable(f"HTTP {status}")
