package com.safewalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safewalk.app.model.Reporte
import com.safewalk.app.repository.ReporteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ReporteState {
    object Idle : ReporteState()
    object Loading : ReporteState()
    object Success : ReporteState()
    data class Error(val message: String) : ReporteState()
}

class ReporteViewModel : ViewModel() {

    private val _estado = MutableStateFlow<ReporteState>(ReporteState.Idle)
    val estado: StateFlow<ReporteState> = _estado

    fun enviarDenuncia(reporteId: String, userId: String, motivo: String, detalles: String) {
        viewModelScope.launch {
            _estado.value = ReporteState.Loading

            val nuevoReporte = Reporte(
                reporteId = reporteId,
                usuarioId = userId,
                motivo = motivo,
                detalles = detalles
            )

            val resultado = ReporteRepository.enviarDenuncia(reporte = nuevoReporte)

            resultado.onSuccess {
                _estado.value = ReporteState.Success
            }.onFailure { error ->
                _estado.value = ReporteState.Error(error.message ?: "Error desconocido")
            }
        }
    }
}