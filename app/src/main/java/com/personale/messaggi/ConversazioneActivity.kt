package com.personale.messaggi

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class ConversazioneActivity : Activity() {

    companion object {
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_NUMERO = "numero"
    }

    private lateinit var tema: Tema
    private var threadId: Long = -1
    private lateinit var numero: String
    private lateinit var elencoView: ListView
    private lateinit var adapter: AdapterMessaggi
    private lateinit var campoTesto: EditText
    private var messaggi: List<Messaggio> = emptyList()
    private var limite = Messaggi.PAGINA
    private var caricandoPrecedenti = false
    private lateinit var caricaAltri: TextView

    private val gestore = Handler(Looper.getMainLooper())
    private val osservatore = object : ContentObserver(gestore) {
        override fun onChange(selfChange: Boolean) = ricarica()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tema = Temi.perId(Preferenze(this).tema)
        threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
        numero = intent.getStringExtra(EXTRA_NUMERO) ?: ""
        title = PhoneNumberUtils.formatNumber(numero, Locale.getDefault().country) ?: numero
        Contatti.cerca(this, numero)?.let { title = it.nome }

        val radice = LinearLayout(this)
        radice.orientation = LinearLayout.VERTICAL
        radice.setBackgroundColor(tema.sfondo)

        // I messaggi vivono in un pannello "finestra" bordato, come nella schermata principale.
        elencoView = ListView(this)
        elencoView.divider = null

        // In cima alla lista: appare solo se ci sono messaggi più vecchi di quelli caricati.
        caricaAltri = TextView(this)
        caricaAltri.text = "Carica messaggi precedenti"
        caricaAltri.gravity = Gravity.CENTER
        caricaAltri.setTextColor(tema.accento)
        caricaAltri.setPadding(dp(12), dp(12), dp(12), dp(12))
        caricaAltri.visibility = View.GONE
        caricaAltri.setOnClickListener {
            limite += Messaggi.PAGINA
            caricandoPrecedenti = true
            ricarica()
        }
        val intestazione = LinearLayout(this)
        intestazione.addView(caricaAltri, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        elencoView.addHeaderView(intestazione, null, false)
        adapter = AdapterMessaggi()
        elencoView.adapter = adapter

        val pannelloLista = LinearLayout(this)
        pannelloLista.background = pannelloConBordo(tema.sfondo, tema.bordo)
        pannelloLista.setPadding(dp(2), dp(2), dp(2), dp(2))
        pannelloLista.addView(elencoView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
        val parametriPannello = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        parametriPannello.setMargins(dp(8), dp(8), dp(8), dp(8))
        radice.addView(pannelloLista, parametriPannello)

        val separatore = View(this)
        separatore.setBackgroundColor(tema.divisore)
        radice.addView(separatore, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))

        val rigaInvio = LinearLayout(this)
        rigaInvio.orientation = LinearLayout.HORIZONTAL
        rigaInvio.setPadding(dp(8), dp(8), dp(8), dp(8))
        rigaInvio.setBackgroundColor(tema.superficie)

        campoTesto = EditText(this)
        campoTesto.hint = "Scrivi un messaggio"
        campoTesto.setTextColor(tema.testo)
        campoTesto.setHintTextColor(tema.testoSecondario)
        rigaInvio.addView(campoTesto, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val invia = TextView(this)
        invia.text = "Invia"
        invia.setTextColor(tema.accento)
        invia.gravity = Gravity.CENTER
        invia.setPadding(dp(16), 0, dp(16), 0)
        invia.background = pannelloConBordo(tema.sfondo, tema.bordo)
        invia.setOnClickListener { inviaMessaggio() }
        val parametriInvia = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
        parametriInvia.setMargins(dp(6), 0, 0, 0)
        rigaInvio.addView(invia, parametriInvia)

        if (Numeri.alfanumerico(numero)) {
            // Mittenti come "POSTE" o "AMAZON" non accettano risposte.
            val avviso = TextView(this)
            avviso.text = "Non è possibile rispondere a questo mittente."
            avviso.setTextColor(tema.testoSecondario)
            avviso.gravity = Gravity.CENTER
            avviso.setPadding(dp(8), dp(14), dp(8), dp(14))
            avviso.setBackgroundColor(tema.superficie)
            radice.addView(avviso, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        } else {
            radice.addView(rigaInvio, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        setContentView(radice)
    }

    override fun onResume() {
        super.onResume()
        contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, osservatore)
        ricarica()
    }

    override fun onPause() {
        super.onPause()
        contentResolver.unregisterContentObserver(osservatore)
    }

    private fun ricarica() {
        if (threadId <= 0) {
            threadId = Messaggi.threadIdPerNumero(this, numero) ?: -1
        }
        val prima = messaggi.size
        if (threadId > 0) {
            messaggi = Messaggi.diConversazione(this, threadId, limite)
            Messaggi.segnaComeLette(this, threadId)
        }
        caricaAltri.visibility = if (messaggi.size >= limite) View.VISIBLE else View.GONE
        adapter.notifyDataSetChanged()
        if (caricandoPrecedenti) {
            // Resta nel punto in cui eravamo: i messaggi nuovi in cima spostano tutto in basso (+1 per l'intestazione).
            elencoView.setSelection(messaggi.size - prima + 1)
            caricandoPrecedenti = false
        } else if (messaggi.isNotEmpty()) {
            elencoView.setSelection(messaggi.size) // ultima riga (l'intestazione occupa la posizione 0)
        }
    }

    // ---------- Menu ----------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Blocca numero")
        menu.add(0, 2, 1, "Elimina conversazione")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> AlertDialog.Builder(this)
                .setTitle("Bloccare questo numero?")
                .setMessage("I suoi messaggi verranno conservati ma senza notifiche, e la conversazione sparirà dall'elenco. L'app non salva il numero, solo un'impronta. Puoi sbloccarlo da «Numeri bloccati».")
                .setPositiveButton("Blocca") { _, _ ->
                    Preferenze(this).blocca(numero)
                    finish()
                }
                .setNegativeButton("Annulla", null)
                .show()
            2 -> AlertDialog.Builder(this)
                .setTitle("Eliminare la conversazione?")
                .setMessage("Non si può annullare.")
                .setPositiveButton("Elimina") { _, _ ->
                    if (threadId > 0 && Messaggi.eliminaConversazione(this, threadId)) {
                        finish()
                    } else {
                        Toast.makeText(this, "Per eliminare l'app deve essere quella predefinita per gli SMS.", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Annulla", null)
                .show()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun mostraAzioniMessaggio(m: Messaggio) {
        AlertDialog.Builder(this)
            .setItems(arrayOf("Copia testo", "Elimina messaggio")) { _, quale ->
                if (quale == 0) {
                    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Messaggio", m.corpo))
                    Toast.makeText(this, "Copiato", Toast.LENGTH_SHORT).show()
                } else if (Messaggi.eliminaMessaggio(this, m.id)) {
                    ricarica()
                } else {
                    Toast.makeText(this, "Per eliminare l'app deve essere quella predefinita per gli SMS.", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    private fun inviaMessaggio() {
        val testo = campoTesto.text.toString().trim()
        if (testo.isEmpty() || numero.isEmpty()) return
        InvioSms.invia(this, numero, testo)
        campoTesto.setText("")
        ricarica()
    }

    /** Riempimento pieno, con bordo attorno solo se il tema lo prevede (i temi vintage). */
    private fun pannelloConBordo(riempimento: Int, bordoColore: Int): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(riempimento)
        d.cornerRadius = 0f
        if (bordoColore != 0) d.setStroke(dp(2), bordoColore)
        return d
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dp(v: Float) = (v * resources.displayMetrics.density)

    // ---------- Adapter ----------

    private inner class AdapterMessaggi : ArrayAdapter<Messaggio>(this@ConversazioneActivity, 0) {
        override fun getCount() = messaggi.size
        override fun getItem(posizione: Int) = messaggi[posizione]

        override fun getView(posizione: Int, convertView: View?, parent: ViewGroup): View {
            val m = messaggi[posizione]
            val esterno = LinearLayout(this@ConversazioneActivity)
            esterno.orientation = LinearLayout.HORIZONTAL
            esterno.setPadding(dp(12), dp(4), dp(12), dp(4))

            val bolla = LinearLayout(this@ConversazioneActivity)
            bolla.orientation = LinearLayout.VERTICAL
            bolla.setPadding(dp(14), dp(8), dp(14), dp(8))

            val sfondoBolla = GradientDrawable()
            sfondoBolla.cornerRadius = dp(tema.angoloBolla)
            sfondoBolla.setColor(if (m.inviatoDaMe) tema.accento else tema.superficie)
            if (tema.bordo != 0) sfondoBolla.setStroke(dp(1f).toInt(), tema.bordo)
            bolla.background = sfondoBolla

            val testoColore = if (m.inviatoDaMe) tema.testoSuAccento else tema.testo
            val testo = TextView(this@ConversazioneActivity)
            testo.text = m.corpo
            testo.textSize = 15f
            testo.setTextColor(testoColore)
            bolla.addView(testo)

            if (m.inviatoDaMe && m.stato != StatoMessaggio.INVIATO) {
                val stato = TextView(this@ConversazioneActivity)
                stato.text = when (m.stato) {
                    StatoMessaggio.IN_CORSO -> "Invio in corso…"
                    StatoMessaggio.FALLITO -> "Non inviato, tocca per riprovare"
                    else -> ""
                }
                stato.textSize = 11f
                stato.setTextColor(if (m.stato == StatoMessaggio.FALLITO) Color.parseColor("#FFCDD2") else Color.parseColor("#D6E4FB"))
                bolla.addView(stato)
                if (m.stato == StatoMessaggio.FALLITO) {
                    bolla.setOnClickListener {
                        InvioSms.riprova(this@ConversazioneActivity, m.id, numero, m.corpo)
                        ricarica()
                    }
                }
            }

            // "Consegnato" solo sull'ultimo messaggio inviato, per non riempire la schermata.
            if (m.inviatoDaMe && m.consegnato && m === messaggi.lastOrNull { it.inviatoDaMe }) {
                val consegnato = TextView(this@ConversazioneActivity)
                consegnato.text = "Consegnato"
                consegnato.textSize = 11f
                consegnato.setTextColor(Color.parseColor("#D6E4FB"))
                bolla.addView(consegnato)
            }
            bolla.setOnLongClickListener {
                mostraAzioniMessaggio(m)
                true
            }

            val parametriBolla = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            esterno.gravity = if (m.inviatoDaMe) Gravity.END else Gravity.START
            esterno.addView(bolla, parametriBolla)

            return esterno
        }
    }
}
