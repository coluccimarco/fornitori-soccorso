import unittest
from unittest.mock import patch

from fornitori_chatbot.backend import MissionRequest
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
