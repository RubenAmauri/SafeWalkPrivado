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
import com.safewalk.app.model.ZonaFrecuente

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
    private val _zonaMasCercana = MutableStateFlow<Pair<ZonaAvistamiento, Double>?>(null)
    val zonaMasCercana: StateFlow<Pair<ZonaAvistamiento, Double>?> = _zonaMasCercana
    private val _avistamientoMasCercanoId = MutableStateFlow<String?>(null)
    val avistamientoMasCercanoId: StateFlow<String?> = _avistamientoMasCercanoId
    private val _avisoZonasFrecuentes = MutableStateFlow<Int>(0)
    val avisoZonasFrecuentes: StateFlow<Int> = _avisoZonasFrecuentes

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
                verificarReportesCercanosAZonasFrecuentes()
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
    fun calcularZonaMasCercana(lat: Double, lng: Double) {
        val zonas = _zonas.value
        if (zonas.isEmpty()) return
        val cercana = zonas.minByOrNull { zona ->
            AvistamientoRepository.distanciaMetros(lat, lng, zona.centro.latitude, zona.centro.longitude)
        } ?: return
        val distancia = AvistamientoRepository.distanciaMetros(lat, lng, cercana.centro.latitude, cercana.centro.longitude)
        _zonaMasCercana.value = Pair(cercana, distancia)

        // Marcar el avistamiento más cercano dentro de esa zona
        val avistamientoMasCercano = cercana.avistamientos.minByOrNull { a ->
            AvistamientoRepository.distanciaMetros(lat, lng, a.latitud, a.longitud)
        }
        _avistamientoMarcado.value = avistamientoMasCercano
        _avistamientoMasCercanoId.value = avistamientoMasCercano?.id
    }

    fun limpiarZonaMasCercana() {
        _zonaMasCercana.value = null
        _avistamientoMasCercanoId.value = null
        _avistamientoMarcado.value = null
    }

    fun limpiarAvisoZonasFrecuentes() {
        _avisoZonasFrecuentes.value = 0
    }

    private suspend fun verificarReportesCercanosAZonasFrecuentes() {
        try {
            val zonasFrecuentes = AvistamientoRepository.getZonasFrecuentes()
            if (zonasFrecuentes.isEmpty()) return
            val avistamientos = _zonas.value.flatMap { it.avistamientos }
            var totalCercanos = 0
            zonasFrecuentes.forEach { zona ->
                val cercanos = avistamientos.count { a ->
                    AvistamientoRepository.distanciaMetros(
                        zona.latitud, zona.longitud, a.latitud, a.longitud
                    ) <= zona.radio
                }
                totalCercanos += cercanos
            }
            _avisoZonasFrecuentes.value = totalCercanos
        } catch (e: Exception) {
            android.util.Log.e("SafeWalk", "Error verificarZonasFrecuentes: ${e.message}", e)
        }
    }
}