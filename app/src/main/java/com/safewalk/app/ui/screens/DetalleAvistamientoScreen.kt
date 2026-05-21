package com.safewalk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
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
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun DetalleAvistamientoScreen(
    avistamiento: Avistamiento,
    onRegresar: () -> Unit,
    onVerEnMapa: (Double, Double) -> Unit = { _, _ -> },
    viewModel: ComentarioViewModel = viewModel(),
    validacionViewModel: ValidacionViewModel
) {
    val comentarios by viewModel.comentarios.collectAsState()
    val enviando by viewModel.enviando.collectAsState()
    var texto by remember { mutableStateOf("") }
    var fotos by remember { mutableStateOf<List<String>>(emptyList()) }
    var fotoExpandida by remember { mutableStateOf<String?>(null) }
    val validaciones by validacionViewModel.validaciones.collectAsState()
    val cargandoValidacion by validacionViewModel.cargando.collectAsState()
    val tipoValidacion = validaciones[avistamiento.id]
    val yaValido = tipoValidacion == "sigue_ahi"
    val estaCargandoValidacion = avistamiento.id in cargandoValidacion
    val esPropio = validacionViewModel.esPropioReporte(avistamiento.usuarioId)
    val contadores by validacionViewModel.contadores.collectAsState()
    val contador = contadores[avistamiento.id] ?: avistamiento.totalConfirmaciones
    val contadoresYaNoEsta by validacionViewModel.contadoresYaNoEsta.collectAsState()
    val contadorYaNoEsta = contadoresYaNoEsta[avistamiento.id] ?: avistamiento.totalYaNoEsta
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(avistamiento.id) {
        viewModel.cargarComentarios(avistamiento.id)
        fotos = AvistamientoRepository.getFotosAvistamiento(avistamiento.id)
        validacionViewModel.cargarValidacion(
            avistamiento.id,
            avistamiento.totalConfirmaciones,
            avistamiento.totalYaNoEsta
        )
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
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        val nivel = when (avistamiento.nivelAgresividad) {
                            NivelAgresividad.BAJO -> "Bajo"
                            NivelAgresividad.MEDIO -> "Medio"
                            NivelAgresividad.ALTO -> "Alto"
                        }
                        val texto = """
                            🐕 Avistamiento de jauría - SafeWalk
                            📍 ${avistamiento.ubicacionAproximada}
                            ⚠️ Nivel de agresividad: $nivel
                            📝 ${avistamiento.descripcion}
                            🕐 ${formatearFecha(avistamiento.fechaCreacion)}
                            ✅ $contador confirmaciones | ❌ $contadorYaNoEsta invalidaciones
                            🗺️ https://maps.google.com/?q=${avistamiento.latitud},${avistamiento.longitud}
    
                            Reportado en SafeWalk. ¡Ten cuidado al pasar por esta zona!
                        """.trimIndent()

                        val intent =
                            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, texto)
                                setPackage("com.whatsapp")
                            }

                        try {
                            context.startActivity(intent)
                            scope.launch {
                                AvistamientoRepository.registrarCompartido(avistamiento.id)
                            }
                        } catch (e: android.content.ActivityNotFoundException) {
                            Toast.makeText(context, "Acción no disponible", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = Color.White
                        )
                    }
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Confirmaciones
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (estaCargandoValidacion) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$contador confirmaciones", fontSize = 14.sp)
                        }
                        // Invalidaciones
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "$contadorYaNoEsta invalidaciones",
                                fontSize = 14.sp,
                                color = Color(0xFFF44336)
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
                                .height(200.dp)
                                .clickable { fotoExpandida = url },  // <- agrega
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

        // Botones de validación + input comentario
        Surface(shadowElevation = 8.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Botones de validación (solo si no es propio reporte)
                if (!esPropio) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botón Sigue ahí
                        OutlinedButton(
                            onClick = {
                                validacionViewModel.registrarOToggle(
                                    avistamiento.id,
                                    "sigue_ahi"
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !estaCargandoValidacion,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (tipoValidacion == "sigue_ahi") Color(0xFF4CAF50) else Color.Transparent,
                                contentColor = if (tipoValidacion == "sigue_ahi") Color.White else Color(
                                    0xFF4CAF50
                                )
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                Color(0xFF4CAF50)
                            )
                        ) {
                            Text(
                                text = if (tipoValidacion == "sigue_ahi") "✓ Sigue ahí (${contador})" else "Sigue ahí (${contador})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Botón Ya no está
                        OutlinedButton(
                            onClick = {
                                validacionViewModel.registrarOToggle(
                                    avistamiento.id,
                                    "ya_no_esta"
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !estaCargandoValidacion,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (tipoValidacion == "ya_no_esta") Color(
                                    0xFFF44336
                                ) else Color.Transparent,
                                contentColor = if (tipoValidacion == "ya_no_esta") Color.White else Color(
                                    0xFFF44336
                                )
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                Color(0xFFF44336)
                            )
                        ) {
                            Text(
                                text = if (tipoValidacion == "ya_no_esta") "✗ Ya no está (${contadorYaNoEsta})" else "Ya no está (${contadorYaNoEsta})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Input comentario
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
        fotoExpandida?.let { url ->
            AlertDialog(
                onDismissRequest = { fotoExpandida = null },
                confirmButton = {},
                text = {
                    AsyncImage(
                        model = url,
                        contentDescription = "Foto expandida",
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        contentScale = ContentScale.FillWidth
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
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