package com.safewalk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsTopHeight

import com.safewalk.app.model.Avistamiento
import com.safewalk.app.model.NivelAgresividad
import com.safewalk.app.model.Comentario // 🔥 IMPORT QUE FALTABA
import com.safewalk.app.viewmodel.ComentarioViewModel
import com.safewalk.app.util.formatearFecha

@Composable
fun DetalleAvistamientoScreen(
    avistamiento: Avistamiento,
    onRegresar: () -> Unit,
    viewModel: ComentarioViewModel = viewModel()
) {

    val comentarios by viewModel.comentarios.collectAsState()
    var texto by remember { mutableStateOf("") }

    LaunchedEffect(avistamiento.id) {
        viewModel.cargarComentarios(avistamiento.id)
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

        // HEADER
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

        // CONTENIDO
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item {
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

                    Text("Descripción", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text(avistamiento.descripcion)

                    HorizontalDivider()

                    Text("Ubicación", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text(avistamiento.ubicacionAproximada)

                    HorizontalDivider()

                    Text("Fecha y hora", fontWeight = FontWeight.SemiBold, color = Color.Gray)

                    val fechaFormateada = remember(avistamiento.fechaCreacion) {
                        formatearFecha(avistamiento.fechaCreacion)
                    }

                    Text(fechaFormateada)

                    HorizontalDivider()

                    Text("Validaciones", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text("${avistamiento.totalConfirmaciones} confirmaciones")

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Comentarios", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            }
        }

        // INPUT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            TextField(
                value = texto,
                onValueChange = { texto = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un comentario...") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                if (texto.isNotBlank()) {
                    viewModel.agregarComentario(avistamiento.id, texto)
                    texto = ""
                }
            }) {
                Text("Enviar")
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
            text = comentario.usuario,
            fontWeight = FontWeight.Bold
        )
        Text(text = comentario.texto)
    }
    HorizontalDivider()
}