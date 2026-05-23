package com.safewalk.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZonaFrecuente(
    @SerialName("zona_id") val id: String = "",
    @SerialName("usuario_id") val usuarioId: String = "",
    val nombre: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val radio: Double = 500.0,
    @SerialName("fecha_creacion") val fechaCreacion: String = ""
)

@Serializable
data class ZonaFrecuenteInsert(
    @SerialName("usuario_id") val usuarioId: String,
    val nombre: String,
    val latitud: Double,
    val longitud: Double,
    val radio: Double = 500.0
)