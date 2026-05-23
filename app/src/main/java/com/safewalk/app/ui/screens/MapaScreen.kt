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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safewalk.app.viewmodel.ValidacionViewModel
import com.google.maps.android.compose.Polyline

@SuppressLint("MissingPermission")
@Composable
fun MapaScreen(
    mapaViewModel: MapaViewModel,
    onCrearReporte: () -> Unit = {},
    onReportar: (Avistamiento) -> Unit = {},
    onVerDetalle: (Avistamiento) -> Unit = {},
    onIrAZonasFrecuentes: () -> Unit = {},
    validacionViewModel: ValidacionViewModel = viewModel()
) {
    val context = LocalContext.current
    val zonas by mapaViewModel.zonas.collectAsState()
    val zonaSeleccionada by mapaViewModel.zonaSeleccionada.collectAsState()
    val avistamientoMarcado by mapaViewModel.avistamientoMarcado.collectAsState()
    val avistamientoMasCercanoId by mapaViewModel.avistamientoMasCercanoId.collectAsState()
    val avisoZonasFrecuentes by mapaViewModel.avisoZonasFrecuentes.collectAsState()

    val zacatecas = LatLng(22.7709, -102.5832)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(zacatecas, 13f)
    }
    val zonaMasCercana by mapaViewModel.zonaMasCercana.collectAsState()
    var ubicacionUsuario by remember { mutableStateOf<LatLng?>(null) }

    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(tienePermiso) {
        if (tienePermiso) {
            val fusedLocation = LocationServices.getFusedLocationProviderClient(context)
            fusedLocation.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val pos = LatLng(location.latitude, location.longitude)
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> tienePermiso = granted }

    LaunchedEffect(Unit) {
        if (!tienePermiso) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        val ubicacionPendiente by mapaViewModel.ubicacionPendiente.collectAsState()
        LaunchedEffect(ubicacionPendiente) {
            ubicacionPendiente?.let { (lat, lng) ->
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 17f))
                mapaViewModel.consumirUbicacionPendiente()
            }
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = tienePermiso),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false)
        ) {
            zonas.forEach { zona ->
                val color = colorZona(zona.nivelPromedio)
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
            zonaMasCercana?.let { (zona, _) ->
                ubicacionUsuario?.let { userPos ->
                    Polyline(
                        points = listOf(userPos, zona.centro),
                        color = Color(0xFF1F3864),
                        width = 6f,
                        pattern = listOf(
                            com.google.android.gms.maps.model.Dash(20f),
                            com.google.android.gms.maps.model.Gap(10f)
                        )
                    )
                }
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
                            ubicacionUsuario = pos
                            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                            mapaViewModel.calcularZonaMasCercana(location.latitude, location.longitude)
                        } else {
                            Toast.makeText(context, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "Activa el permiso de ubicación en Ajustes para usar esta función", Toast.LENGTH_LONG).show()
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

        // Botón Zonas frecuentes
        FloatingActionButton(
            onClick = onIrAZonasFrecuentes,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    bottom = if (zonaSeleccionada != null) 300.dp else 144.dp,
                    start = 16.dp
                ),
            containerColor = Color(0xFF1F3864)
        ) {
            Icon(Icons.Default.Star, contentDescription = "Zonas frecuentes", tint = Color.White)
        }

        // Card de detalle
        zonaSeleccionada?.let { zona ->
            DetalleZona(
                zona = zona,
                onCerrar = { mapaViewModel.cerrarDetalle() },
                onReportar = onReportar,
                onVerDetalle = onVerDetalle,
                validacionViewModel = validacionViewModel,
                avistamientoMasCercanoId = avistamientoMasCercanoId,
                onVerUbicacion = { latLng, avistamiento ->
                    mapaViewModel.marcarAvistamiento(avistamiento)
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        //Visualizador de la distancia a la zona más cercana
        zonaMasCercana?.let { (zona, distanciaMetros) ->
            val textoDistancia = if (distanciaMetros >= 1000) {
                "%.1f km a zona más cercana".format(distanciaMetros / 1000)
            } else {
                "${distanciaMetros.toInt()} m a zona más cercana"
            }
            val colorZona = colorZona(zona.nivelPromedio)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clickable { mapaViewModel.limpiarZonaMasCercana() },
                shape = RoundedCornerShape(50),
                color = Color(0xFF1F3864),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colorZona, RoundedCornerShape(50))
                    )
                    Text(
                        text = textoDistancia,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("✕", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
            if (avisoZonasFrecuentes > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { mapaViewModel.limpiarAvisoZonasFrecuentes() },
                        containerColor = Color(0xFF1F3864),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color.Red, RoundedCornerShape(50))
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = if (avisoZonasFrecuentes > 9) "9+" else "$avisoZonasFrecuentes",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetalleZona(
    zona: ZonaAvistamiento,
    onCerrar: () -> Unit,
    onReportar: (Avistamiento) -> Unit,
    onVerDetalle: (Avistamiento) -> Unit,
    validacionViewModel: ValidacionViewModel,
    avistamientoMasCercanoId: String?,
    onVerUbicacion: (LatLng, Avistamiento) -> Unit,
    modifier: Modifier = Modifier
) {
    val avistamientosOrdenados = remember(zona.avistamientos, avistamientoMasCercanoId) {
        if (avistamientoMasCercanoId != null) {
            val cercano = zona.avistamientos.find { it.id == avistamientoMasCercanoId }
            val resto = zona.avistamientos.filter { it.id != avistamientoMasCercanoId }
            if (cercano != null) listOf(cercano) + resto else zona.avistamientos
        } else zona.avistamientos
    }
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
                items(avistamientosOrdenados) { avistamiento ->
                    ItemAvistamiento(
                        avistamiento = avistamiento,
                        onVerUbicacion = onVerUbicacion,
                        onReportar = onReportar,
                        onVerDetalle = onVerDetalle,
                        validacionViewModel = validacionViewModel,
                        esMasCercano = avistamiento.id == avistamientoMasCercanoId  // <- agrega
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
    onReportar: (Avistamiento) -> Unit,
    onVerDetalle: (Avistamiento) -> Unit,
    validacionViewModel: ValidacionViewModel,
    esMasCercano: Boolean = false
) {
    var mostrarMenu by remember { mutableStateOf(false) }
    val contadores by validacionViewModel.contadores.collectAsState()
    val contadoresYaNoEsta by validacionViewModel.contadoresYaNoEsta.collectAsState()
    val contador = contadores[avistamiento.id] ?: avistamiento.totalConfirmaciones
    val contadorYaNoEsta = contadoresYaNoEsta[avistamiento.id] ?: avistamiento.totalYaNoEsta

    LaunchedEffect(avistamiento.id) {
        validacionViewModel.cargarValidacion(
            avistamiento.id,
            avistamiento.totalConfirmaciones,
            avistamiento.totalYaNoEsta
        )
    }

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
        if (esMasCercano) {
            Surface(
                color = Color(0xFF1F3864),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    "📍 Más cercano",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
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
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("$contador", fontSize = 11.sp, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("$contadorYaNoEsta", fontSize = 11.sp, color = Color.Gray)
                }
            }
            TextButton(
                onClick = { onVerDetalle(avistamiento) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Ver detalle", fontSize = 11.sp, color = Color(0xFF1F3864))
            }
        }
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