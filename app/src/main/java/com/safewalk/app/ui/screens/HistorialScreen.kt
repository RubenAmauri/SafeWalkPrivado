package com.safewalk.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.NivelAgresividad
import com.safewalk.app.util.formatearFecha
import com.safewalk.app.viewmodel.HistorialViewModel

@Composable
fun HistorialScreen(
    viewModel: HistorialViewModel,
    onVolver: () -> Unit,
    onVerDetalle: (Avistamiento) -> Unit,
    onEditar: (Avistamiento) -> Unit,
    inactivosIds: Set<String> = emptySet()
) {
    val reportes by viewModel.reportes.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.cargarMisReportes()
    }
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()

    // Estado para el diálogo de confirmación de eliminación
    var reporteAEliminar by remember { mutableStateOf<Avistamiento?>(null) }

    // Diálogo de confirmación de eliminación
    reporteAEliminar?.let { reporte ->
        AlertDialog(
            onDismissRequest = { reporteAEliminar = null },
            title = { Text("Eliminar reporte") },
            text = { Text("¿Estás seguro de que quieres eliminar este reporte? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarReporte(reporte.id)
                        reporteAEliminar = null
                    }
                ) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { reporteAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
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
                        IconButton(onClick = onVolver) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "Mis reportes",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                cargando -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF1F3864)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error ?: "", color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.cargarMisReportes() }) {
                            Text("Reintentar")
                        }
                    }
                }
                reportes.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color.Gray.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sigue generando reportes 🫡", color = Color.Gray)
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(reportes, key = { it.id }) { reporte ->
                            TarjetaReporteHistorial(
                                reporte = reporte,
                                esInactivo = reporte.id in inactivosIds,
                                onEditar = { onEditar(reporte) },
                                onEliminar = { reporteAEliminar = reporte }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaReporteHistorial(
    reporte: Avistamiento,
    esInactivo: Boolean = false,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val colorNivel = when (reporte.nivelAgresividad) {
        NivelAgresividad.BAJO -> Color(0xFF4CAF50)
        NivelAgresividad.MEDIO -> Color(0xFFFF9800)
        NivelAgresividad.ALTO -> Color(0xFFF44336)
    }
    val etiquetaNivel = when (reporte.nivelAgresividad) {
        NivelAgresividad.BAJO -> "Bajo"
        NivelAgresividad.MEDIO -> "Medio"
        NivelAgresividad.ALTO -> "Alto"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (esInactivo) Color(0xFFFFEBEE) else Color.Transparent)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = colorNivel.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = etiquetaNivel,
                    color = colorNivel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Row {
                IconButton(onClick = onEditar) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Color(0xFF1F3864)
                    )
                }
                IconButton(onClick = onEliminar) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color.Red
                    )
                }
            }
            if (esInactivo) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "+14 días sin interacción",
                        fontSize = 11.sp,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(reporte.descripcion, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(reporte.ubicacionAproximada, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            formatearFecha(reporte.fechaCreacion),
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}