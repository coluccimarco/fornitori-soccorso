# BUR — Business User Requirements FORNITORI

**Versione:** 1.0 · **Data:** 03/11/2026 · **Responsabile:** Team Lead / referente corso<br>
**Indice:** scopo · attori · requisiti · vincoli · acceptance

## Scopo

Riepiloga i bisogni utente per allineare i cinque componenti. È una baseline di
lavoro costruita dalla traccia condivisa; le decisioni in conflitto con PDF/Excel
richiedono conferma del docente.

## Attori e obiettivi

- Responsabile centrale: gestisce anagrafiche, missioni, assegnazioni, dashboard e
  rettifiche autorizzate.
- Autista: consulta solo attività assegnate, accetta/rifiuta e registra chiusura.
- Cliente: richiede assistenza e consulta solo la propria pratica dopo verifica.
- Operatore chatbot/notifiche: guida richiesta, invia aggiornamenti e gestisce provider.

## Requisiti business

1. Memorizzare utenti, autisti, mezzi, assicurazioni, clienti, officine e missioni.
2. Preservare il dataset storico Excel e riconciliare il totale 16 missioni OK /
   €1.509,71.
3. Autenticare ruoli e impedire accesso a dati non pertinenti.
4. Creare missione, proporre a un autista libero con timeout 10 minuti, gestire
   risposta e riproposizione senza perdere storico.
5. Chiudere missione con km effettivi, tariffa e dashboard coerenti.
6. Consentire richiesta chatbot verificata, indicazioni mappa e notifiche affidabili.
7. Mantenere audit per cambi di stato/costo e operazioni sensibili.

## Vincoli e acceptance

Regola economica target: `(50 + 1,20 × km) × coefficiente notte`; 1,25 tra 20:00 e
08:00. Excel riferisce l'ora di accettazione; istante esatto è da confermare.
Requisiti sono soddisfatti solo con test e demo, non con una schermata statica.
La baseline è in [business rules](../05_BUSINESS_RULES.md) e
[evidenze](../TEST_EVIDENCE.md).

**Dipendenze:** specifica ufficiale e approvazione docente.<br>
**Completamento:** ogni requisito mappato a story, endpoint, test e demo.
**Riferimenti:** [FUD](./FUD_FORNITORI.md), [API](../04_API_REST.md).
