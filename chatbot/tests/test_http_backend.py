"""Verifica su un server HTTP locale che le richieste rispettino api/openapi.yaml."""
import json
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from fornitori_chatbot.backend import (
    BackendUnavailable,
    Conflict,
    InvalidRequest,
    MissionRequest,
    RateLimited,
    Unauthorized,
)
from fornitori_chatbot.http_backend import HttpBackend


class StubServer:
    def __init__(self):
        self.status = 200
        self.body = b"{}"
        self.requests = []
        stub = self

        class Handler(BaseHTTPRequestHandler):
            def _respond(self):
                length = int(self.headers.get("Content-Length") or 0)
                raw = self.rfile.read(length) if length else b""
                stub.requests.append(
                    {
                        "method": self.command,
                        "path": self.path,
                        "headers": dict(self.headers),
                        "json": json.loads(raw) if raw else None,
                    }
                )
                self.send_response(stub.status)
                self.send_header("Content-Length", str(len(stub.body)))
                self.end_headers()
                self.wfile.write(stub.body)

            do_GET = _respond
            do_POST = _respond

            def log_message(self, *args):
                pass

        self.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.url = f"http://127.0.0.1:{self.server.server_port}"

    def close(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()


class HttpBackendTests(unittest.TestCase):
    def setUp(self):
        self.stub = StubServer()
        self.addCleanup(self.stub.close)
        self.backend = HttpBackend(self.stub.url + "/", "token-di-prova", timeout=2)

    def reply(self, status, body):
        self.stub.status = status
        self.stub.body = body if isinstance(body, bytes) else json.dumps(body).encode()

    def mission_request(self):
        return MissionRequest(
            session_id="s" * 32,
            request_id="r" * 32,
            verification_token="token-verifica",
            cliente_id=7,
            tipo_intervento="riparazione_sul_posto",
            descrizione_problema="Motore in panne",
            origine_indirizzo="Via Roma 1",
            canale_origine="telegram",
        )

    def test_lookup_payload_and_result(self):
        self.reply(
            200,
            {
                "verificato": True,
                "clienteId": 7,
                "nomeMascherato": "M*** R***",
                "verificationToken": "abc",
            },
        )
        result = self.backend.lookup_targa("s" * 32, "AB123CD", "POL-1")
        sent = self.stub.requests[0]
        self.assertEqual((sent["method"], sent["path"]), ("POST", "/api/v1/chat/lookup-targa"))
        self.assertEqual(sent["headers"]["Authorization"], "Bearer token-di-prova")
        self.assertEqual(
            sent["json"],
            {"sessionId": "s" * 32, "targa": "AB123CD", "verificationChallenge": "POL-1"},
        )
        self.assertTrue(result.verificato)
        self.assertEqual(result.cliente_id, 7)
        self.assertEqual(result.verification_token, "abc")

    def test_lookup_404_is_not_verified(self):
        self.reply(404, b"")
        self.assertFalse(self.backend.lookup_targa("s" * 32, "AB123CD", "X").verificato)

    def test_create_mission_payload(self):
        self.reply(201, {"id": 31, "codice": "M-2026-00031", "stato": "nuova"})
        created = self.backend.create_mission(self.mission_request())
        sent = self.stub.requests[0]
        self.assertEqual(sent["path"], "/api/v1/chat/missioni")
        self.assertEqual(sent["headers"]["X-Verification-Token"], "token-verifica")
        self.assertNotIn("verificationToken", sent["json"])
        self.assertEqual(sent["json"]["requestId"], "r" * 32)
        self.assertEqual(sent["json"]["clienteId"], 7)
        self.assertEqual(sent["json"]["canaleOrigine"], "telegram")
        self.assertEqual(sent["json"]["tipoIntervento"], "riparazione_sul_posto")
        self.assertEqual(created.id, 31)

    def test_status_sends_verification_header(self):
        self.reply(200, {"codice": "M-1", "stato": "accettata", "autistaNome": "Autista Prova"})
        status = self.backend.get_status(31, "abc")
        sent = self.stub.requests[0]
        self.assertEqual((sent["method"], sent["path"]), ("GET", "/api/v1/chat/missioni/31/stato"))
        self.assertEqual(sent["headers"]["X-Verification-Token"], "abc")
        self.assertEqual(status.autista_nome, "Autista Prova")

    def test_status_codes_are_mapped(self):
        for code, error in (
            (400, InvalidRequest),
            (401, Unauthorized),
            (403, Unauthorized),
            (409, Conflict),
            (429, RateLimited),
            (500, BackendUnavailable),
        ):
            with self.subTest(code=code):
                self.reply(code, b"")
                with self.assertRaises(error):
                    self.backend.create_mission(self.mission_request())

    def test_non_json_response_is_unavailable(self):
        self.reply(200, b"<html>")
        with self.assertRaises(BackendUnavailable):
            self.backend.get_status(1, "abc")

    def test_incomplete_response_is_unavailable(self):
        self.reply(201, {"id": 1})
        with self.assertRaises(BackendUnavailable):
            self.backend.create_mission(self.mission_request())

    def test_unreachable_server_is_unavailable(self):
        backend = HttpBackend("http://127.0.0.1:1", "token", timeout=1)
        with self.assertRaises(BackendUnavailable):
            backend.get_status(1, "abc")


if __name__ == "__main__":
    unittest.main()
