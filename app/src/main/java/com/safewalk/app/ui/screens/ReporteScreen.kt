package com.safewalk.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.forEach
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safewalk.app.viewmodel.ReporteViewModel
import com.safewalk.app.viewmodel.ReporteState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteScreen(
    reporteId: String,
    userId: String,
    onRegresar: () -> Unit,
    viewModel: ReporteViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    // Estados locales
    var categoriaSeleccionada by remember { mutableStateOf("") }
    val categorias = listOf(
        "Spam",
        "Odio, abuso o acoso",
        "Discurso violento",
        "Contenido para adultos",
        "Terrorismo o extremismo",
        "Contenido gráfico o violento"
    )
    var detalles by remember { mutableStateOf("") }

    // --- VISTA DE ÉXITO ---
    if (estado is ReporteState.Success) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SafeWalk",
                color = Color(0xFF1F3864),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(100.dp))
            Text(
                text = "¡Tu reporte ha sido enviado!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onRegresar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.Gray),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("¡Listo!", color = Color.Black)
            }
        }
    } else {
        // FORMULARIO PRINCIPAL
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Informar un problema", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onRegresar) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("¿Que quieres reportar?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Elige una categoría que mejor describa tu problema",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de Categorías
                categorias.forEach { categoria ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoriaSeleccionada = categoria }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(categoria, fontSize = 14.sp)
                        RadioButton(
                            selected = (categoria == categoriaSeleccionada),
                            onClick = { categoriaSeleccionada = categoria }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Da una descripción detallada:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = detalles,
                    onValueChange = { detalles = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { Text("Escribe aquí...") },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botón Enviar
                Button(
                    onClick = { viewModel.enviarDenuncia(reporteId, userId, categoriaSeleccionada, detalles) },
                    modifier = Modifier
                        .width(150.dp)
                        .height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    enabled = categoriaSeleccionada.isNotBlank()
                ) {
                    Text("Enviar reporte", color = Color.White)
                }

                if (estado is ReporteState.Error) {
                    Text(
                        "Error, falta por llenar algún campo",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}