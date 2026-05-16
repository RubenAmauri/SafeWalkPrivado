package com.safewalk.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
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
import com.safewalk.app.viewmodel.FeedViewModel
import com.safewalk.app.util.formatearFecha

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    latitud: Double,
    longitud: Double,
    onVerDetalle: (Avistamiento) -> Unit,
    onReportar: (Avistamiento) -> Unit,
    onIrAHistorial: () -> Unit
) {
    val avistamientos by viewModel.avistamientos.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()
    val nivelesSeleccionados by viewModel.nivelesSeleccionados.collectAsState()
    val soloRecientes by viewModel.soloRecientes.collectAsState()

    var mostrarMenuFiltros by remember { mutableStateOf(false) }
    val hayFiltrosActivos = nivelesSeleccionados.isNotEmpty() || soloRecientes

    LaunchedEffect(latitud, longitud) {
        viewModel.cargarFeed(latitud, longitud)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔥 HEADER
        Surface(color = Color(0xFF1F3864)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Comunidad",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {

                    if (hayFiltrosActivos) {
                        TextButton(onClick = { viewModel.limpiarFiltros() }) {
                            Text("Limpiar", color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    // 🔥 HISTORIAL
                    IconButton(onClick = onIrAHistorial) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "Mis reportes",
                            tint = Color.White
                        )
                    }

                    // 🔥 FILTROS
                    Box {
                        IconButton(onClick = { mostrarMenuFiltros = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filtrar",
                                tint = if (hayFiltrosActivos) Color(0xFFFFD54F) else Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = mostrarMenuFiltros,
                            onDismissRequest = { mostrarMenuFiltros = false }
                        ) {

                            Text(
                                text = "Nivel de agresividad",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )

                            NivelAgresividad.entries.forEach { nivel ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = nivel in nivelesSeleccionados,
                                                onCheckedChange = null
                                            )
                                            Text(nivel.name)
                                        }
                                    },
                                    onClick = { viewModel.toggleNivel(nivel) }
                                )
                            }

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = soloRecientes,
                                            onCheckedChange = null
                                        )
                                        Text("Solo últimas 24h")
                                    }
                                },
                                onClick = { viewModel.toggleRecientes() }
                            )
                        }
                    }
                }
            }
        }

        // 🔥 LISTA
        Box(modifier = Modifier.fillMaxSize()) {

            if (cargando && avistamientos.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            else if (error != null && avistamientos.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error ?: "Error", color = Color.Red)

                    Button(onClick = { viewModel.cargarFeed(latitud, longitud) }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reintentar")
                    }
                }
            }

            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(avistamientos) { avistamiento ->
                        ReporteItem(
                            avistamiento = avistamiento,
                            onClick = { onVerDetalle(avistamiento) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReporteItem(
    avistamiento: Avistamiento,
    onClick: () -> Unit
) {
    val colorNivel = when (avistamiento.nivelAgresividad) {
        NivelAgresividad.BAJO -> Color(0xFF4CAF50)
        NivelAgresividad.MEDIO -> Color(0xFFFF9800)
        NivelAgresividad.ALTO -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = avistamiento.ubicacionAproximada,
                fontWeight = FontWeight.Bold
            )

            Text(avistamiento.descripcion)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatearFecha(avistamiento.fechaCreacion),
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 🔥 BOTÓN COMENTARIOS (CLAVE CU-06)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Row {
                    Icon(Icons.Default.Report, contentDescription = null)
                    Text(" ${avistamiento.totalConfirmaciones}")
                }

                Row(
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Icon(Icons.Default.Comment, contentDescription = null)
                    Text(" Comentarios")
                }
            }
        }
    }
}