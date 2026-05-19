package com.safewalk.app.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safewalk.app.SupabaseClient
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.NivelAgresividad
import com.safewalk.app.repository.AvistamientoRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class CrearAvistamientoViewModel : ViewModel() {

    // Estado del formulario
    var descripcion by mutableStateOf("")
    var nivel by mutableStateOf<NivelAgresividad?>(null)
    var fotoUri by mutableStateOf<Uri?>(null)
    var fotoError by mutableStateOf<String?>(null)

    // Estado de guardado
    private val _guardando = MutableStateFlow(false)
    val guardando: StateFlow<Boolean> = _guardando

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _guardadoExitoso = MutableStateFlow(false)
    val guardadoExitoso: StateFlow<Boolean> = _guardadoExitoso

    fun validarYAsignarFoto(uri: Uri, context: Context) {
        fotoError = null

        val mimeType = context.contentResolver.getType(uri)
        val tiposPermitidos = listOf("image/jpeg", "image/png", "image/webp")
        if (mimeType !in tiposPermitidos) {
            fotoError = "Solo se permiten imágenes en formato JPG, PNG o WEBP"
            return
        }

        // Comprimir primero
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val bytesComprimidos = outputStream.toByteArray()

        // Validar tamaño después de comprimir
        val maxBytes = 5 * 1024 * 1024 // 5MB
        if (bytesComprimidos.size > maxBytes) {
            fotoError = "La imagen supera 5MB incluso después de comprimir"
            return
        }

        fotoUri = uri
    }

    fun limpiarFormulario() {
        descripcion = ""
        nivel = null
        fotoUri = null
        fotoError = null
        _error.value = null
        _guardadoExitoso.value = false
    }

    fun guardarReporte(avistamiento: Avistamiento, context: Context) {
        viewModelScope.launch {
            _guardando.value = true
            _error.value = null
            try {
                val avistamientoId = AvistamientoRepository.agregarAvistamiento(avistamiento)

                fotoUri?.let { uri ->
                    val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
                    if (uid != null) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val bytes = outputStream.toByteArray()
                        val url = AvistamientoRepository.subirFoto(
                            avistamientoId = avistamientoId,
                            usuarioId = uid,
                            imagenBytes = bytes
                        )
                        android.util.Log.d("SafeWalk", "Foto subida, URL: $url")
                    }
                }

                _guardadoExitoso.value = true
            } catch (e: Exception) {
                _error.value = "Error al guardar: ${e.message}"
                android.util.Log.e("SafeWalk", "Error al guardar reporte: ${e.message}", e)
            } finally {
                _guardando.value = false
            }
        }
    }
}