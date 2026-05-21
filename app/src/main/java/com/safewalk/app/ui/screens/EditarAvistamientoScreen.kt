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
import androidx.compose.material.icons.filled.Delete
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.safewalk.app.model.FotoInfo
import com.safewalk.app.model.NivelAgresividad
import com.safewalk.app.repository.AvistamientoRepository
import com.safewalk.app.viewmodel.HistorialViewModel
import java.util.Locale

@Composable
fun EditarAvistamientoScreen(
    avistamiento: Avistamiento,
    viewModel: HistorialViewModel,
    ubicacionPreseleccionada: LatLng?,
    onSeleccionarUbicacion: () -> Unit,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val guardadoExitoso by viewModel.guardadoExitoso.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()

    var descripcion by remember { mutableStateOf(avistamiento.descripcion) }
    var nivelSeleccionado by remember { mutableStateOf(avistamiento.nivelAgresividad) }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var mostrarMenuFoto by remember { mutableStateOf(false) }
    var fotosActuales by remember { mutableStateOf<List<FotoInfo>>(emptyList()) }
    var fotosAEliminar by remember { mutableStateOf<List<FotoInfo>>(emptyList()) }
    var direccionPreview by remember { mutableStateOf<String?>(null) }

    val ubicacionActual = ubicacionPreseleccionada
        ?: LatLng(avistamiento.latitud, avistamiento.longitud)

    LaunchedEffect(Unit) {
        fotosActuales = AvistamientoRepository.getFotosConId(avistamiento.id)
    }

    LaunchedEffect(guardadoExitoso) {
        if (guardadoExitoso) {
            viewModel.limpiarEdicion()
            onRegresar()
        }
    }

    LaunchedEffect(ubicacionPreseleccionada) {
        ubicacionPreseleccionada?.let { latLng ->
            val geocoder = android.location.Geocoder(context, Locale("es", "MX"))
            try {
                val resultados = withContext(Dispatchers.IO) {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                }
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

    val launcherGaleria = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { fotoUri = it } }

    val launcherCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val stream = java.io.ByteArrayOutputStream()
            it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, stream)
            val tempFile = java.io.File(context.cacheDir, "temp_foto_editar.jpg")
            tempFile.writeBytes(stream.toByteArray())
            fotoUri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.provider", tempFile
            )
        }
    }

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
                        text = "Editar reporte",
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
                onValueChange = { descripcion = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Nivel
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
                        onClick = { nivelSeleccionado = nivel },
                        label = { Text(etiqueta) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f),
                            selectedLabelColor = color
                        )
                    )
                }
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
                    text = direccionPreview
                        ?: if (ubicacionPreseleccionada != null)
                            "%.4f, %.4f".format(ubicacionPreseleccionada.latitude, ubicacionPreseleccionada.longitude)
                        else avistamiento.ubicacionAproximada
                )
            }

            // Fotos actuales
            val fotosMostradas = fotosActuales.filter { it !in fotosAEliminar }
            if (fotosMostradas.isNotEmpty()) {
                Text("Fotos actuales", fontWeight = FontWeight.SemiBold)
                fotosMostradas.forEach { foto ->
                    Box {
                        AsyncImage(
                            model = foto.url,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { fotosAEliminar = fotosAEliminar + foto },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar foto",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }

            // Nueva foto
            Text("Agregar foto", fontWeight = FontWeight.SemiBold)
            Box {
                OutlinedButton(
                    onClick = { mostrarMenuFoto = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (fotoUri != null) "Cambiar foto nueva" else "Adjuntar foto")
                }
                DropdownMenu(
                    expanded = mostrarMenuFoto,
                    onDismissRequest = { mostrarMenuFoto = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tomar foto") },
                        onClick = { mostrarMenuFoto = false; launcherCamara.launch(null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Elegir de galería") },
                        onClick = { mostrarMenuFoto = false; launcherGaleria.launch("image/*") }
                    )
                }
            }
            fotoUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Nueva foto",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            error?.let {
                Text(it, color = Color.Red, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    if (descripcion.isNotBlank()) {
                        val ubicacionFinal = ubicacionPreseleccionada
                            ?: LatLng(avistamiento.latitud, avistamiento.longitud)
                        val direccionFinal = direccionPreview ?: avistamiento.ubicacionAproximada

                        viewModel.guardarEdicion(
                            avistamientoId = avistamiento.id,
                            descripcion = descripcion,
                            agresividad = nivelSeleccionado.name.lowercase(),
                            ubicacionAproximada = direccionFinal,
                            latitud = ubicacionFinal.latitude,
                            longitud = ubicacionFinal.longitude,
                            fotoUri = fotoUri,
                            fotosAEliminar = fotosAEliminar,
                            context = context
                        )
                    }
                },
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F3864))
            ) {
                if (cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Guardar cambios", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}