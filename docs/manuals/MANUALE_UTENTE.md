# Manuale utente

**Codice:** FOR-USER-001 · **Versione:** 1.0 · **Data:** 03/11/2026
**Destinatari:** centrale, autista e cliente · **Stato:** manuale limitato alle funzioni esistenti

## Prima di iniziare

Non esiste un'interfaccia web di centrale o autista nel repository. Le sezioni
centrale/autista descrivono le operazioni previste e **non sono istruzioni per una
schermata disponibile**. Il solo percorso cliente oggi documentato è il chatbot
Python/Telegram; usarlo richiede un bot configurato e autorizzato dal team.

## Cliente — richiesta via Telegram

**Prerequisiti:** il bot Telegram deve essere stato predisposto e avviato dal team;
non condividere in chat dati non necessari. La prova Telegram è dichiarata in
[`chatbot/README.md`](../../chatbot/README.md); disponibilità del servizio non è
garantita dalla CI.

1. Aprire il bot corretto e premere **Avvia**.
2. Inserire la targa quando il dialogo la richiede.
3. Seguire le richieste di posizione/indirizzo e descrizione del problema.
4. Usare `AIUTO` per indicazioni, `ANNULLA` per uscire e `STATO` per consultare lo
   stato associato alla chat, se presente.
5. Se il servizio segnala errore o indisponibilità, non reinviare molte volte;
   attendere o contattare la centrale secondo le indicazioni ricevute.

**Importante:** il controllo della targa non prova che chi scrive sia il proprietario
del veicolo. Le conversazioni possono essere perse al riavvio del bot; il comportamento
non va usato come servizio di emergenza o produzione.

## Responsabile della centrale

**Funzione prevista — interfaccia web non ancora disponibile.** Il backend dispone
di API per sessione e anagrafiche; missioni, dashboard, rettifiche e storico hanno
API runtime ma non una UI. Non fornire istruzioni click-by-click o credenziali demo.
Operazioni consentite secondo contratto:

- login/logout e consultazione identità;
- consultare/ricercare anagrafiche e inserire cliente;
- gestire missioni e relative proposte tramite client autorizzato;
- consultare dashboard e storico secondo ruolo;
- autorizzare rettifica motivata solo con ruolo centrale.

Vedi [OpenAPI](../../api/openapi.yaml); non utilizzare endpoint `designed`.

## Autista

**Funzione prevista — interfaccia mobile/web non disponibile.** Il backend espone
endpoint autista per missioni assegnate, risposta, completamento e storico. Le
autorizzazioni impediscono l'accesso a missioni non assegnate secondo il service.
Per ora non esiste una guida schermata né un'app client; i test API non sono un
manuale di uso quotidiano.

## Errori e sicurezza

Non condividere password o token. `401` indica sessione/token non valido,
`403` permessi insufficienti; errori `400/409/429` vanno risolti seguendo il
messaggio, senza ripetere una mutazione alla cieca. Mappa e WhatsApp sono
**funzioni previste — non ancora disponibili**.
