package com.safewalk.app.repository

import com.google.android.gms.maps.model.LatLng
import com.safewalk.app.SupabaseClient
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.AvistamientoInsert
import com.safewalk.app.model.NivelAgresividad
import com.safewalk.app.model.ZonaAvistamiento
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlin.math.*

object AvistamientoRepository {

    suspend fun iniciarSesionAnonima() {
        val session = SupabaseClient.client.auth.currentSessionOrNull()
        if (session == null) {
            SupabaseClient.client.auth.signInAnonymously()
        }
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        try {
            SupabaseClient.client.postgrest
                .from("usuarios")
                .upsert(mapOf("usuario_id" to uid)) {
                    onConflict = "usuario_id"
                }
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error al crear usuario: ${e.message}", e)
        }
    }

    suspend fun getAvistamientos(): List<Avistamiento> {
        return SupabaseClient.client.postgrest
            .from("avistamientos")
            .select()
            .decodeList<Avistamiento>()
    }

    suspend fun getAvistamientosFeed(latitud: Double, longitud: Double): List<Avistamiento> {
        val resultados = SupabaseClient.client.postgrest
            .from("avistamientos")
            .select {
                filter {
                    neq("estado", "inactivo")
                }
            }
            .decodeList<Avistamiento>()

        return resultados.sortedBy { avistamiento ->
            distanciaMetros(latitud, longitud, avistamiento.latitud, avistamiento.longitud)
        }
    }

    suspend fun agregarAvistamiento(avistamiento: Avistamiento) {
        try {
            val insert = AvistamientoInsert(
                usuarioId = SupabaseClient.client.auth.currentUserOrNull()?.id,
                latitud = avistamiento.latitud,
                longitud = avistamiento.longitud,
                ubicacion = "POINT(${avistamiento.longitud} ${avistamiento.latitud})",
                agresividad = avistamiento.nivelAgresividad.name.lowercase(),
                descripcion = avistamiento.descripcion,
                ubicacionAproximada = avistamiento.ubicacionAproximada
            )
            SupabaseClient.client.postgrest
                .from("avistamientos")
                .insert(insert)
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error al insertar avistamiento: ${e.message}", e)
            throw e
        }
    }

    fun getZonas(avistamientos: List<Avistamiento>): List<ZonaAvistamiento> {
        val radioAgrupacion = 1500.0
        val grupos = mutableListOf<MutableList<Avistamiento>>()

        for (avistamiento in avistamientos) {
            val grupoExistente = grupos.firstOrNull { grupo ->
                val centroLat = grupo.map { it.latitud }.average()
                val centroLng = grupo.map { it.longitud }.average()
                distanciaMetros(centroLat, centroLng, avistamiento.latitud, avistamiento.longitud) < radioAgrupacion
            }
            if (grupoExistente != null) grupoExistente.add(avistamiento)
            else grupos.add(mutableListOf(avistamiento))
        }

        return grupos.mapIndexed { index, grupo ->
            val centroLat = grupo.map { it.latitud }.average()
            val centroLng = grupo.map { it.longitud }.average()
            ZonaAvistamiento(
                id = "zona_$index",
                centro = LatLng(centroLat, centroLng),
                avistamientos = grupo,
                nivelPromedio = calcularNivelPromedio(grupo)
            )
        }
    }

    private fun calcularNivelPromedio(avistamientos: List<Avistamiento>): NivelAgresividad {
        val promedio = avistamientos.map {
            when (it.nivelAgresividad) {
                NivelAgresividad.BAJO -> 1
                NivelAgresividad.MEDIO -> 2
                NivelAgresividad.ALTO -> 3
            }
        }.average()
        return when {
            promedio >= 2.5 -> NivelAgresividad.ALTO
            promedio >= 1.5 -> NivelAgresividad.MEDIO
            else -> NivelAgresividad.BAJO
        }
    }

    internal fun distanciaMetros(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
    suspend fun eliminarAvistamiento(avistamientoId: String) {
        SupabaseClient.client.postgrest
            .from("avistamientos")
            .delete {
                filter {
                    eq("avistamiento_id", avistamientoId)
                }
            }
    }

    suspend fun editarAvistamiento(
        avistamientoId: String,
        descripcion: String,
        agresividad: String,
        ubicacionAproximada: String
    ) {
        SupabaseClient.client.postgrest
            .from("avistamientos")
            .update({
                set("descripcion", descripcion)
                set("agresividad", agresividad)
                set("ubicacion_aproximada", ubicacionAproximada)
                set("fecha_actualizacion", java.util.Date().toInstant().toString())
            }) {
                filter {
                    eq("avistamiento_id", avistamientoId)
                }
            }
    }
}