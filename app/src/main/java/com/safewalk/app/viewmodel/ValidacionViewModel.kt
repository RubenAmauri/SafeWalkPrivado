package com.safewalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safewalk.app.SupabaseClient
import com.safewalk.app.repository.AvistamientoRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Job
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

    // Tracks in-flight cargarValidacion jobs so they can be cancelled before a toggle writes state
    private val cargaJobs = mutableMapOf<String, Job>()

    // IDs where the user has already toggled locally — cargarValidacion must not overwrite these
    private val localmenteModificados = mutableSetOf<String>()

    fun cargarValidacion(avistamientoId: String, contadorInicial: Int, contadorYaNoEstaInicial: Int = 0) {
        // Cancel any previous in-flight fetch for this ID to avoid duplicate stale writes
        cargaJobs[avistamientoId]?.cancel()
        cargaJobs[avistamientoId] = viewModelScope.launch {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
            val tipo = AvistamientoRepository.obtenerValidacionUsuario(avistamientoId, uid)
            // Never overwrite state that the user has already modified locally in this session
            if (avistamientoId !in localmenteModificados) {
                _validaciones.value = _validaciones.value + (avistamientoId to tipo)
            }
            if (!_contadores.value.containsKey(avistamientoId)) {
                _contadores.value = _contadores.value + (avistamientoId to contadorInicial)
            }
            if (!_contadoresYaNoEsta.value.containsKey(avistamientoId)) {
                _contadoresYaNoEsta.value = _contadoresYaNoEsta.value + (avistamientoId to contadorYaNoEstaInicial)
            }
        }
    }

    fun registrarOToggle(avistamientoId: String, tipo: String) {
        // Cancel any in-flight cargarValidacion so a stale DB read can't race against our write
        cargaJobs[avistamientoId]?.cancel()
        cargaJobs.remove(avistamientoId)
        // Mark as locally modified before launching so cargarValidacion called from any
        // recomposition triggered by this action will not overwrite the result
        localmenteModificados.add(avistamientoId)

        viewModelScope.launch {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
            val tipoActual = _validaciones.value[avistamientoId]
            val contadorSigueAhi = _contadores.value[avistamientoId] ?: 0
            val contadorYaNoEsta = _contadoresYaNoEsta.value[avistamientoId] ?: 0

            _cargando.value = _cargando.value + avistamientoId
            try {
                if (tipoActual == tipo) {
                    // Retirar validación — only decrement DB if counter is above zero
                    AvistamientoRepository.eliminarValidacion(avistamientoId, uid)
                    _validaciones.value = _validaciones.value + (avistamientoId to null)
                    if (tipo == "sigue_ahi" && contadorSigueAhi > 0) {
                        AvistamientoRepository.actualizarContador(avistamientoId, -1)
                        _contadores.value = _contadores.value + (avistamientoId to contadorSigueAhi - 1)
                    } else if (tipo == "ya_no_esta" && contadorYaNoEsta > 0) {
                        AvistamientoRepository.actualizarContadorYaNoEsta(avistamientoId, -1)
                        _contadoresYaNoEsta.value = _contadoresYaNoEsta.value + (avistamientoId to contadorYaNoEsta - 1)
                    }
                } else {
                    // Cambiar o agregar validación
                    AvistamientoRepository.registrarValidacion(avistamientoId, uid, tipo)
                    _validaciones.value = _validaciones.value + (avistamientoId to tipo)

                    if (tipoActual == "sigue_ahi") {
                        if (contadorSigueAhi > 0) {
                            AvistamientoRepository.actualizarContador(avistamientoId, -1)
                            _contadores.value = _contadores.value + (avistamientoId to contadorSigueAhi - 1)
                        }
                        AvistamientoRepository.actualizarContadorYaNoEsta(avistamientoId, 1)
                        _contadoresYaNoEsta.value = _contadoresYaNoEsta.value + (avistamientoId to contadorYaNoEsta + 1)
                    } else if (tipoActual == "ya_no_esta") {
                        if (contadorYaNoEsta > 0) {
                            AvistamientoRepository.actualizarContadorYaNoEsta(avistamientoId, -1)
                            _contadoresYaNoEsta.value = _contadoresYaNoEsta.value + (avistamientoId to contadorYaNoEsta - 1)
                        }
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
