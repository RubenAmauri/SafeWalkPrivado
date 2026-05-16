package com.safewalk.app.model

data class Comentario(
    val id: String,
    val avistamientoId: String,
    val usuario: String,
    val texto: String,
    val fecha: String
)