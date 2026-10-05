package com.personale.messaggi

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.database.Cursor
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.text.InputType
import android.text.format.DateUtils
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Preferenze
    private lateinit var tema: Tema
    private lateinit var elencoView: ListView
    private lateinit var bannerPredefinita: TextView
    private lateinit var adapter: AdapterConversazioni
    private lateinit var bannerFiltro: TextView
    private var conversazioni: List<Conversazione> = emptyList()
    private var filtro: String? = null

    private val gestore = Handler(Looper.getMainLooper())
    private val aggiorna = Runnable { if (Permessi.tuttiConcessi(this)) aggiornaElenco() }
    private val osservatore = object : ContentObserver(gestore) {
        override fun onChange(selfChange: Boolean) {
            // Più modifiche ravvicinate (es. un messaggio con più parti) → un solo aggiornamento.
            gestore.removeCallbacks(aggiorna)
            gestore.postDelayed(aggiorna, 400)
        }
    }

    private companion object {
        const val RICHIESTA_RUOLO_SMS = 900
        const val RICHIESTA_CONTATTO = 901
        const val VOCE_MENU_TEMA = 1
        const val VOCE_MENU_CERCA = 2
        const val VOCE_MENU_BLOCCATI = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)

        prefs = Preferenze(this)
        tema = Temi.perId(prefs.tema)

        val radice = LinearLayout(this)
        radice.orientation = LinearLayout.VERTICAL
        radice.setBackgroundColor(tema.sfondo)

        bannerPredefinita = TextView(this)
        bannerPredefinita.textSize = 15f
        bannerPredefinita.setPadding(dp(20), dp(14), dp(20), dp(14))
        bannerPredefinita.setBackgroundColor(tema.bannerSfondo)
        bannerPredefinita.setTextColor(tema.bannerTesto)
        bannerPredefinita.setOnClickListener { chiediDiDiventarePredefinita() }
        radice.addView(bannerPredefinita, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val separatore = View(this)
        separatore.setBackgroundColor(tema.divisore)
        radice.addView(separatore, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))

        bannerFiltro = TextView(this)
        bannerFiltro.textSize = 14f
        bannerFiltro.setPadding(dp(20), dp(10), dp(20), dp(10))
        bannerFiltro.setBackgroundColor(tema.superficie)
        bannerFiltro.setTextColor(tema.accento)
        bannerFiltro.visibility = View.GONE
        bannerFiltro.setOnClickListener { impostaFiltro(null) }
        radice.addView(bannerFiltro, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // La lista vive dentro un "pannello finestra" bordato che si stacca dal desktop sottostante.
        elencoView = ListView(this)
        elencoView.divider = ColorDrawable(tema.divisore)
        elencoView.dividerHeight = dp(1)
        adapter = AdapterConversazioni()
        elencoView.adapter = adapter
        elencoView.setOnItemClickListener { _, _, posizione, _ -> apriConversazione(conversazioni[posizione]) }
        elencoView.setOnItemLongClickListener { _, _, posizione, _ ->
            chiediEliminaConversazione(conversazioni[posizione])
            true
        }

        val pannelloLista = LinearLayout(this)
        pannelloLista.background = pannelloConBordo(tema.superficie, tema.bordo)
        pannelloLista.setPadding(dp(2), dp(2), dp(2), dp(2))
        pannelloLista.addView(elencoView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
        val parametriPannello = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        parametriPannello.setMargins(dp(8), dp(8), dp(8), dp(8))
        radice.addView(pannelloLista, parametriPannello)

        val nuovo = TextView(this)
        nuovo.text = "+  Nuovo messaggio"
        nuovo.textSize = 16f
        nuovo.gravity = Gravity.CENTER
        nuovo.setPadding(0, dp(14), 0, dp(14))
        nuovo.background = pannelloConBordo(tema.superficie, tema.bordo)
        nuovo.setTextColor(tema.accento)
        nuovo.setOnClickListener { nuovoMessaggio() }
        val parametriNuovo = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        parametriNuovo.setMargins(dp(8), 0, dp(8), dp(8))
        radice.addView(nuovo, parametriNuovo)

        setContentView(radice)
        Bordi.applica(radice)

        if (!Permessi.tuttiConcessi(this)) Permessi.richiedi(this)
    }

    override fun onResume() {
        super.onResume()
        aggiornaBannerPredefinita()
        if (Permessi.tuttiConcessi(this)) {
            aggiornaElenco()
            contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, osservatore)
        }
    }

    override fun onPause() {
        super.onPause()
        contentResolver.unregisterContentObserver(osservatore)
        gestore.removeCallbacks(aggiorna)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Permessi.CODICE_RICHIESTA) {
            if (Permessi.tuttiConcessi(this)) {
                aggiornaElenco()
            } else {
                Toast.makeText(this, "Senza questi permessi l'app non può leggere o inviare SMS.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ---------- Menu e temi ----------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, VOCE_MENU_CERCA, 0, "Cerca")
        menu.add(0, VOCE_MENU_BLOCCATI, 1, "Numeri bloccati")
        menu.add(0, VOCE_MENU_TEMA, 2, "Tema")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            VOCE_MENU_TEMA -> mostraSceltaTema()
            VOCE_MENU_CERCA -> chiediTestoDaCercare()
            VOCE_MENU_BLOCCATI -> mostraNumeriBloccati()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun chiediTestoDaCercare() {
        val campo = EditText(this)
        campo.hint = "Testo, nome o numero"
        campo.setSingleLine()
        AlertDialog.Builder(this)
            .setTitle("Cerca")
            .setView(campo)
            .setPositiveButton("Cerca") { _, _ -> impostaFiltro(campo.text.toString().trim().ifEmpty { null }) }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun impostaFiltro(testo: String?) {
        filtro = testo
        bannerFiltro.visibility = if (testo == null) View.GONE else View.VISIBLE
        bannerFiltro.text = "Risultati per «$testo» — tocca per annullare"
        aggiornaElenco()
    }

    /**
     * L'app non conserva i numeri bloccati, solo un'impronta. L'elenco si ricostruisce
     * dalle conversazioni presenti negli SMS: si vedono quelle il cui mittente risulta bloccato.
     */
    private fun mostraNumeriBloccati() {
        Thread {
            val bloccate = Conversazioni.elenco(this).filter { prefs.bloccato(it.numero) }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                if (bloccate.isEmpty()) {
                    Toast.makeText(this, "Nessuna conversazione bloccata.", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                AlertDialog.Builder(this)
                    .setTitle("Tocca un mittente per sbloccarlo")
                    .setItems(bloccate.map { it.nome ?: formattaNumero(it.numero) }.toTypedArray()) { _, quale ->
                        prefs.sblocca(bloccate[quale].numero)
                        aggiornaElenco()
                    }
                    .setNegativeButton("Chiudi", null)
                    .show()
            }
        }.start()
    }

    private fun chiediEliminaConversazione(c: Conversazione) {
        AlertDialog.Builder(this)
            .setTitle("Eliminare la conversazione?")
            .setMessage("Con ${c.nome ?: formattaNumero(c.numero)}. Non si può annullare.")
            .setPositiveButton("Elimina") { _, _ ->
                if (Messaggi.eliminaConversazione(this, c.threadId)) {
                    aggiornaElenco()
                } else {
                    Toast.makeText(this, "Per eliminare l'app deve essere quella predefinita per gli SMS.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun mostraSceltaTema() {
        val nomi = Temi.tutti.map { it.nome }.toTypedArray()
        val indiceAttuale = Temi.tutti.indexOfFirst { it.id == prefs.tema }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Scegli un tema")
            .setSingleChoiceItems(nomi, indiceAttuale) { dialog, quale ->
                prefs.tema = Temi.tutti[quale].id
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // ---------- Elenco ----------

    private fun aggiornaElenco() {
        Contatti.svuotaCache() // la rubrica può essere cambiata dall'ultima volta
        val testoFiltro = filtro
        Thread {
            var nuovo = Conversazioni.elenco(this) { prefs.bloccato(it) }
            if (testoFiltro != null) {
                val perTesto = Messaggi.threadConTesto(this, testoFiltro)
                nuovo = nuovo.filter {
                    it.threadId in perTesto ||
                        (it.nome?.contains(testoFiltro, ignoreCase = true) == true) ||
                        it.numero.contains(testoFiltro, ignoreCase = true)
                }
            }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                conversazioni = nuovo
                adapter.notifyDataSetChanged()
            }
        }.start()
    }

    private fun apriConversazione(c: Conversazione) {
        val apri = Intent(this, ConversazioneActivity::class.java).apply {
            putExtra(ConversazioneActivity.EXTRA_THREAD_ID, c.threadId)
            putExtra(ConversazioneActivity.EXTRA_NUMERO, c.numero)
        }
        startActivity(apri)
    }

    private fun nuovoMessaggio() {
        AlertDialog.Builder(this)
            .setTitle("Nuovo messaggio")
            .setItems(arrayOf("Scegli dalla rubrica", "Scrivi un numero")) { _, quale ->
                if (quale == 0) aprirubrica() else chiediNumeroPerNuovoMessaggio()
            }
            .show()
    }

    private fun aprirubrica() {
        // Il selettore di sistema: nessun permesso in più richiesto, e mostra già solo i contatti con un numero.
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        try {
            startActivityForResult(intent, RICHIESTA_CONTATTO)
        } catch (e: Exception) {
            Toast.makeText(this, "Nessuna app rubrica trovata sul telefono.", Toast.LENGTH_LONG).show()
        }
    }

    private fun chiediNumeroPerNuovoMessaggio() {
        val campo = EditText(this)
        campo.hint = "Numero di telefono"
        campo.inputType = InputType.TYPE_CLASS_PHONE
        val contenitore = LinearLayout(this)
        contenitore.setPadding(dp(20), dp(8), dp(20), 0)
        contenitore.addView(campo)

        AlertDialog.Builder(this)
            .setTitle("Nuovo messaggio")
            .setView(contenitore)
            .setPositiveButton("Avanti") { _, _ ->
                val numero = campo.text.toString().trim()
                if (numero.isNotEmpty()) apriConversazioneConNumero(numero)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun apriConversazioneConNumero(numero: String) {
        val apri = Intent(this, ConversazioneActivity::class.java)
        apri.putExtra(ConversazioneActivity.EXTRA_NUMERO, numero)
        startActivity(apri)
    }

    // ---------- App SMS predefinita ----------

    private fun aggiornaBannerPredefinita() {
        // Dalle versioni recenti di Android il valore "sms_default_application" può essere vuoto
        // anche quando siamo l'app predefinita: la fonte affidabile è il ruolo SMS.
        val predefinita = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            Telephony.Sms.getDefaultSmsPackage(this) == packageName
        }
        if (predefinita) {
            bannerPredefinita.visibility = View.GONE
        } else {
            bannerPredefinita.visibility = View.VISIBLE
            bannerPredefinita.text = "Questa non è ancora la tua app SMS predefinita. Tocca qui per attivarla."
        }
    }

    private fun chiediDiDiventarePredefinita() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val gestore = getSystemService(RoleManager::class.java)
            if (gestore != null && gestore.isRoleAvailable(RoleManager.ROLE_SMS)) {
                startActivityForResult(gestore.createRequestRoleIntent(RoleManager.ROLE_SMS), RICHIESTA_RUOLO_SMS)
                return
            }
        }
        @Suppress("DEPRECATION")
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
        }
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RICHIESTA_RUOLO_SMS) aggiornaBannerPredefinita()
        if (requestCode == RICHIESTA_CONTATTO && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val numero = leggiNumeroDaContatto(uri)
                if (numero != null) apriConversazioneConNumero(numero)
            }
        }
    }

    private fun leggiNumeroDaContatto(uri: android.net.Uri): String? {
        var cursore: Cursor? = null
        try {
            cursore = contentResolver.query(uri, null, null, null, null)
            if (cursore != null && cursore.moveToFirst()) {
                val indice = cursore.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (indice >= 0) return cursore.getString(indice)
            }
        } finally {
            cursore?.close()
        }
        return null
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

    // ---------- Adapter ----------

    private inner class AdapterConversazioni : BaseAdapter() {
        override fun getCount() = conversazioni.size
        override fun getItem(posizione: Int) = conversazioni[posizione]
        override fun getItemId(posizione: Int) = conversazioni[posizione].threadId

        override fun getView(posizione: Int, convertView: View?, parent: ViewGroup): View {
            val c = conversazioni[posizione]
            val riga = LinearLayout(this@MainActivity)
            riga.orientation = LinearLayout.VERTICAL
            riga.setPadding(dp(16), dp(12), dp(16), dp(12))
            // Sfondo "finestra", diverso dal desktop: è questo a dare struttura alla lista.
            riga.setBackgroundColor(tema.superficie)

            val alto = LinearLayout(this@MainActivity)
            alto.orientation = LinearLayout.HORIZONTAL

            val titolo = TextView(this@MainActivity)
            titolo.text = c.nome ?: formattaNumero(c.numero)
            titolo.textSize = 16f
            titolo.setTextColor(tema.testo)
            if (c.nonLetta) titolo.setTypeface(titolo.typeface, Typeface.BOLD)
            alto.addView(titolo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val data = TextView(this@MainActivity)
            data.text = DateUtils.getRelativeTimeSpanString(c.data, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)
            data.textSize = 12f
            data.setTextColor(tema.testoSecondario)
            alto.addView(data)

            riga.addView(alto)

            val anteprima = TextView(this@MainActivity)
            anteprima.text = c.ultimoTesto
            anteprima.textSize = 14f
            anteprima.setTextColor(if (c.nonLetta) tema.testo else tema.testoSecondario)
            anteprima.maxLines = 1
            anteprima.ellipsize = android.text.TextUtils.TruncateAt.END
            riga.addView(anteprima)

            return riga
        }
    }

    private fun formattaNumero(numero: String): String = PhoneNumberUtils.formatNumber(numero, java.util.Locale.getDefault().country) ?: numero
}
