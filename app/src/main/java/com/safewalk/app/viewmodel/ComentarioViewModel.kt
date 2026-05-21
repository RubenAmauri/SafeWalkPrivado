package com.safewalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safewalk.app.SupabaseClient
import com.safewalk.app.model.Comentario
import com.safewalk.app.repository.AvistamientoRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ComentarioViewModel : ViewModel() {

    private val _comentarios = MutableStateFlow<List<Comentario>>(emptyList())
    val comentarios: StateFlow<List<Comentario>> = _comentarios

    private val _enviando = MutableStateFlow(false)
    val enviando: StateFlow<Boolean> = _enviando

    fun currentUserId(): String? =
        SupabaseClient.client.auth.currentUserOrNull()?.id

    fun cargarComentarios(avistamientoId: String) {
        viewModelScope.launch {
            _comentarios.value = AvistamientoRepository.getComentarios(avistamientoId)
        }
    }

    fun agregarComentario(avistamientoId: String, texto: String) {
        viewModelScope.launch {
            _enviando.value = true
            val nuevo = AvistamientoRepository.agregarComentario(avistamientoId, texto)
            if (nuevo != null) {
                _comentarios.value = _comentarios.value + nuevo
            }
            _enviando.value = false
        }
    }

    fun editarComentario(comentarioId: String, nuevoTexto: String) {
        viewModelScope.launch {
            _enviando.value = true
            val actualizado = AvistamientoRepository.editarComentario(comentarioId, nuevoTexto)
            if (actualizado != null) {
                _comentarios.value = _comentarios.value.map {
                    if (it.id == comentarioId) actualizado else it
                }
            }
            _enviando.value = false
        }
    }

    fun eliminarComentario(comentarioId: String) {
        viewModelScope.launch {
            AvistamientoRepository.eliminarComentario(comentarioId)
            _comentarios.value = _comentarios.value.filter { it.id != comentarioId }
        }
    }
}