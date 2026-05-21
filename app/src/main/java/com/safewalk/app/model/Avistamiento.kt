package com.safewalk.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Avistamiento(
    @SerialName("total_confirmaciones")
    val totalConfirmaciones: Int = 0,
    @SerialName("avistamiento_id")
    val id: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    @SerialName("agresividad")
    val nivelAgresividad: NivelAgresividad = NivelAgresividad.BAJO,
    val descripcion: String = "",
    @SerialName("fecha_creacion")
    val fechaCreacion: String = "",
    @SerialName("ubicacion_aproximada")
    val ubicacionAproximada: String = "",
    @SerialName("usuario_id")
    val usuarioId: String = "",
    @SerialName("total_ya_no_esta")
    val totalYaNoEsta: Int = 0,
)

@Serializable
enum class NivelAgresividad {
    @SerialName("bajo") BAJO,
    @SerialName("medio") MEDIO,
    @SerialName("alto") ALTO
}

@Serializable
data class AvistamientoInsert(
    @SerialName("usuario_id") val usuarioId: String?,
    val latitud: Double,
    val longitud: Double,
    val ubicacion: String,
    val agresividad: String,
    val descripcion: String,
    @SerialName("ubicacion_aproximada") val ubicacionAproximada: String
)

@Serializable
data class FotoInsert(
    @SerialName("avistamiento_id") val avistamientoId: String,
    val url: String
)
@Serializable
data class IncrementarContadorParams(
    val id: String,
    val delta: Int
)

@Serializable
data class IncrementarYaNoEstaParams(
    val id: String,
    val delta: Int
)
@Serializable
data class Compartido(
    @SerialName("avistamiento_id") val avistamientoId: String = "",
    @SerialName("usuario_id") val usuarioId: String = ""
)
@Serializable
data class FotoInfo(
    @SerialName("foto_id") val fotoId: String = "",
    @SerialName("avistamiento_id") val avistamientoId: String = "",
    val url: String = ""
)