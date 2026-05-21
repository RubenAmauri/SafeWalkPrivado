package com.safewalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.NivelAgresividad
import com.safewalk.app.repository.AvistamientoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    private val _avistamientos = MutableStateFlow<List<Avistamiento>>(emptyList())

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _avistamientoSeleccionado = MutableStateFlow<Avistamiento?>(null)
    val avistamientoSeleccionado: StateFlow<Avistamiento?> = _avistamientoSeleccionado

    private val _nivelesSeleccionados = MutableStateFlow<Set<NivelAgresividad>>(emptySet())
    val nivelesSeleccionados: StateFlow<Set<NivelAgresividad>> = _nivelesSeleccionados

    private val _soloRecientes = MutableStateFlow(false)
    val soloRecientes: StateFlow<Boolean> = _soloRecientes

    private val _inactivosIds = MutableStateFlow<Set<String>>(emptySet())
    val inactivosIds: StateFlow<Set<String>> = _inactivosIds

    private val _tabActivo = MutableStateFlow(0)
    val tabActivo: StateFlow<Int> = _tabActivo

    fun setInactivosIds(ids: Set<String>) {
        _inactivosIds.value = ids
    }

    fun setTabActivo(tab: Int) {
        _tabActivo.value = tab
    }

    val avistamientos: StateFlow<List<Avistamiento>> = combine(
        _avistamientos,
        _nivelesSeleccionados,
        _soloRecientes
    ) { lista, niveles, recientes ->
        var resultado = lista

        if (niveles.isNotEmpty()) {
            resultado = resultado.filter { it.nivelAgresividad in niveles }
        }

        if (recientes) {
            val hace12h = System.currentTimeMillis() - 12 * 60 * 60 * 1000
            resultado = resultado.filter { avistamiento ->
                try {
                    val normalizada = avistamiento.fechaCreacion
                        .replace(Regex("\\.\\d+"), "")
                        .replace("+00:00", "+0000")
                        .replace("Z", "+0000")
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.US)
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val fecha = sdf.parse(normalizada)
                    fecha != null && fecha.time >= hace12h
                } catch (e: Exception) {
                    true
                }
            }
        }
        resultado
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun cargarFeed(latitud: Double, longitud: Double) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                _avistamientos.value = AvistamientoRepository.getAvistamientosFeed(latitud, longitud)
            } catch (e: Exception) {
                _error.value = "Error al cargar el feed: ${e.message}"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun toggleNivel(nivel: NivelAgresividad) {
        val actual = _nivelesSeleccionados.value.toMutableSet()
        if (nivel in actual) actual.remove(nivel) else actual.add(nivel)
        _nivelesSeleccionados.value = actual
    }

    fun toggleRecientes() {
        _soloRecientes.value = !_soloRecientes.value
    }

    fun limpiarFiltros() {
        _nivelesSeleccionados.value = emptySet()
        _soloRecientes.value = false
    }

    fun seleccionarAvistamiento(avistamiento: Avistamiento) {
        _avistamientoSeleccionado.value = avistamiento
    }

    fun limpiarSeleccion() {
        _avistamientoSeleccionado.value = null
    }
}