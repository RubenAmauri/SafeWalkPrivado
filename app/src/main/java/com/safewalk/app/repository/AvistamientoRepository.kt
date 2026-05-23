package com.safewalk.app.repository

import com.google.android.gms.maps.model.LatLng
import com.safewalk.app.SupabaseClient
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.AvistamientoInsert
import com.safewalk.app.model.Comentario
import com.safewalk.app.model.ComentarioInsert
import com.safewalk.app.model.Compartido
import com.safewalk.app.model.FotoInsert
import com.safewalk.app.model.IncrementarContadorParams
import com.safewalk.app.model.IncrementarYaNoEstaParams
import com.safewalk.app.model.NivelAgresividad
import com.safewalk.app.model.ZonaAvistamiento
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlin.math.*
import com.safewalk.app.model.FotoInfo
import com.safewalk.app.model.ZonaFrecuente
import com.safewalk.app.model.ZonaFrecuenteInsert

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

    suspend fun agregarAvistamiento(avistamiento: Avistamiento): String {
        val resultado = SupabaseClient.client.postgrest
            .from("avistamientos")
            .insert(AvistamientoInsert(
                usuarioId = SupabaseClient.client.auth.currentUserOrNull()?.id,
                latitud = avistamiento.latitud,
                longitud = avistamiento.longitud,
                ubicacion = "POINT(${avistamiento.longitud} ${avistamiento.latitud})",
                agresividad = avistamiento.nivelAgresividad.name.lowercase(),
                descripcion = avistamiento.descripcion,
                ubicacionAproximada = avistamiento.ubicacionAproximada
            )) {
                select()
            }
            .decodeSingle<Avistamiento>()
        return resultado.id
    }

    fun getZonas(avistamientos: List<Avistamiento>): List<ZonaAvistamiento> {
        val radioAgrupacion = 1500.0
        val grupos = mutableListOf<MutableList<Avistamiento>>()
        val sumLat = mutableListOf<Double>()
        val sumLng = mutableListOf<Double>()

        for (avistamiento in avistamientos) {
            val index = grupos.indices.firstOrNull { i ->
                val n = grupos[i].size.toDouble()
                distanciaMetros(sumLat[i] / n, sumLng[i] / n, avistamiento.latitud, avistamiento.longitud) < radioAgrupacion
            }
            if (index != null) {
                grupos[index].add(avistamiento)
                sumLat[index] += avistamiento.latitud
                sumLng[index] += avistamiento.longitud
            } else {
                grupos.add(mutableListOf(avistamiento))
                sumLat.add(avistamiento.latitud)
                sumLng.add(avistamiento.longitud)
            }
        }

        return grupos.mapIndexed { index, grupo ->
            val n = grupo.size.toDouble()
            ZonaAvistamiento(
                id = "zona_$index",
                centro = LatLng(sumLat[index] / n, sumLng[index] / n),
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
    suspend fun subirFoto(
        avistamientoId: String,
        usuarioId: String,
        imagenBytes: ByteArray
    ): String? {
        return try {
            val path = "$usuarioId/$avistamientoId.jpg"
            SupabaseClient.client.storage
                .from("fotos-avistamientos")
                .upload(path, imagenBytes)
            val url = SupabaseClient.client.storage
                .from("fotos-avistamientos")
                .publicUrl(path)
            SupabaseClient.client.postgrest
                .from("fotos")
                .insert(FotoInsert(avistamientoId = avistamientoId, url = url))
            url
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error al subir foto: ${e.message}", e)
            null
        }
    }
    suspend fun getFotosAvistamiento(avistamientoId: String): List<String> {
        return try {
            SupabaseClient.client.postgrest
                .from("fotos")
                .select {
                    filter {
                        eq("avistamiento_id", avistamientoId)
                    }
                }
                .decodeList<FotoInsert>()
                .map { it.url }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun obtenerValidacionUsuario(avistamientoId: String, usuarioId: String): String? {
        return try {
            val resultado = SupabaseClient.client.postgrest
                .from("validaciones")
                .select {
                    filter {
                        eq("avistamiento_id", avistamientoId)
                        eq("usuario_id", usuarioId)
                    }
                }
                .decodeList<Map<String, String>>()
            resultado.firstOrNull()?.get("tipo")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun registrarValidacion(avistamientoId: String, usuarioId: String, tipo: String) {
        // Upsert — si ya existe una validación del usuario, la reemplaza
        SupabaseClient.client.postgrest
            .from("validaciones")
            .upsert(mapOf(
                "avistamiento_id" to avistamientoId,
                "usuario_id" to usuarioId,
                "tipo" to tipo
            )) {
                onConflict = "avistamiento_id,usuario_id"
            }
    }

    suspend fun eliminarValidacion(avistamientoId: String, usuarioId: String) {
        SupabaseClient.client.postgrest
            .from("validaciones")
            .delete {
                filter {
                    eq("avistamiento_id", avistamientoId)
                    eq("usuario_id", usuarioId)
                }
            }
    }

    suspend fun getContadorConfirmaciones(avistamientoId: String): Int {
        return try {
            SupabaseClient.client.postgrest
                .from("avistamientos")
                .select { filter { eq("avistamiento_id", avistamientoId) } }
                .decodeSingle<Avistamiento>().totalConfirmaciones
        } catch (e: Exception) {
            0
        }
    }

    suspend fun actualizarContador(avistamientoId: String, incremento: Int) {
        try {
            SupabaseClient.client.postgrest.rpc(
                "incrementar_confirmaciones",
                IncrementarContadorParams(id = avistamientoId, delta = incremento)
            )
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error en actualizarContador: ${e.message}", e)
        }
    }

    suspend fun actualizarContadorYaNoEsta(avistamientoId: String, incremento: Int) {
        try {
            SupabaseClient.client.postgrest.rpc(
                "incrementar_ya_no_esta",
                IncrementarYaNoEstaParams(id = avistamientoId, delta = incremento)
            )
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error en actualizarContadorYaNoEsta: ${e.message}", e)
        }
    }
    suspend fun getComentarios(avistamientoId: String): List<Comentario> {
        return try {
            SupabaseClient.client.postgrest
                .from("comentarios")
                .select {
                    filter { eq("avistamiento_id", avistamientoId) }
                }
                .decodeList<Comentario>()
                .sortedBy { it.fecha }
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error al cargar comentarios: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun agregarComentario(avistamientoId: String, texto: String): Comentario? {
        return try {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return null
            SupabaseClient.client.postgrest
                .from("comentarios")
                .insert(
                    ComentarioInsert(
                        avistamientoId = avistamientoId,
                        usuarioId = uid,
                        contenido = texto
                    )
                ) {
                    select()
                }
                .decodeSingle<Comentario>()
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error al agregar comentario: ${e.message}", e)
            null
        }
    }
    suspend fun registrarCompartido(avistamientoId: String) {
        try {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
            SupabaseClient.client.postgrest
                .from("compartidos")
                .insert(mapOf(
                    "avistamiento_id" to avistamientoId,
                    "usuario_id" to uid
                ))
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error al registrar compartido: ${e.message}", e)
        }
    }
    suspend fun getMisReportes(uid: String): List<Avistamiento> {
        return try {
            SupabaseClient.client.postgrest
                .from("avistamientos")
                .select { filter { eq("usuario_id", uid) } }
                .decodeList<Avistamiento>()
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error getMisReportes: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getComentariosCountPorReporte(ids: List<String>): Map<String, Int> {
        if (ids.isEmpty()) return emptyMap()
        return try {
            SupabaseClient.client.postgrest
                .from("comentarios")
                .select { filter { isIn("avistamiento_id", ids) } }
                .decodeList<Comentario>()
                .groupBy { it.avistamientoId }
                .mapValues { it.value.size }
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error getComentariosCount: ${e.message}", e)
            emptyMap()
        }
    }

    suspend fun getCompartidosCountPorReporte(ids: List<String>): Map<String, Int> {
        if (ids.isEmpty()) return emptyMap()
        return try {
            SupabaseClient.client.postgrest
                .from("compartidos")
                .select { filter { isIn("avistamiento_id", ids) } }
                .decodeList<Compartido>()
                .groupBy { it.avistamientoId }
                .mapValues { it.value.size }
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error getCompartidosCount: ${e.message}", e)
            emptyMap()
        }
    }
    suspend fun getFotosConId(avistamientoId: String): List<FotoInfo> {
        return try {
            SupabaseClient.client.postgrest
                .from("fotos")
                .select { filter { eq("avistamiento_id", avistamientoId) } }
                .decodeList<FotoInfo>()
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error getFotosConId: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun eliminarFoto(fotoId: String, avistamientoId: String, usuarioId: String) {
        try {
            SupabaseClient.client.postgrest
                .from("fotos")
                .delete { filter { eq("foto_id", fotoId) } }
            try {
                SupabaseClient.client.storage
                    .from("fotos-avistamientos")
                    .delete(listOf("$usuarioId/$avistamientoId.jpg"))
            } catch (e: Exception) {
                android.util.Log.w("SafeWalk", "No se pudo eliminar del storage: ${e.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error eliminarFoto: ${e.message}", e)
        }
    }

    suspend fun editarAvistamientoCompleto(
        avistamientoId: String,
        descripcion: String,
        agresividad: String,
        ubicacionAproximada: String,
        latitud: Double,
        longitud: Double
    ) {
        SupabaseClient.client.postgrest
            .from("avistamientos")
            .update({
                set("descripcion", descripcion)
                set("agresividad", agresividad)
                set("ubicacion_aproximada", ubicacionAproximada)
                set("latitud", latitud)
                set("longitud", longitud)
                set("ubicacion", "POINT($longitud $latitud)")
                set("fecha_actualizacion", java.util.Date().toInstant().toString())
            }) {
                filter { eq("avistamiento_id", avistamientoId) }
            }
    }
    suspend fun editarComentario(comentarioId: String, nuevoTexto: String): Comentario? {
        return try {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return null
            SupabaseClient.client.postgrest
                .from("comentarios")
                .update({ set("contenido", nuevoTexto) }) {
                    filter {
                        eq("comentario_id", comentarioId)
                        eq("usuario_id", uid)
                    }
                    select()
                }
                .decodeSingle<Comentario>()
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error al editar comentario: ${e.message}", e)
            null
        }
    }

    suspend fun eliminarComentario(comentarioId: String) {
        try {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
            SupabaseClient.client.postgrest
                .from("comentarios")
                .delete {
                    filter {
                        eq("comentario_id", comentarioId)
                        eq("usuario_id", uid)
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error al eliminar comentario: ${e.message}", e)
        }
    }
    suspend fun getZonasFrecuentes(): List<ZonaFrecuente> {
        return try {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return emptyList()
            SupabaseClient.client.postgrest
                .from("zonas_frecuentes")
                .select { filter { eq("usuario_id", uid) } }
                .decodeList<ZonaFrecuente>()
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error getZonasFrecuentes: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun agregarZonaFrecuente(nombre: String, latitud: Double, longitud: Double): ZonaFrecuente? {
        return try {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return null
            SupabaseClient.client.postgrest
                .from("zonas_frecuentes")
                .insert(
                    ZonaFrecuenteInsert(
                        usuarioId = uid,
                        nombre = nombre,
                        latitud = latitud,
                        longitud = longitud
                    )
                ) { select() }
                .decodeSingle<ZonaFrecuente>()
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error agregarZonaFrecuente: ${e.message}", e)
            null
        }
    }

    suspend fun editarZonaFrecuente(zonaId: String, nombre: String, latitud: Double, longitud: Double) {
        try {
            SupabaseClient.client.postgrest
                .from("zonas_frecuentes")
                .update({
                    set("nombre", nombre)
                    set("latitud", latitud)
                    set("longitud", longitud)
                }) { filter { eq("zona_id", zonaId) } }
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error editarZonaFrecuente: ${e.message}", e)
        }
    }

    suspend fun eliminarZonaFrecuente(zonaId: String) {
        try {
            SupabaseClient.client.postgrest
                .from("zonas_frecuentes")
                .delete { filter { eq("zona_id", zonaId) } }
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error eliminarZonaFrecuente: ${e.message}", e)
        }
    }

}

