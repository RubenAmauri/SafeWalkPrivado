package com.safewalk.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.safewalk.app.model.Comentario

class ComentarioViewModel : ViewModel() {

    private val _comentarios = MutableStateFlow<List<Comentario>>(emptyList())
    val comentarios: StateFlow<List<Comentario>> = _comentarios

    fun cargarComentarios(avistamientoId: String) {
        // Simulación por ahora
        _comentarios.value = listOf(
            Comentario("1", avistamientoId, "Usuario1", "Cuidado con este perro", "Hace 1h")
        )
    }

    fun agregarComentario(avistamientoId: String, texto: String) {
        val nuevo = Comentario(
            id = System.currentTimeMillis().toString(),
            avistamientoId = avistamientoId,
            usuario = "Usuario actual",
            texto = texto,
            fecha = "Ahora"
        )

        _comentarios.value = _comentarios.value + nuevo
    }
}