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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.unit.sp
import com.safewalk.app.viewmodel.ValidacionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    latitud: Double,
    longitud: Double,
    onVerDetalle: (Avistamiento) -> Unit,
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
    onClick: () -> Unit,
    validacionViewModel: ValidacionViewModel = viewModel()
) {
    val validaciones by validacionViewModel.validaciones.collectAsState()
    val cargando by validacionViewModel.cargando.collectAsState()
    val yaValido = validaciones[avistamiento.id] ?: false
    val estaCargando = avistamiento.id in cargando
    val esPropio = validacionViewModel.esPropioReporte(avistamiento.usuarioId)
    val contadores by validacionViewModel.contadores.collectAsState()
    val contador = contadores[avistamiento.id] ?: avistamiento.totalConfirmaciones

    LaunchedEffect(avistamiento.id) {
        validacionViewModel.cargarValidacion(avistamiento.id, avistamiento.totalConfirmaciones)
    }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = avistamiento.ubicacionAproximada,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = colorNivel.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when (avistamiento.nivelAgresividad) {
                            NivelAgresividad.BAJO -> "Bajo"
                            NivelAgresividad.MEDIO -> "Medio"
                            NivelAgresividad.ALTO -> "Alto"
                        },
                        color = colorNivel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(avistamiento.descripcion, maxLines = 2)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatearFecha(avistamiento.fechaCreacion),
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Validaciones
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (estaCargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (!esPropio) {
                                    validacionViewModel.toggleValidacion(avistamiento.id)
                                }
                            },
                            enabled = !esPropio
                        ) {
                            Icon(
                                imageVector = if (yaValido)
                                    Icons.Default.CheckCircle
                                else
                                    Icons.Default.CheckCircle,
                                contentDescription = "Validar",
                                tint = when {
                                    esPropio -> Color.Gray.copy(alpha = 0.4f)
                                    yaValido -> Color(0xFF4CAF50)
                                    else -> Color.Gray
                                }
                            )
                        }
                    }
                    Text(
                        text = "$contador",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                // Comentarios
                Row(
                    modifier = Modifier.clickable { onClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Comment,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Comentar", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }
    }
}