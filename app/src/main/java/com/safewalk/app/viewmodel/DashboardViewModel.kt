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
import java.util.Calendar
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
                }.sortedByDescending { it.total }

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

    private fun parseFecha(fechaStr: String): Date? {
        val formatos = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (formato in formatos) {
            try {
                val sdf = SimpleDateFormat(formato, Locale.getDefault())
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                return sdf.parse(fechaStr)
            } catch (e: Exception) { continue }
        }
        return null
    }

    private fun calcularPorSemana(reportes: List<Avistamiento>): List<Pair<String, Int>> {
        return (5 downTo 0).map { semanas ->
            val inicio = Calendar.getInstance().apply {
                add(Calendar.WEEK_OF_YEAR, -semanas)
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }.time
            val fin = Calendar.getInstance().apply {
                add(Calendar.WEEK_OF_YEAR, -semanas)
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
            }.time
            val count = reportes.count { r ->
                val f = parseFecha(r.fechaCreacion)
                f != null && !f.before(inicio) && !f.after(fin)
            }
            val etiqueta = if (semanas == 0) "Esta\nsem." else "Hace\n${semanas}s"
            etiqueta to count
        }
    }
}