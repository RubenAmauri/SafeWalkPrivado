package com.safewalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safewalk.app.SupabaseClient
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.ZonaAvistamiento
import com.safewalk.app.repository.AvistamientoRepository
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MapaViewModel : ViewModel() {

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
    private val _ubicacionPendiente = MutableStateFlow<Pair<Double, Double>?>(null)
    val ubicacionPendiente: StateFlow<Pair<Double, Double>?> = _ubicacionPendiente

    private var recargarJob: Job? = null

    fun navegarAUbicacion(lat: Double, lng: Double) {
        _ubicacionPendiente.value = Pair(lat, lng)
    }

    fun consumirUbicacionPendiente() {
        _ubicacionPendiente.value = null
    }

    init {
        viewModelScope.launch {
            AvistamientoRepository.iniciarSesionAnonima()
            recargarZonas()
            suscribirseACambios()
        }
    }

    private suspend fun suscribirseACambios() {
        try {
            val channel = SupabaseClient.client.realtime.channel("avistamientos")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "avistamientos"
            }.onEach {
                recargarJob?.cancel()
                recargarJob = viewModelScope.launch {
                    delay(2000)
                    recargarZonas()
                }
            }.launchIn(viewModelScope)
            channel.subscribe()
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error al suscribirse: ${e.message}", e)
        }
    }

    fun recargarZonas() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val avistamientos = AvistamientoRepository.getAvistamientos()
                _zonas.value = withContext(Dispatchers.Default) {
                    AvistamientoRepository.getZonas(avistamientos)
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar avistamientos: ${e.message}"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun seleccionarZona(zona: ZonaAvistamiento) {
        _zonaSeleccionada.value = zona
    }

    fun marcarAvistamiento(avistamiento: Avistamiento) {
        _avistamientoMarcado.value = avistamiento
    }

    fun cerrarDetalle() {
        _zonaSeleccionada.value = null
        _avistamientoMarcado.value = null
    }
}