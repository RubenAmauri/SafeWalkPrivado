package com.safewalk.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.LatLng
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.NivelAgresividad
import com.safewalk.app.viewmodel.CrearAvistamientoViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CrearReporteScreen(
    crearViewModel: CrearAvistamientoViewModel,
    ubicacionPreseleccionada: LatLng?,
    onGuardar: (Avistamiento) -> Unit,
    onSeleccionarUbicacion: () -> Unit,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    var descripcion by remember { mutableStateOf<String>(crearViewModel.descripcion) }
    var nivelSeleccionado by remember { mutableStateOf<NivelAgresividad?>(crearViewModel.nivel) }
    var mostrarErrorDescripcion by remember { mutableStateOf(false) }
    var mostrarErrorNivel by remember { mutableStateOf(false) }
    var mostrarErrorUbicacion by remember { mutableStateOf(false) }
    var direccionPreview by remember { mutableStateOf<String?>(null) }

    val fotoUri = crearViewModel.fotoUri
    val fotoError = crearViewModel.fotoError

    // Launcher para galería
    val launcherGaleria = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { crearViewModel.validarYAsignarFoto(it, context) }
    }

    // Launcher para cámara
    val launcherCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            // Convertir bitmap a URI temporal
            val stream = java.io.ByteArrayOutputStream()
            it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, stream)
            val byteArray = stream.toByteArray()
            val tempFile = java.io.File(context.cacheDir, "temp_foto.jpg")
            tempFile.writeBytes(byteArray)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempFile
            )
            crearViewModel.validarYAsignarFoto(uri, context)
        }
    }

    var mostrarMenuFoto by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        Surface(color = Color(0xFF1F3864)) {
            Column {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onRegresar) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Crear reporte de avistamiento",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Descripción
            Text("Descripción *", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = descripcion,
                onValueChange = {
                    descripcion = it
                    crearViewModel.descripcion = it
                    mostrarErrorDescripcion = false
                },
                placeholder = { Text("¿Cuántos perros viste? ¿Qué estaban haciendo?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                isError = mostrarErrorDescripcion
            )
            if (mostrarErrorDescripcion) {
                Text("La descripción es obligatoria.", color = Color.Red, fontSize = 12.sp)
            }

            // Nivel de agresividad
            Text("Nivel de agresividad *", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NivelAgresividad.entries.forEach { nivel ->
                    val (etiqueta, color) = when (nivel) {
                        NivelAgresividad.BAJO -> "Bajo" to Color(0xFF4CAF50)
                        NivelAgresividad.MEDIO -> "Medio" to Color(0xFFFF9800)
                        NivelAgresividad.ALTO -> "Alto" to Color(0xFFF44336)
                    }
                    FilterChip(
                        selected = nivelSeleccionado == nivel,
                        onClick = {
                            nivelSeleccionado = nivel
                            crearViewModel.nivel = nivel
                            mostrarErrorNivel = false
                        },
                        label = { Text(etiqueta) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f),
                            selectedLabelColor = color
                        )
                    )
                }
            }
            if (mostrarErrorNivel) {
                Text("Selecciona un nivel de agresividad.", color = Color.Red, fontSize = 12.sp)
            }

            // Ubicación
            Text("Ubicación *", fontWeight = FontWeight.SemiBold)
            OutlinedButton(
                onClick = onSeleccionarUbicacion,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        direccionPreview != null -> direccionPreview!!
                        ubicacionPreseleccionada != null -> "%.4f, %.4f".format(
                            ubicacionPreseleccionada.latitude,
                            ubicacionPreseleccionada.longitude
                        )
                        else -> "Seleccionar en el mapa"
                    }
                )
            }
            LaunchedEffect(ubicacionPreseleccionada) {
                ubicacionPreseleccionada?.let { latLng ->
                    val geocoder = android.location.Geocoder(context, Locale("es", "MX"))
                    try {
                        val resultados = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                        if (!resultados.isNullOrEmpty()) {
                            val dir = resultados[0]
                            direccionPreview = listOfNotNull(
                                dir.thoroughfare,
                                dir.subLocality,
                                dir.locality
                            ).joinToString(", ")
                        }
                    } catch (e: Exception) {
                        direccionPreview = null
                    }
                }
            }
            if (mostrarErrorUbicacion) {
                Text("Selecciona una ubicación en el mapa.", color = Color.Red, fontSize = 12.sp)
            }

            // Foto (opcional)
            Text("Foto (opcional)", fontWeight = FontWeight.SemiBold)
            Box {
                OutlinedButton(
                    onClick = { mostrarMenuFoto = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (fotoUri != null) "Cambiar foto" else "Adjuntar foto")
                }
                DropdownMenu(
                    expanded = mostrarMenuFoto,
                    onDismissRequest = { mostrarMenuFoto = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tomar foto") },
                        onClick = {
                            mostrarMenuFoto = false
                            launcherCamara.launch(null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Elegir de galería") },
                        onClick = {
                            mostrarMenuFoto = false
                            launcherGaleria.launch("image/*")
                        }
                    )
                }
            }
            fotoError?.let {
                Text(it, color = Color.Red, fontSize = 12.sp)
            }
            fotoUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Foto seleccionada",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón guardar
            Button(
                onClick = {
                    mostrarErrorDescripcion = descripcion.isBlank()
                    mostrarErrorNivel = nivelSeleccionado == null
                    mostrarErrorUbicacion = ubicacionPreseleccionada == null

                    if (!mostrarErrorDescripcion && !mostrarErrorNivel && !mostrarErrorUbicacion) {
                        val fecha = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "MX"))
                            .format(Date())

                        val geocoder = android.location.Geocoder(context, Locale("es", "MX"))
                        val direccionFinal = direccionPreview ?: try {
                            val resultados = geocoder.getFromLocation(
                                ubicacionPreseleccionada!!.latitude,
                                ubicacionPreseleccionada!!.longitude,
                                1
                            )
                            if (!resultados.isNullOrEmpty()) {
                                val dir = resultados[0]
                                listOfNotNull(
                                    dir.thoroughfare,
                                    dir.subLocality,
                                    dir.locality
                                ).joinToString(", ")
                            } else {
                                "%.4f, %.4f".format(
                                    ubicacionPreseleccionada!!.latitude,
                                    ubicacionPreseleccionada!!.longitude
                                )
                            }
                        } catch (e: Exception) {
                            "%.4f, %.4f".format(
                                ubicacionPreseleccionada!!.latitude,
                                ubicacionPreseleccionada!!.longitude
                            )
                        }

                        val nuevo = Avistamiento(
                            id = UUID.randomUUID().toString(),
                            latitud = ubicacionPreseleccionada!!.latitude,
                            longitud = ubicacionPreseleccionada!!.longitude,
                            nivelAgresividad = nivelSeleccionado!!,
                            descripcion = descripcion,
                            fechaCreacion = fecha,
                            ubicacionAproximada = direccionFinal
                        )
                        onGuardar(nuevo)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F3864))
            ) {
                Text("Guardar reporte", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}