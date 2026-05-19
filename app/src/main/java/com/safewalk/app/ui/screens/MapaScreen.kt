package com.safewalk.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.NivelAgresividad
import com.safewalk.app.model.ZonaAvistamiento
import com.safewalk.app.util.formatearFecha
import com.safewalk.app.viewmodel.MapaViewModel
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Report
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.core.app.ActivityCompat

@SuppressLint("MissingPermission")
@Composable
fun MapaScreen(
    mapaViewModel: MapaViewModel,
    onCrearReporte: () -> Unit = {},
    onReportar: (Avistamiento) -> Unit = {}
) {
    val context = LocalContext.current
    val zonas by mapaViewModel.zonas.collectAsState()
    val zonaSeleccionada by mapaViewModel.zonaSeleccionada.collectAsState()
    val avistamientoMarcado by mapaViewModel.avistamientoMarcado.collectAsState()

    val zacatecas = LatLng(22.7709, -102.5832)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(zacatecas, 13f)
    }

    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> tienePermiso = granted }

    LaunchedEffect(Unit) {
        if (!tienePermiso) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = tienePermiso),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false)
        ) {
            zonas.forEach { zona ->
                val color = colorZona(zona.nivelPromedio)
                val ubicacionPendiente by mapaViewModel.ubicacionPendiente.collectAsState()

                LaunchedEffect(ubicacionPendiente) {
                    ubicacionPendiente?.let { (lat, lng) ->
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 17f))
                        mapaViewModel.consumirUbicacionPendiente()
                    }
                }
                Circle(
                    center = zona.centro,
                    radius = 600.0,
                    fillColor = color.copy(alpha = 0.25f),
                    strokeColor = color,
                    strokeWidth = 3f,
                    clickable = true,
                    onClick = { mapaViewModel.seleccionarZona(zona) }
                )
            }
            avistamientoMarcado?.let { avistamiento ->
                Marker(
                    state = MarkerState(
                        position = LatLng(avistamiento.latitud, avistamiento.longitud)
                    ),
                    title = avistamiento.ubicacionAproximada,
                    snippet = avistamiento.descripcion,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        colorMarker(avistamiento.nivelAgresividad)
                    )
                )
            }
        }

        // Botón GPS
        FloatingActionButton(
            onClick = {
                if (tienePermiso) {
                    val fusedLocation = LocationServices.getFusedLocationProviderClient(context)
                    fusedLocation.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val pos = LatLng(location.latitude, location.longitude)
                            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                        } else {
                            Toast.makeText(
                                context,
                                "No se pudo obtener la ubicación actual",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            context as androidx.activity.ComponentActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {
                        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        Toast.makeText(
                            context,
                            "Activa el permiso de ubicación en Ajustes para usar esta función",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    bottom = if (zonaSeleccionada != null) 300.dp else 80.dp,
                    start = 16.dp
                ),
            containerColor = Color(0xFF1F3864)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación", tint = Color.White)
        }

        // Botón Crear reporte
        FloatingActionButton(
            onClick = onCrearReporte,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = if (zonaSeleccionada != null) 300.dp else 16.dp, start = 16.dp),
            containerColor = Color(0xFF1F3864)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Crear reporte", tint = Color.White)
        }

        // Card de detalle
        zonaSeleccionada?.let { zona ->
            DetalleZona(
                zona = zona,
                onCerrar = { mapaViewModel.cerrarDetalle() },
                onReportar = onReportar,
                onVerUbicacion = { latLng, avistamiento ->
                    mapaViewModel.marcarAvistamiento(avistamiento)
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun DetalleZona(
    zona: ZonaAvistamiento,
    onCerrar: () -> Unit,
    onReportar: (Avistamiento) -> Unit,
    onVerUbicacion: (LatLng, Avistamiento) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${zona.avistamientos.size} reporte(s) en esta zona",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                TextButton(onClick = onCerrar) { Text("Cerrar") }
            }
            NivelChip(nivel = zona.nivelPromedio, prefijo = "Zona: ")
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(zona.avistamientos) { avistamiento ->
                    ItemAvistamiento(
                        avistamiento = avistamiento,
                        onVerUbicacion = onVerUbicacion,
                        onReportar = onReportar
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemAvistamiento(
    avistamiento: Avistamiento,
    onVerUbicacion: (LatLng, Avistamiento) -> Unit,
    onReportar: (Avistamiento) -> Unit
) {
    var mostrarMenu by remember { mutableStateOf(false) }

    val fechaFormateada = remember(avistamiento.fechaCreacion) {
        formatearFecha(avistamiento.fechaCreacion)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .clickable {
                onVerUbicacion(LatLng(avistamiento.latitud, avistamiento.longitud), avistamiento)
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = avistamiento.ubicacionAproximada,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                NivelChip(nivel = avistamiento.nivelAgresividad)

                Box {
                    IconButton(onClick = { mostrarMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = mostrarMenu,
                        onDismissRequest = { mostrarMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Report, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reportar contenido", color = Color.Red)
                                }
                            },
                            onClick = {
                                mostrarMenu = false
                                onReportar(avistamiento)
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = avistamiento.descripcion, fontSize = 12.sp)
        Text(text = fechaFormateada, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun NivelChip(nivel: NivelAgresividad, prefijo: String = "") {
    val (texto, color) = when (nivel) {
        NivelAgresividad.BAJO -> "Bajo" to Color(0xFF4CAF50)
        NivelAgresividad.MEDIO -> "Medio" to Color(0xFFFF9800)
        NivelAgresividad.ALTO -> Color(0xFFF44336).let { "Alto" to it }
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = "$prefijo$texto",
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

private fun colorZona(nivel: NivelAgresividad): Color = when (nivel) {
    NivelAgresividad.BAJO -> Color(0xFF4CAF50)
    NivelAgresividad.MEDIO -> Color(0xFFFF9800)
    NivelAgresividad.ALTO -> Color(0xFFF44336)
}

private fun colorMarker(nivel: NivelAgresividad): Float = when (nivel) {
    NivelAgresividad.BAJO -> BitmapDescriptorFactory.HUE_GREEN
    NivelAgresividad.MEDIO -> BitmapDescriptorFactory.HUE_ORANGE
    NivelAgresividad.ALTO -> BitmapDescriptorFactory.HUE_RED
}