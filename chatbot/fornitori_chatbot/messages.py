"""Testi mostrati al cliente, raccolti qui per poterli rivedere senza toccare il dialogo."""

WELCOME = (
    "Benvenuto nel soccorso stradale.\n"
    "I dati che ci fornisci servono solo a gestire il soccorso: "
    "scegliendo un'opzione acconsenti al loro trattamento.\n"
    "1 - Richiedi soccorso\n"
    "2 - A che punto è il mio soccorso?"
)
HELP = (
    "Rispondi alla domanda che ti ho fatto.\n"
    "Scrivi ANNULLA per ricominciare da capo.\n"
    "Per un'emergenza contatta direttamente la centrale."
)
CANCELLED = "Richiesta annullata. Scrivi un messaggio quando vuoi ricominciare."
EXPIRED = "La conversazione precedente è scaduta, ricominciamo."
TOO_MANY_ATTEMPTS = "Non riesco a capire. Contatta la centrale operativa per ricevere assistenza."
LOCKED = "Troppi tentativi di verifica. Riprova più tardi o contatta la centrale operativa."
RATE_LIMITED = "Troppe richieste in questo momento. Riprova tra qualche minuto."
UNAVAILABLE = "Il servizio non è disponibile in questo momento. Riprova tra poco."
GOODBYE = "Grazie, a presto. Scrivi un messaggio se ti serve altro."

ASK_PLATE = "Scrivi la targa del veicolo (esempio: AB 123 CD)."
INVALID_PLATE = "La targa non sembra valida. Scrivila con sole lettere e numeri."
ASK_CHALLENGE = "Per verificare che il veicolo sia tuo, scrivi il numero di polizza."
VERIFICATION_FAILED = "Non è stato possibile verificare i dati inseriti. Ricontrolla targa e polizza."
REVERIFY = "Per sicurezza devo verificare di nuovo i tuoi dati."
VERIFIED = "Verifica completata, {nome}."
VERIFIED_NO_NAME = "Verifica completata."

ASK_LOCATION = "Dove ti trovi? Scrivi l'indirizzo oppure condividi la posizione."
INVALID_LOCATION = "Non ho capito la posizione. Scrivi via e città oppure condividi la posizione."
SHARED_LOCATION = "Posizione condivisa ({lat:.6f}, {lng:.6f})"
ASK_PROBLEM = "Descrivi brevemente il problema."
INVALID_PROBLEM = "Descrivi il problema con qualche parola in più."
CONFIRM = (
    "Riepilogo:\n"
    "Posizione: {location}\n"
    "Problema: {problem}\n"
    "Rispondi SI per inviare la richiesta, MODIFICA per correggerla, ANNULLA per annullarla."
)
INVALID_CONFIRM = "Rispondi SI, MODIFICA oppure ANNULLA."
CREATED = (
    "Richiesta presa in carico, codice {codice}. La centrale sta cercando un autista.\n"
    "Scrivi STATO per sapere a che punto è, oppure FINE per chiudere."
)
CREATE_CONFLICT = "Non posso registrare la richiesta. Contatta la centrale operativa."
CREATE_REJECTED = "La richiesta non è stata accettata dal sistema. Contatta la centrale operativa."

NO_MISSION = "Non trovo richieste di soccorso aperte da questa chat. Contatta la centrale operativa."
AFTER_STATUS = "Scrivi STATO per aggiornare, oppure FINE per chiudere."
AFTER_CREATE_INVALID = "Scrivi STATO per sapere a che punto è il soccorso, oppure FINE per chiudere."

STATUS_TEXT = {
    "nuova": "La richiesta {codice} è stata ricevuta ed è in attesa di assegnazione.",
    "proposta": "Stiamo assegnando la richiesta {codice} a un autista.",
    "non_accettata": "Stiamo assegnando la richiesta {codice} a un altro autista.",
    "scaduta": "Stiamo assegnando la richiesta {codice} a un altro autista.",
    "accettata": "La richiesta {codice} è stata presa in carico.",
    "eseguita": "L'intervento per la richiesta {codice} è concluso.",
}
STATUS_UNKNOWN = "La richiesta {codice} è in lavorazione."
DRIVER_ASSIGNED = " Autista assegnato: {autista}."
DRIVER_DONE = " Autista: {autista}."
