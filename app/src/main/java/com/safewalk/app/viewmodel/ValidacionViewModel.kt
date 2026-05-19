package com.safewalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safewalk.app.SupabaseClient
import com.safewalk.app.repository.AvistamientoRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ValidacionViewModel : ViewModel() {

    private val _validaciones = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val validaciones: StateFlow<Map<String, Boolean>> = _validaciones

    private val _contadores = MutableStateFlow<Map<String, Int>>(emptyMap())
    val contadores: StateFlow<Map<String, Int>> = _contadores

    private val _cargando = MutableStateFlow<Set<String>>(emptySet())
    val cargando: StateFlow<Set<String>> = _cargando

    fun cargarValidacion(avistamientoId: String, contadorInicial: Int) {
        viewModelScope.launch {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
            val yaValido = AvistamientoRepository.obtenerValidacionUsuario(avistamientoId, uid)
            _validaciones.value = _validaciones.value + (avistamientoId to yaValido)
            _contadores.value = _contadores.value + (avistamientoId to contadorInicial)
        }
    }

    fun toggleValidacion(avistamientoId: String) {
        viewModelScope.launch {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
            val yaValido = _validaciones.value[avistamientoId] ?: false
            val contadorActual = _contadores.value[avistamientoId] ?: 0

            _cargando.value = _cargando.value + avistamientoId
            try {
                if (yaValido) {
                    AvistamientoRepository.retirarValidacion(avistamientoId, uid)
                    _validaciones.value = _validaciones.value + (avistamientoId to false)
                    _contadores.value = _contadores.value + (avistamientoId to maxOf(0, contadorActual - 1))
                } else {
                    AvistamientoRepository.validarAvistamiento(avistamientoId, uid)
                    _validaciones.value = _validaciones.value + (avistamientoId to true)
                    _contadores.value = _contadores.value + (avistamientoId to contadorActual + 1)
                }
            } catch (e: Exception) {
                android.util.Log.e("SafeWalk", "Error al validar: ${e.message}", e)
            } finally {
                _cargando.value = _cargando.value - avistamientoId
            }
        }
    }

    fun esPropioReporte(usuarioIdReporte: String): Boolean {
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
        return uid == usuarioIdReporte
    }
}