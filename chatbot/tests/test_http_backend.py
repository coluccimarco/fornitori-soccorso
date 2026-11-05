import io
import json
import unittest
import urllib.error
from unittest.mock import patch

from fornitori_chatbot import messages as msg
from fornitori_chatbot.backend import BackendUnavailable, MissionRequest, NotFound
from fornitori_chatbot.engine import ChatEngine, InboundMessage
from fornitori_chatbot.http_backend import HttpBackend


class HttpContractTests(unittest.TestCase):
    def setUp(self):
        self.backend = HttpBackend("http://localhost:8080", "test")

    def test_lookup_old_and_new_responses(self):
        for body, expected in (({"verificato": True, "clienteId": 1}, "VERIFICATA"),
                               ({"verificato": False}, "DA_VERIFICARE"),
                               ({"verificato": False, "esito": "DA_VERIFICARE"}, "DA_VERIFICARE")):
            with self.subTest(body=body), patch.object(self.backend, "_call", return_value=body):
                self.assertEqual(self.backend.lookup_targa("session", "AB123CD").esito, expected)

    def test_creation_payload_and_in_verifica(self):
        request = MissionRequest(session_id="session", request_id="request", cliente_id=None,
                                 targa="ZZ999ZZ", telefono_sul_posto="+393331234567",
                                 tipo_problema="INCIDENTE", origine_indirizzo="Via Roma 1",
                                 canale_origine="telegram", destinatario="chat")
        with patch.object(self.backend, "_call", return_value={
            "id": 1, "codice": "M-1", "stato": "verifica_polizza", "inVerifica": True,
        }) as call:
            self.assertTrue(self.backend.create_mission(request).in_verifica)
        self.assertEqual(call.call_args.args, ("POST", "/api/v1/chat/missioni", {
            "sessionId": "session", "requestId": "request", "clienteId": None,
            "targa": "ZZ999ZZ", "telefonoSulPosto": "+393331234567", "tipoProblema": "INCIDENTE",
            "origineIndirizzo": "Via Roma 1", "origineLat": None, "origineLng": None,
            "officinaId": None, "canaleOrigine": "telegram", "destinatario": "chat",
        }))

    def test_missing_lookup_endpoint_does_not_mean_policy_review(self):
        with patch.object(self.backend, "_call", side_effect=NotFound):
            with self.assertRaises(NotFound):
                self.backend.lookup_targa("session", "AB123CD")

    def test_invalid_lookup_response_is_a_service_error(self):
        for body in ({}, {"verificato": "false"}, {"verificato": False, "esito": "ERRORE"},
                     {"verificato": False, "esito": []}):
            with self.subTest(body=body), patch.object(self.backend, "_call", return_value=body):
                with self.assertRaises(BackendUnavailable):
                    self.backend.lookup_targa("session", "AB123CD")

    def test_http_lookup_error_keeps_dialog_waiting_for_phone_retry(self):
        engine = ChatEngine(self.backend)
        engine.handle(InboundMessage("telegram", "chat", text="AB123CD"))
        error = urllib.error.HTTPError("http://localhost/lookup", 404, "Not Found", {}, None)
        with patch("urllib.request.urlopen", side_effect=error):
            replies = engine.handle(InboundMessage("telegram", "chat", text="3331234567"))
        self.assertEqual(replies, [msg.UNAVAILABLE])
        with patch("urllib.request.urlopen", return_value=io.BytesIO(
            b'{"verificato": false, "esito": "DA_VERIFICARE"}'
        )):
            replies = engine.handle(InboundMessage("telegram", "chat", text="3331234567"))
        self.assertEqual(replies, [msg.POLICY_PENDING])

    def test_creation_json_on_the_http_request(self):
        request = MissionRequest(session_id="session", request_id="request", cliente_id=None,
                                 targa="ZZ999ZZ", telefono_sul_posto="+393331234567",
                                 tipo_problema="INCIDENTE", origine_indirizzo="Via Roma 1",
                                 canale_origine="telegram", destinatario="chat")
        response = io.BytesIO(b'{"id": 1, "codice": "M-1", "stato": "verifica_polizza", "inVerifica": true}')
        with patch("urllib.request.urlopen", return_value=response) as send:
            self.assertTrue(self.backend.create_mission(request).in_verifica)
        outgoing = send.call_args.args[0]
        self.assertEqual(outgoing.get_method(), "POST")
        self.assertEqual(outgoing.full_url, "http://localhost:8080/api/v1/chat/missioni")
        payload = json.loads(outgoing.data)
        self.assertIsNone(payload["clienteId"])
        self.assertEqual(payload["targa"], "ZZ999ZZ")
        self.assertEqual(payload["telefonoSulPosto"], "+393331234567")
        self.assertEqual(payload["tipoProblema"], "INCIDENTE")
        self.assertNotIn("tipoIntervento", payload)
        self.assertNotIn("descrizioneProblema", payload)
