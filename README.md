# I miei SMS

App SMS personale in Kotlin, senza librerie esterne. Nessun MMS in questa versione: solo testo.

## Cosa fa (versione 0.1)

- Elenco conversazioni con nome del contatto (se in rubrica), anteprima e orario.
- Apertura di una conversazione con invio, ricezione in tempo reale e stato del messaggio (in corso / inviato / non riuscito, con possibilità di ritentare toccandolo).
- Messaggi lunghi divisi automaticamente in più SMS.
- Notifica per ogni messaggio ricevuto, che apre direttamente la conversazione.
- Avviso (senza contenuto) quando arriva un MMS, dato che questa versione non lo sa mostrare.
- Pulsante per diventare l'app SMS predefinita direttamente dalla schermata principale.
- Rispetta il requisito Android di "rispondi con un messaggio" durante una chiamata in arrivo (componente tecnico obbligatorio per essere selezionabili come app predefinita, richiesto dal sistema — non è pensato per essere usato direttamente).

## Un avviso importante

Gli SMS sono spesso il canale dei codici di verifica (banche, 2FA). Finché non hai provato questa app per qualche giorno e ti fidi del suo funzionamento, ti consiglio di **disattivare** l'app SMS di sistema (es. Google Messaggi) invece di disinstallarla, così puoi tornare indietro in un attimo se serve:
```
adb shell pm disable-user --user 0 com.google.android.inputmethod.latin
```
(sostituisci con il nome del pacchetto della tua app messaggi, come abbiamo verificato insieme per Gboard)

## Installarla

Stessa procedura del progetto della tastiera:
1. Apri la cartella in Android Studio e premi Run, **oppure**
2. Carica tutto (compresa `.github`) su un repository GitHub privato: la compilazione parte da sola in Actions e trovi l'APK da scaricare in fondo alla pagina dell'esecuzione, sotto "Artifacts".

Dopo l'installazione, apri l'app e tocca il banner in alto per impostarla come app SMS predefinita.

## Dove mettere le mani

| Cosa vuoi cambiare | File |
|---|---|
| Come appare l'elenco conversazioni | `MainActivity.kt` |
| Come appare una conversazione (colori bolle, ecc.) | `ConversazioneActivity.kt` |
| Cosa succede quando arriva un SMS | `SmsDeliverReceiver.kt` |
| Notifiche | `Notifiche.kt` |
| Come vengono inviati i messaggi | `InvioSms.kt` |
| Permessi richiesti | `Permessi.kt` |

## Possibili sviluppi futuri

- Supporto MMS (foto/video) — la parte più complessa, richiede parsing PDU e impostazioni APN dell'operatore.
- Tema colori personalizzabile, come nella tastiera.
- Eliminazione conversazioni, ricerca, blocco numeri.
