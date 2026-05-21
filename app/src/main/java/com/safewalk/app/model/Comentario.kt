package com.safewalk.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comentario(
    @SerialName("comentario_id") val id: String = "",
    @SerialName("avistamiento_id") val avistamientoId: String = "",
    @SerialName("usuario_id") val usuarioId: String = "",
    @SerialName("contenido") val texto: String = "",
    @SerialName("fecha_creacion") val fecha: String = ""
)

@Serializable
data class ComentarioInsert(
    @SerialName("avistamiento_id") val avistamientoId: String,
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("contenido") val contenido: String
)