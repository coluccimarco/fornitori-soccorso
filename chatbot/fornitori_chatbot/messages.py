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
INVALID_PLATE = "Il formato della targa non è valido. Controlla e riprova."
ASK_PHONE = "Inserisca il numero di telefono della persona presente sul posto."
INVALID_PHONE = "Numero non valido. Inserisca da 6 a 20 caratteri: cifre e spazi, con un eventuale + iniziale."
POLICY_PENDING = "Attenda... Stiamo verificando la copertura assicurativa del veicolo. Intanto condivida la posizione o scriva l'indirizzo."

INVALID_LOCATION = "Non ho capito la posizione. Scrivi via e città oppure condividi la posizione."
SHARED_LOCATION = "Posizione condivisa ({lat:.6f}, {lng:.6f})"
ADDRESS_NOT_FOUND = (
    "Non trovo l'indirizzo per scegliere l'officina più vicina. "
    "Scrivi via e città oppure condividi la posizione."
)
ASK_PROBLEM_TYPE = "Qual è il problema?"
CREATED = "Non appena un autista sarà disponibile riceverai un messaggio."
CREATED_CODE = "Codice richiesta {codice}. Scrivi STATO per aggiornamenti."
CREATED_PENDING = "Attenda... La centrale sta verificando la polizza e la ricontatterà al numero indicato. Codice richiesta {codice}."
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
    "verifica_polizza": "Per la richiesta {codice} stiamo verificando la copertura assicurativa",
    "gestione_manuale": "Per la richiesta {codice} la centrale sta cercando un autista disponibile",
    "annullata": "La richiesta {codice} è stata annullata",
}
NO_OPEN_REQUESTS = "Non risultano richieste aperte per questo numero. Scrivi la targa per attivare un soccorso."

NOTIFY_ACCEPTED = "La tua richiesta {codice} è stata accettata. Entro 45 minuti arriverà l'autista."
NOTIFY_CANCELLED = "La richiesta {codice} è stata annullata. Per assistenza contatti la centrale operativa."
NOTIFY_COMPLETED = "L'intervento per la richiesta {codice} è concluso. Grazie."
