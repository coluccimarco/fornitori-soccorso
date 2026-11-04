"""Avvio del chatbot: python3 -m fornitori_chatbot [--http] [--telegram]"""
import argparse
import logging
import os
import sys

from .engine import ChatEngine, InboundMessage
from .fake_backend import DEMO_CUSTOMERS, FakeBackend
from .http_backend import HttpBackend
from .notifications import NotificationDispatcher
from .telegram_channel import InvalidToken, TelegramApi, TelegramBot


def build_backend(use_http: bool):
    if not use_http:
        print("Backend finto. Dati di prova:")
        for customer in DEMO_CUSTOMERS:
            print(f"  targa {customer.targa_norm}")
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


def run_console(engine: ChatEngine) -> None:
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


def run_telegram(engine: ChatEngine, backend) -> None:
    token = os.environ.get("TELEGRAM_BOT_TOKEN")
    if not token:
        sys.exit("Imposta TELEGRAM_BOT_TOKEN per usare Telegram.")
    api = TelegramApi(token)
    NotificationDispatcher(backend, "telegram", api.send_message).start()
    print("Bot Telegram attivo. Premi Ctrl+C per fermarlo.")
    try:
        TelegramBot(api, engine).run()
    except InvalidToken:
        sys.exit("Telegram ha rifiutato il token: controlla TELEGRAM_BOT_TOKEN.")
    except KeyboardInterrupt:
        print()


def main() -> None:
    parser = argparse.ArgumentParser(description="Soccorso Stradale Bot")
    parser.add_argument("--http", action="store_true", help="usa il backend reale")
    parser.add_argument("--telegram", action="store_true", help="collega il bot a Telegram")
    args = parser.parse_args()

    logging.basicConfig(level=logging.WARNING, format="%(levelname)s %(name)s: %(message)s")
    backend = build_backend(args.http)
    engine = ChatEngine(backend)
    if args.telegram:
        run_telegram(engine, backend)
    else:
        run_console(engine)


if __name__ == "__main__":
    main()
