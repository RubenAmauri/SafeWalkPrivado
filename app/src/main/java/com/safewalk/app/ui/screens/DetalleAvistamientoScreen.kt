package com.safewalk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.Comentario
import com.safewalk.app.model.NivelAgresividad
import com.safewalk.app.repository.AvistamientoRepository
import com.safewalk.app.util.formatearFecha
import com.safewalk.app.viewmodel.ComentarioViewModel
import androidx.compose.material.icons.filled.CheckCircle
import com.safewalk.app.viewmodel.ValidacionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DetalleAvistamientoScreen(
    avistamiento: Avistamiento,
    onRegresar: () -> Unit,
    onVerEnMapa: (Double, Double) -> Unit = { _, _ -> },
    viewModel: ComentarioViewModel = viewModel(),
    validacionViewModel: ValidacionViewModel = viewModel()
) {
    val comentarios by viewModel.comentarios.collectAsState()
    val enviando by viewModel.enviando.collectAsState()
    var texto by remember { mutableStateOf("") }
    var fotos by remember { mutableStateOf<List<String>>(emptyList()) }
    val validaciones by validacionViewModel.validaciones.collectAsState()
    val cargandoValidacion by validacionViewModel.cargando.collectAsState()
    val yaValido = validaciones[avistamiento.id] ?: false
    val estaCargandoValidacion = avistamiento.id in cargandoValidacion
    val esPropio = validacionViewModel.esPropioReporte(avistamiento.usuarioId)
    val contadores by validacionViewModel.contadores.collectAsState()
    val contador = contadores[avistamiento.id] ?: avistamiento.totalConfirmaciones

    LaunchedEffect(avistamiento.id) {
        viewModel.cargarComentarios(avistamiento.id)
        fotos = AvistamientoRepository.getFotosAvistamiento(avistamiento.id)
        validacionViewModel.cargarValidacion(avistamiento.id, avistamiento.totalConfirmaciones)
    }


    val colorNivel = when (avistamiento.nivelAgresividad) {
        NivelAgresividad.BAJO -> Color(0xFF4CAF50)
        NivelAgresividad.MEDIO -> Color(0xFFFF9800)
        NivelAgresividad.ALTO -> Color(0xFFF44336)
    }
    val etiquetaNivel = when (avistamiento.nivelAgresividad) {
        NivelAgresividad.BAJO -> "Bajo"
        NivelAgresividad.MEDIO -> "Medio"
        NivelAgresividad.ALTO -> "Alto"
    }

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
                        text = "Detalle del avistamiento",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Contenido
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    // Nivel de agresividad
                    Surface(
                        color = colorNivel.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = colorNivel,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(14.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Nivel de agresividad: $etiquetaNivel",
                                color = colorNivel,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    // Descripción
                    Text(
                        "Descripción",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(avistamiento.descripcion, fontSize = 15.sp)
                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                }

                item {
                    // Ubicación — toca para ver en mapa
                    Text(
                        "Ubicación",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = avistamiento.ubicacionAproximada,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onVerEnMapa(avistamiento.latitud, avistamiento.longitud) }
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Ver en mapa",
                                tint = Color(0xFF1F3864)
                            )
                        }
                    }
                    HorizontalDivider()
                }

                item {
                    // Fecha
                    Text(
                        "Fecha y hora",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formatearFecha(avistamiento.fechaCreacion), fontSize = 15.sp)
                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                }

                //Validaciones
                item {
                    Text(
                        "Validaciones",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (estaCargandoValidacion) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    if (!esPropio) validacionViewModel.toggleValidacion(avistamiento.id)
                                },
                                enabled = !esPropio,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
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
                            text = "$contador confirmaciones",
                            fontSize = 15.sp
                        )
                        if (esPropio) {
                            Text(
                                text = "(tu reporte)",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                }

                // Fotos
                if (fotos.isNotEmpty()) {
                    item {
                        Text(
                            "Fotos",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(fotos) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Foto del avistamiento",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    Text(
                        "Comentarios",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (comentarios.isEmpty()) {
                    item {
                        Text("Sé el primero en comentar", color = Color.Gray)
                    }
                } else {
                    items(comentarios) { comentario ->
                        ComentarioItem(comentario)
                    }
                }

                // Espacio para el input de comentario
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }

        // Input comentario
        Surface(shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = texto,
                    onValueChange = { texto = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un comentario...") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (texto.isNotBlank()) {
                            viewModel.agregarComentario(avistamiento.id, texto)
                            texto = ""
                        }
                    },
                    enabled = !enviando,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F3864))
                ) {
                    if (enviando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Enviar")
                    }
                }
            }
        }
    }
}

    @Composable
    fun ComentarioItem(comentario: Comentario) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Usuario ${comentario.usuarioId.take(8)}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = comentario.texto, fontSize = 14.sp)
            Text(
                text = formatearFecha(comentario.fecha),
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        HorizontalDivider()
    }
