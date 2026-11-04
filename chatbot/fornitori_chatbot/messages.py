"""Testi mostrati al cliente: seguono il foglio DialoghiChatbot dei dati di esempio."""

WELCOME = (
    "Ciao, sono Soccorso Stradale Bot. Per attivare un soccorso scrivi la targa del veicolo; "
    "per conoscere lo stato scrivi STATO."
)
HELP = "Rispondi alla domanda che ti ho fatto, oppure scrivi ANNULLA per ricominciare da capo."
CANCELLED = "Richiesta annullata. Scrivi la targa quando vuoi attivare un soccorso."
EXPIRED = "La conversazione precedente è scaduta, ricominciamo."
TOO_MANY_ATTEMPTS = "Non riesco a capire. Contatta la centrale operativa per ricevere assistenza."
RATE_LIMITED = "Troppi tentativi. Riprova tra qualche minuto oppure chiama la centrale."
UNAVAILABLE = "Il servizio non è disponibile in questo momento. Riprova tra poco."

PLATE_FOUND = "Targa riconosciuta ({nome}, {compagnia}). Condividi la posizione o scrivi l'indirizzo."
PLATE_NOT_FOUND = "Non trovo la targa tra i nostri assicurati. Controlla e riprova oppure chiama la centrale."

INVALID_LOCATION = "Non ho capito la posizione. Scrivi via e città oppure condividi la posizione."
SHARED_LOCATION = "Posizione condivisa ({lat:.6f}, {lng:.6f})"
ASK_INTERVENTION = (
    "Che intervento ti serve? Premi un tasto, oppure scrivi 1 per la riparazione sul posto "
    "o 2 per il traino."
)
BUTTON_REPAIR = "Riparazione sul posto"
BUTTON_TOWING = "Traino"
ADDRESS_NOT_FOUND = (
    "Non trovo l'indirizzo per scegliere l'officina più vicina. "
    "Scrivi via e città oppure condividi la posizione."
)
ASK_PROBLEM = "Descrivi in poche parole il problema."
INVALID_PROBLEM = "Descrivi il problema con qualche parola in più."
CREATED = (
    "Richiesta {codice} presa in carico. Ti avviseremo appena un autista accetta. "
    "Scrivi STATO per aggiornamenti."
)
CREATE_CONFLICT = "Non posso registrare la richiesta. Contatta la centrale operativa."
CREATE_REJECTED = "La richiesta non è stata accettata dal sistema. Contatta la centrale operativa."

STATUS_DEFAULT = "La tua richiesta {codice} è in lavorazione"
STATUS_TEXTS = {
    "nuova": "La tua richiesta {codice} è stata ricevuta: stiamo cercando un autista disponibile",
    "proposta": "La tua richiesta {codice} è stata inviata a un autista: attendiamo la sua conferma",
    "accettata": "La tua richiesta {codice} è stata presa in carico",
    "non_accettata": "La tua richiesta {codice} è in lavorazione: stiamo cercando un altro autista",
    "scaduta": "La tua richiesta {codice} è in lavorazione: stiamo cercando un altro autista",
    "eseguita": "L'intervento per la richiesta {codice} è concluso",
}
STATUS_DRIVER = "; autista assegnato: {autista}"
NO_OPEN_REQUESTS = "Non risultano richieste aperte per questo numero. Scrivi la targa per attivare un soccorso."

NOTIFY_ACCEPTED = "La tua richiesta {codice} è stata presa in carico. Autista: {autista}."
NOTIFY_COMPLETED = "L'intervento per la richiesta {codice} è concluso. Grazie."
