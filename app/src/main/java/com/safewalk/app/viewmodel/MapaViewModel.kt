package com.safewalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safewalk.app.SupabaseClient
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.ZonaAvistamiento
import com.safewalk.app.repository.AvistamientoRepository
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.safewalk.app.model.NivelAgresividad

class MapaViewModel : ViewModel() {
    var descripcionReporte by mutableStateOf("")
    var nivelReporte by mutableStateOf<NivelAgresividad?>(null)

    private val _zonas = MutableStateFlow<List<ZonaAvistamiento>>(emptyList())
    val zonas: StateFlow<List<ZonaAvistamiento>> = _zonas

    private val _zonaSeleccionada = MutableStateFlow<ZonaAvistamiento?>(null)
    val zonaSeleccionada: StateFlow<ZonaAvistamiento?> = _zonaSeleccionada

    private val _avistamientoMarcado = MutableStateFlow<Avistamiento?>(null)
    val avistamientoMarcado: StateFlow<Avistamiento?> = _avistamientoMarcado

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            AvistamientoRepository.iniciarSesionAnonima()
            recargarZonas()
            suscribirseACambios()
        }
    }

    private suspend fun suscribirseACambios() {
        val channel = SupabaseClient.client.realtime.channel("avistamientos")

        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "avistamientos"
        }.onEach {
            android.util.Log.d("SafeWalk", "Cambio detectado en avistamientos: $it")
            recargarZonas()
        }.launchIn(viewModelScope)

        channel.subscribe()
        android.util.Log.d("SafeWalk", "Suscrito a canal avistamientos")
    }

    fun recargarZonas() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val avistamientos = AvistamientoRepository.getAvistamientos()
                _zonas.value = AvistamientoRepository.getZonas(avistamientos)
            } catch (e: Exception) {
                _error.value = "Error al cargar avistamientos: ${e.message}"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun agregarAvistamiento(avistamiento: Avistamiento) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                AvistamientoRepository.agregarAvistamiento(avistamiento)
                recargarZonas()
            } catch (e: Exception) {
                _error.value = "Error al guardar avistamiento: ${e.message}"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun seleccionarZona(zona: ZonaAvistamiento) {
        _zonaSeleccionada.value = zona
    }

    fun seleccionar(avistamiento: Avistamiento) {
        _avistamientoMarcado.value = avistamiento
    }

    fun marcarAvistamiento(avistamiento: Avistamiento) {
        _avistamientoMarcado.value = avistamiento
    }

    fun cerrarDetalle() {
        _zonaSeleccionada.value = null
        _avistamientoMarcado.value = null
    }
    fun limpiarFormularioReporte() {
        descripcionReporte = ""
        nivelReporte = null
    }
}