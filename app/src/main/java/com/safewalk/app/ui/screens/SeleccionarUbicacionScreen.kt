package com.safewalk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun SeleccionarUbicacionScreen(
    onUbicacionSeleccionada: (LatLng) -> Unit,
    onRegresar: () -> Unit
) {
    val zacatecas = LatLng(22.7709, -102.5832)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(zacatecas, 14f)
    }
    var ubicacionSeleccionada by remember { mutableStateOf<LatLng?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
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