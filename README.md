<div align="center">

<img src="assets/banner.svg" alt="SMS.EXE - I miei SMS" width="800">

# 💾 I miei SMS

**App SMS personale per Android — senza internet, senza librerie, senza tracker.**

🇮🇹 Italiano · [🇬🇧 English](README.en.md)

![versione](https://img.shields.io/badge/versione-3.6.6-000080?style=flat-square&labelColor=c0c0c0)
![android](https://img.shields.io/badge/Android-8.0%2B-008080?style=flat-square&labelColor=c0c0c0)
![kotlin](https://img.shields.io/badge/Kotlin-zero%20librerie-800080?style=flat-square&labelColor=c0c0c0)
![licenza](https://img.shields.io/badge/licenza-PolyForm%20Noncommercial-808080?style=flat-square&labelColor=c0c0c0)
![internet](https://img.shields.io/badge/permesso%20INTERNET-no-c00000?style=flat-square&labelColor=c0c0c0)

<img src="assets/progress.svg" alt="Verifica privacy" width="640">

</div>

---

## 🖥️ Cos'è

`I miei SMS` sostituisce l'app messaggi di sistema con una scritta da zero in Kotlin, con **solo le API di Android**.
Non ha il permesso `INTERNET`: l'app **non può** mandare nulla fuori dal telefono.

```
┌─ Informazioni su I miei SMS ──────────────── _ □ X ┐
│                                                    │
│   💬  I miei SMS   versione 3.6.6                  │
│                                                    │
│   Memoria libera ........ tutta                    │
│   Cloud ................. nessuno                  │
│   Tracker ............... 0                        │
│                                                    │
│              [   OK   ]   [ Annulla ]              │
└────────────────────────────────────────────────────┘
```

## ✨ Cosa fa

| | Funzione |
|---|---|
| 💬 | Elenco conversazioni con nome del contatto, anteprima e orario |
| 📨 | Invio e ricezione in tempo reale; messaggi lunghi divisi in automatico |
| ✅ | Stato di ogni messaggio: in corso, inviato, **consegnato**, non riuscito (tocca per riprovare, senza duplicati) |
| 🔐 | **Codici di verifica** riconosciuti: pulsante «Copia codice» nella notifica |
| 🔔 | Notifiche con **Rispondi** e **Letto** direttamente dalla notifica |
| 🔍 | Ricerca per testo, nome o numero |
| 🗑️ | Elimina conversazioni e singoli messaggi · copia il testo con un tocco lungo |
| 🚫 | Blocca numeri (vedi «Privacy» qui sotto) |
| 🎨 | Temi colore, anche in stile vintage con bordi |
| ⚡ | Elenco veloce e caricamento a pagine (200 messaggi alla volta) |
| 🧹 | Scarta gli SMS doppi dell'operatore e segna come falliti gli invii rimasti a metà |

> 📵 **Gli MMS non sono supportati.** Quando ne arriva uno ricevi un avviso (senza contenuto).

## 🔒 Privacy

- **Nessun accesso a internet**: il permesso `INTERNET` non c'è nel manifest.
- **Nessun backup** dei dati (`allowBackup="false"`).
- **Notifiche private sul blocco schermo**: mittente, testo e codici si vedono solo dopo lo sblocco.
- **Codice copiato marcato come sensibile** negli appunti (Android 13+).
- **Numeri bloccati non salvati**: resta solo un'impronta (PBKDF2-SHA256, sale casuale). L'elenco «Numeri bloccati» si ricostruisce dalle conversazioni presenti negli SMS. L'impronta rallenta chi volesse risalire ai numeri, ma non è una protezione assoluta.
- **APK release** (non debuggable), firmato con una chiave che non sta nel repository.
- Gli SMS restano nell'archivio di sistema di Android: l'app non li copia altrove.

## 📥 Installazione

1. Vai in [**Releases**](../../releases) e scarica l'ultimo `mysms-*.apk`.
2. Se hai una versione **precedente alla 3.6.6**, disinstallala prima (la chiave di firma è cambiata). Gli SMS restano, perché stanno nel sistema.
3. Apri l'APK e consenti l'installazione da fonti sconosciute.
4. Avvia l'app, concedi i permessi e tocca il banner in alto per impostarla come **app SMS predefinita**.

> ⚠️ Molte banche e servizi mandano i codici di accesso via SMS. Prima di fidarti dell'app, **disattiva** (non disinstallare) l'app messaggi di sistema, così puoi tornare indietro in un attimo:
> ```
> adb shell pm list packages | grep -i messag
> adb shell pm disable-user --user 0 <nome.del.pacchetto>
> ```

## 🛠️ Compilare

```bash
# In locale: serve Android Studio (JDK 17) oppure Gradle 8.11
gradle testDebugUnitTest assembleRelease
```

Con GitHub Actions la compilazione parte a ogni push. Per firmare con la **tua** chiave aggiungi tre *secret* al repository:

| Secret | Contenuto |
|---|---|
| `FIRMA_BASE64` | il keystore `.jks` in base64 (`base64 -w0 firma.jks`) |
| `FIRMA_PASSWORD` | la password del keystore e della chiave |
| `FIRMA_ALIAS` | l'alias della chiave |

Senza secret la build usa la chiave di debug standard di Android.
Per pubblicare una versione basta un tag: `git tag v3.6.6 && git push origin v3.6.6` — Actions compila e crea la release con l'APK.

## 🗂️ Dove mettere le mani

| Cosa vuoi cambiare | File |
|---|---|
| Elenco conversazioni, menu, ricerca | `MainActivity.kt` |
| Una conversazione (bolle, azioni sui messaggi) | `ConversazioneActivity.kt` |
| Cosa succede quando arriva un SMS | `SmsDeliverReceiver.kt` |
| Notifiche e loro pulsanti | `Notifiche.kt`, `AzioniNotificaReceiver.kt` |
| Invio, stato e consegna | `InvioSms.kt`, `StatoInvioReceiver.kt`, `StatoConsegnaReceiver.kt` |
| Riconoscimento codici / confronto numeri (con test) | `Codici.kt`, `Numeri.kt` |
| Temi e impostazioni, blocco numeri | `Temi.kt` |

## 🧪 Test

I test (`app/src/test`) coprono il riconoscimento dei codici di verifica e il confronto dei numeri. JUnit è usato **solo** per i test e non finisce nell'APK.
Le parti che parlano con il telefono (invio, ricezione, notifiche) non sono coperte da test automatici e vanno provate su un dispositivo.

## 📜 Licenza

[**PolyForm Noncommercial 1.0.0**](LICENSE) — codice sorgente aperto alla lettura, **non** open source nel senso OSI.

| | |
|---|---|
| ✅ | Usarla, anche tutti i giorni, per uso personale, studio, ricerca, hobby |
| ✅ | **Fare un fork**, modificarla e condividere le tue versioni |
| ✅ | Distribuire copie e derivati a chi vuoi, purché gratuitamente e con questa licenza (e il file `NOTICE`) |
| ❌ | **Venderla o usarla a scopo commerciale**, lei o qualsiasi suo derivato |

Per un uso commerciale serve un accordo con l'autore. Il testo di legge è quello del file `LICENSE`; questa tabella è solo un riassunto.

## 🗺️ Possibili sviluppi

- Supporto MMS (foto/video): la parte più complessa, richiede parsing PDU e impostazioni APN.
- Passare a `RecyclerView` (aggiungerebbe una libreria).
- Opzione per mostrare il testo anche sul blocco schermo.

<div align="center">

<sub>💾 Fatto per essere piccolo, onesto e solo tuo.</sub>

</div>
