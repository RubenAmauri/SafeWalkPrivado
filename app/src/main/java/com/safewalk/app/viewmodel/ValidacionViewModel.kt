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

    private val _validaciones = MutableStateFlow<Map<String, String?>>(emptyMap())
    val validaciones: StateFlow<Map<String, String?>> = _validaciones

    private val _contadores = MutableStateFlow<Map<String, Int>>(emptyMap())
    val contadores: StateFlow<Map<String, Int>> = _contadores

    private val _contadoresYaNoEsta = MutableStateFlow<Map<String, Int>>(emptyMap())
    val contadoresYaNoEsta: StateFlow<Map<String, Int>> = _contadoresYaNoEsta

    private val _cargando = MutableStateFlow<Set<String>>(emptySet())
    val cargando: StateFlow<Set<String>> = _cargando

    fun cargarValidacion(avistamientoId: String, contadorInicial: Int, contadorYaNoEstaInicial: Int = 0) {
        viewModelScope.launch {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
            val tipo = AvistamientoRepository.obtenerValidacionUsuario(avistamientoId, uid)
            _validaciones.value = _validaciones.value + (avistamientoId to tipo)
            // Solo inicializar si no tenemos ya un valor local
            if (!_contadores.value.containsKey(avistamientoId)) {
                _contadores.value = _contadores.value + (avistamientoId to contadorInicial)
            }
            if (!_contadoresYaNoEsta.value.containsKey(avistamientoId)) {
                _contadoresYaNoEsta.value = _contadoresYaNoEsta.value + (avistamientoId to contadorYaNoEstaInicial)
            }
        }
    }

    fun registrarOToggle(avistamientoId: String, tipo: String) {
        viewModelScope.launch {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
            val tipoActual = _validaciones.value[avistamientoId]
            val contadorSigueAhi = _contadores.value[avistamientoId] ?: 0
            val contadorYaNoEsta = _contadoresYaNoEsta.value[avistamientoId] ?: 0

            _cargando.value = _cargando.value + avistamientoId
            try {
                if (tipoActual == tipo) {
                    // Retirar validación
                    AvistamientoRepository.eliminarValidacion(avistamientoId, uid)
                    _validaciones.value = _validaciones.value + (avistamientoId to null)
                    if (tipo == "sigue_ahi") {
                        AvistamientoRepository.actualizarContador(avistamientoId, -1)
                        _contadores.value = _contadores.value + (avistamientoId to maxOf(0, contadorSigueAhi - 1))
                    } else {
                        AvistamientoRepository.actualizarContadorYaNoEsta(avistamientoId, -1)
                        _contadoresYaNoEsta.value = _contadoresYaNoEsta.value + (avistamientoId to maxOf(0, contadorYaNoEsta - 1))
                    }
                } else {
                    // Cambiar o agregar validación
                    AvistamientoRepository.registrarValidacion(avistamientoId, uid, tipo)
                    _validaciones.value = _validaciones.value + (avistamientoId to tipo)

                    // Si cambia de tipo, ajustar ambos contadores
                    if (tipoActual == "sigue_ahi") {
                        AvistamientoRepository.actualizarContador(avistamientoId, -1)
                        _contadores.value = _contadores.value + (avistamientoId to maxOf(0, contadorSigueAhi - 1))
                        AvistamientoRepository.actualizarContadorYaNoEsta(avistamientoId, 1)
                        _contadoresYaNoEsta.value = _contadoresYaNoEsta.value + (avistamientoId to contadorYaNoEsta + 1)
                    } else if (tipoActual == "ya_no_esta") {
                        AvistamientoRepository.actualizarContadorYaNoEsta(avistamientoId, -1)
                        _contadoresYaNoEsta.value = _contadoresYaNoEsta.value + (avistamientoId to maxOf(0, contadorYaNoEsta - 1))
                        AvistamientoRepository.actualizarContador(avistamientoId, 1)
                        _contadores.value = _contadores.value + (avistamientoId to contadorSigueAhi + 1)
                    } else {
                        // Primera vez
                        if (tipo == "sigue_ahi") {
                            AvistamientoRepository.actualizarContador(avistamientoId, 1)
                            _contadores.value = _contadores.value + (avistamientoId to contadorSigueAhi + 1)
                        } else {
                            AvistamientoRepository.actualizarContadorYaNoEsta(avistamientoId, 1)
                            _contadoresYaNoEsta.value = _contadoresYaNoEsta.value + (avistamientoId to contadorYaNoEsta + 1)
                        }
                    }
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