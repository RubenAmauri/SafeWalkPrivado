package com.safewalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safewalk.app.SupabaseClient
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.NivelAgresividad
import com.safewalk.app.repository.AvistamientoRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReporteInteraccion(
    val avistamiento: Avistamiento,
    val validaciones: Int,
    val invalidaciones: Int,
    val comentarios: Int,
    val compartidos: Int
) {
    val total = validaciones + invalidaciones + comentarios + compartidos
}

data class DashboardData(
    val totalReportes: Int = 0,
    val totalValidaciones: Int = 0,
    val totalInvalidaciones: Int = 0,
    val totalComentarios: Int = 0,
    val totalCompartidos: Int = 0,
    val reportesPorSemana: List<Pair<String, Int>> = emptyList(),
    val bajo: Int = 0,
    val medio: Int = 0,
    val alto: Int = 0,
    val interacciones: List<ReporteInteraccion> = emptyList(),
    val reportesInactivos: List<Avistamiento> = emptyList()
)

class DashboardViewModel : ViewModel() {

    private val sdfMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
    private val sdfBasic = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }

    private val _datos = MutableStateFlow<DashboardData?>(null)
    val datos: StateFlow<DashboardData?> = _datos

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    fun cargar() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                val reportes = AvistamientoRepository.getMisReportes(uid)

                if (reportes.isEmpty()) {
                    _datos.value = DashboardData()
                    return@launch
                }

                val ids = reportes.map { it.id }
                val comentariosPorReporte = AvistamientoRepository.getComentariosCountPorReporte(ids)
                val compartidosPorReporte = AvistamientoRepository.getCompartidosCountPorReporte(ids)

                val totalValidaciones = reportes.sumOf { it.totalConfirmaciones }
                val totalInvalidaciones = reportes.sumOf { it.totalYaNoEsta }
                val totalComentarios = comentariosPorReporte.values.sum()
                val totalCompartidos = compartidosPorReporte.values.sum()

                val bajo = reportes.count { it.nivelAgresividad == NivelAgresividad.BAJO }
                val medio = reportes.count { it.nivelAgresividad == NivelAgresividad.MEDIO }
                val alto = reportes.count { it.nivelAgresividad == NivelAgresividad.ALTO }

                val interacciones = reportes.map { r ->
                    ReporteInteraccion(
                        avistamiento = r,
                        validaciones = r.totalConfirmaciones,
                        invalidaciones = r.totalYaNoEsta,
                        comentarios = comentariosPorReporte[r.id] ?: 0,
                        compartidos = compartidosPorReporte[r.id] ?: 0
                    )
                }.sortedByDescending { it.avistamiento.fechaCreacion }

                val ahora = Date()
                val milisEn14Dias = 14L * 24 * 60 * 60 * 1000
                val inactivos = reportes.filter { r ->
                    val fecha = parseFecha(r.fechaCreacion)
                    if (fecha != null) {
                        val diff = ahora.time - fecha.time
                        val totalInt = r.totalConfirmaciones + r.totalYaNoEsta +
                                (comentariosPorReporte[r.id] ?: 0) + (compartidosPorReporte[r.id] ?: 0)
                        diff >= milisEn14Dias && totalInt == 0
                    } else false
                }


                _datos.value = DashboardData(
                    totalReportes = reportes.size,
                    totalValidaciones = totalValidaciones,
                    totalInvalidaciones = totalInvalidaciones,
                    totalComentarios = totalComentarios,
                    totalCompartidos = totalCompartidos,
                    reportesPorSemana = calcularPorSemana(reportes),
                    bajo = bajo,
                    medio = medio,
                    alto = alto,
                    interacciones = interacciones,
                    reportesInactivos = inactivos
                )
            } catch (e: Exception) {
                android.util.Log.e("SafeWalk", "Error dashboard: ${e.message}", e)
            } finally {
                _cargando.value = false
            }
        }
    }

    private val tzOffsetRegex = Regex("\\+\\d{2}:\\d{2}$")
    private val microsecondsRegex = Regex("(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\d+")

    private fun parseFecha(fechaStr: String): Date? {
        return try {
            val normalizada = fechaStr
                .replace(microsecondsRegex, "$1")
                .replace("+00:00", "Z")
                .replace(tzOffsetRegex, "Z")
            sdfMillis.parse(normalizada)
        } catch (e: Exception) {
            try {
                sdfBasic.parse(fechaStr.replace("+00:00", "Z").replace(tzOffsetRegex, "Z"))
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun calcularPorSemana(reportes: List<Avistamiento>): List<Pair<String, Int>> {
        val ahora = System.currentTimeMillis()
        val unDiaMs = 24L * 60 * 60 * 1000
        val unaSemanaMs = 7 * unDiaMs

        return (5 downTo 0).map { semanas ->
            val finMs = ahora - (semanas * unaSemanaMs)
            val inicioMs = finMs - unaSemanaMs

            val count = reportes.count { r ->
                val f = parseFecha(r.fechaCreacion)
                f != null && f.time >= inicioMs && f.time < finMs
            }
            val etiqueta = if (semanas == 0) "Esta\nsem." else "Hace\n${semanas}s"
            etiqueta to count
        }
    }
}