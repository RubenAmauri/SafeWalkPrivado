package com.safewalk.app.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safewalk.app.SupabaseClient
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.FotoInfo
import com.safewalk.app.repository.AvistamientoRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class HistorialViewModel : ViewModel() {

    private val _reportes = MutableStateFlow<List<Avistamiento>>(emptyList())
    val reportes: StateFlow<List<Avistamiento>> = _reportes

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _avistamientoParaEditar = MutableStateFlow<Avistamiento?>(null)
    val avistamientoParaEditar: StateFlow<Avistamiento?> = _avistamientoParaEditar

    private val _guardadoExitoso = MutableStateFlow(false)
    val guardadoExitoso: StateFlow<Boolean> = _guardadoExitoso

    init { cargarMisReportes() }

    fun seleccionarParaEditar(avistamiento: Avistamiento) {
        _avistamientoParaEditar.value = avistamiento
    }

    fun limpiarEdicion() {
        _avistamientoParaEditar.value = null
        _guardadoExitoso.value = false
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
                        filter { eq("usuario_id", uid) }
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

    fun guardarEdicion(
        avistamientoId: String,
        descripcion: String,
        agresividad: String,
        ubicacionAproximada: String,
        latitud: Double,
        longitud: Double,
        fotoUri: Uri?,
        fotosAEliminar: List<FotoInfo>,
        context: Context
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch

                // Editar campos base
                AvistamientoRepository.editarAvistamientoCompleto(
                    avistamientoId = avistamientoId,
                    descripcion = descripcion,
                    agresividad = agresividad,
                    ubicacionAproximada = ubicacionAproximada,
                    latitud = latitud,
                    longitud = longitud
                )

                // Eliminar fotos marcadas
                fotosAEliminar.forEach { foto ->
                    AvistamientoRepository.eliminarFoto(foto.fotoId, avistamientoId, uid)
                }

                // Subir nueva foto si hay
                fotoUri?.let { uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val bytes = outputStream.toByteArray()
                    AvistamientoRepository.subirFoto(avistamientoId, uid, bytes)
                }

                _guardadoExitoso.value = true
                cargarMisReportes()
            } catch (e: Exception) {
                _error.value = "Error al guardar: ${e.message}"
                android.util.Log.e("SafeWalk", "Error guardarEdicion: ${e.message}", e)
            } finally {
                _cargando.value = false
            }
        }
    }

}