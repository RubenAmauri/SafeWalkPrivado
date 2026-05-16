package com.safewalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safewalk.app.SupabaseClient
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.repository.AvistamientoRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HistorialViewModel : ViewModel() {

    private val _reportes = MutableStateFlow<List<Avistamiento>>(emptyList())
    val reportes: StateFlow<List<Avistamiento>> = _reportes

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        cargarMisReportes()
    }

    fun cargarMisReportes() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                _reportes.value = SupabaseClient.client.postgrest
                    .from("avistamientos")
                    .select {
                        filter {
                            eq("usuario_id", uid)
                        }
                        order("fecha_creacion", Order.DESCENDING)
                    }
                    .decodeList<Avistamiento>()
            } catch (e: Exception) {
                _error.value = "Error al cargar tus reportes: ${e.message}"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun eliminarReporte(avistamientoId: String) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                AvistamientoRepository.eliminarAvistamiento(avistamientoId)
                cargarMisReportes()
            } catch (e: Exception) {
                _error.value = "Error al eliminar: ${e.message}"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun editarReporte(
        avistamientoId: String,
        descripcion: String,
        agresividad: String,
        ubicacionAproximada: String
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                AvistamientoRepository.editarAvistamiento(
                    avistamientoId = avistamientoId,
                    descripcion = descripcion,
                    agresividad = agresividad,
                    ubicacionAproximada = ubicacionAproximada
                )
                cargarMisReportes()
            } catch (e: Exception) {
                _error.value = "Error al editar: ${e.message}"
            } finally {
                _cargando.value = false
            }
        }
    }
}