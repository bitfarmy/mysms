package com.personale.messaggi

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.database.Cursor
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.text.InputType
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var elencoView: ListView
    private lateinit var bannerPredefinita: TextView
    private lateinit var adapter: AdapterConversazioni
    private var conversazioni: List<Conversazione> = emptyList()

    private companion object {
        const val RICHIESTA_RUOLO_SMS = 900
        const val RICHIESTA_CONTATTO = 901
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)

        val radice = LinearLayout(this)
        radice.orientation = LinearLayout.VERTICAL

        bannerPredefinita = TextView(this)
        bannerPredefinita.textSize = 15f
        bannerPredefinita.setPadding(dp(20), dp(14), dp(20), dp(14))
        bannerPredefinita.setOnClickListener { chiediDiDiventarePredefinita() }
        radice.addView(bannerPredefinita, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        elencoView = ListView(this)
        adapter = AdapterConversazioni()
        elencoView.adapter = adapter
        elencoView.setOnItemClickListener { _, _, posizione, _ -> apriConversazione(conversazioni[posizione]) }
        radice.addView(elencoView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val nuovo = TextView(this)
        nuovo.text = "+  Nuovo messaggio"
        nuovo.textSize = 16f
        nuovo.gravity = Gravity.CENTER
        nuovo.setPadding(0, dp(16), 0, dp(16))
        nuovo.setOnClickListener { nuovoMessaggio() }
        radice.addView(nuovo, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        setContentView(radice)

        if (!Permessi.tuttiConcessi(this)) Permessi.richiedi(this)
    }

    override fun onResume() {
        super.onResume()
        aggiornaBannerPredefinita()
        if (Permessi.tuttiConcessi(this)) aggiornaElenco()
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

    // ---------- Elenco ----------

    private fun aggiornaElenco() {
        conversazioni = Conversazioni.elenco(this)
        adapter.notifyDataSetChanged()
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
        val predefinita = Telephony.Sms.getDefaultSmsPackage(this) == packageName
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
            riga.setPadding(dp(20), dp(12), dp(20), dp(12))

            val alto = LinearLayout(this@MainActivity)
            alto.orientation = LinearLayout.HORIZONTAL

            val titolo = TextView(this@MainActivity)
            titolo.text = c.nome ?: formattaNumero(c.numero)
            titolo.textSize = 16f
            if (c.nonLetta) titolo.setTypeface(titolo.typeface, Typeface.BOLD)
            alto.addView(titolo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val data = TextView(this@MainActivity)
            data.text = DateUtils.getRelativeTimeSpanString(c.data, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)
            data.textSize = 12f
            data.alpha = 0.6f
            alto.addView(data)

            riga.addView(alto)

            val anteprima = TextView(this@MainActivity)
            anteprima.text = c.ultimoTesto
            anteprima.textSize = 14f
            anteprima.alpha = if (c.nonLetta) 1f else 0.6f
            anteprima.maxLines = 1
            anteprima.ellipsize = android.text.TextUtils.TruncateAt.END
            riga.addView(anteprima)

            return riga
        }
    }

    private fun formattaNumero(numero: String): String = PhoneNumberUtils.formatNumber(numero, java.util.Locale.getDefault().country) ?: numero
}
