package com.safewalk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.NivelAgresividad
import com.safewalk.app.viewmodel.MapaViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CrearReporteScreen(
    mapaViewModel: MapaViewModel,
    ubicacionPreseleccionada: LatLng?,
    onGuardar: (Avistamiento) -> Unit,
    onSeleccionarUbicacion: () -> Unit,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    var descripcion by remember { mutableStateOf<String>(mapaViewModel.descripcionReporte) }
    var nivelSeleccionado by remember { mutableStateOf<NivelAgresividad?>(mapaViewModel.nivelReporte) }
    var mostrarErrorDescripcion by remember { mutableStateOf(false) }
    var mostrarErrorNivel by remember { mutableStateOf(false) }
    var mostrarErrorUbicacion by remember { mutableStateOf(false) }
    var direccionPreview by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {

        // Header
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
                    mapaViewModel.descripcionReporte = it
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
                            mapaViewModel.nivelReporte = nivel
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

            Spacer(modifier = Modifier.height(8.dp))

            // Botón guardar
            Button(
                onClick = {
                    android.util.Log.d("SafeWalk", "Botón guardar presionado - desc: '$descripcion', nivel: $nivelSeleccionado, ubicacion: $ubicacionPreseleccionada")
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
                                listOfNotNull(dir.thoroughfare, dir.subLocality, dir.locality).joinToString(", ")
                            } else {
                                "%.4f, %.4f".format(ubicacionPreseleccionada!!.latitude, ubicacionPreseleccionada!!.longitude)
                            }
                        } catch (e: Exception) {
                            "%.4f, %.4f".format(ubicacionPreseleccionada!!.latitude, ubicacionPreseleccionada!!.longitude)
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