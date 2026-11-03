"""Console di prova: python3 -m fornitori_chatbot [--http]"""
import argparse
import os
import sys

from .engine import ChatEngine, InboundMessage
from .fake_backend import DEMO_CUSTOMERS, FakeBackend
from .http_backend import HttpBackend


def build_backend(use_http: bool):
    if not use_http:
        print("Backend finto. Dati di prova:")
        for customer in DEMO_CUSTOMERS:
            print(f"  targa {customer.targa_norm} - polizza {customer.polizza}")
        return FakeBackend()
    base_url = os.environ.get("FORNITORI_API_URL", "http://localhost:8080")
    token = os.environ.get("FORNITORI_CHAT_TOKEN")
    if not token:
        sys.exit("Imposta FORNITORI_CHAT_TOKEN per usare il backend reale.")
    print(f"Backend reale: {base_url}")
    return HttpBackend(base_url, token)


def parse_location(text: str):
    """Il comando /pos LAT LNG simula la posizione condivisa dal canale."""
    parts = text.split()
    if len(parts) != 3 or parts[0] != "/pos":
        return None
    try:
        return float(parts[1]), float(parts[2])
    except ValueError:
        return None


def main() -> None:
    parser = argparse.ArgumentParser(description="Console di prova del chatbot FORNITORI")
    parser.add_argument("--http", action="store_true", help="usa il backend reale")
    args = parser.parse_args()

    engine = ChatEngine(build_backend(args.http))
    print("Scrivi un messaggio. /pos LAT LNG simula la posizione condivisa, /esci termina.\n")
    while True:
        try:
            text = input("Tu: ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            return
        if text == "/esci":
            return
        position = parse_location(text)
        if position:
            message = InboundMessage("telegram", "console", latitude=position[0], longitude=position[1])
        else:
            message = InboundMessage("telegram", "console", text=text)
        for reply in engine.handle(message):
            print(f"Bot: {reply}")
        print()


if __name__ == "__main__":
    main()
