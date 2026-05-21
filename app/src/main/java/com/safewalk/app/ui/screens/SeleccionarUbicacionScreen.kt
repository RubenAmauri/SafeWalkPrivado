package com.safewalk.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@SuppressLint("MissingPermission")
@Composable
fun SeleccionarUbicacionScreen(
    onUbicacionSeleccionada: (LatLng) -> Unit,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val zacatecas = LatLng(22.7709, -102.5832)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(zacatecas, 14f)
    }
    var ubicacionSeleccionada by remember { mutableStateOf<LatLng?>(null) }

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

    // Centrar en ubicación del usuario al abrir
    LaunchedEffect(tienePermiso) {
        if (tienePermiso) {
            val fusedLocation = LocationServices.getFusedLocationProviderClient(context)
            fusedLocation.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val pos = LatLng(location.latitude, location.longitude)
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                }
            }
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = tienePermiso),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false),
            onMapClick = { latLng ->
                ubicacionSeleccionada = latLng
            }
        ) {
            ubicacionSeleccionada?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Ubicación seleccionada"
                )
            }
        }

        // Header
        Surface(
            modifier = Modifier.align(Alignment.TopCenter),
            color = Color(0xFF1F3864)
        ) {
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
                        text = "Seleccionar la ubicación del avistamiento",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Instrucción
        var mostrarInstruccion by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(3000)
            mostrarInstruccion = false
        }
        if (ubicacionSeleccionada == null && mostrarInstruccion) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                color = Color.White.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Toca el mapa para marcar\nla ubicación del avistamiento",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF1F3864),
                    fontWeight = FontWeight.Medium
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
                            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(24.dp),
            containerColor = Color(0xFF1F3864)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación", tint = Color.White)
        }

        // Botón confirmar
        ubicacionSeleccionada?.let { latLng ->
            FloatingActionButton(
                onClick = { onUbicacionSeleccionada(latLng) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(24.dp),
                containerColor = Color(0xFF1F3864)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Confirmar", tint = Color.White)
            }
        }
    }
}