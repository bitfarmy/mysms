package com.personale.messaggi

enum class StatoMessaggio { RICEVUTO, IN_CORSO, INVIATO, FALLITO }

data class Conversazione(
    val threadId: Long,
    val numero: String,
    val nome: String?,
    val ultimoTesto: String,
    val data: Long,
    val nonLetta: Boolean,
)

data class Messaggio(
    val id: Long,
    val corpo: String,
    val data: Long,
    val inviatoDaMe: Boolean,
    val stato: StatoMessaggio,
)

data class Contatto(val nome: String, val fotoUri: String?)
