package com.safewalk.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reporte(
    @SerialName("avistamiento_id") // Nombre exacto en tu tabla de Postgres
    val reporteId: String,
    @SerialName("usuario_id")
    val usuarioId: String,
    val motivo: String,
    val detalles: String? = null
)