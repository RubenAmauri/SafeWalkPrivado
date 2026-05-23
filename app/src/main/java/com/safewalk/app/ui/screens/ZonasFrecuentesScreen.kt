package com.safewalk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.safewalk.app.model.ZonaFrecuente
import com.safewalk.app.viewmodel.ZonasFrecuentesViewModel

@Composable
fun ZonasFrecuentesScreen(
    viewModel: ZonasFrecuentesViewModel,
    ubicacionPreseleccionada: LatLng?,
    onSeleccionarUbicacion: () -> Unit,
    onRegresar: () -> Unit
) {
    val zonas by viewModel.zonas.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val guardadoExitoso by viewModel.guardadoExitoso.collectAsState()
    val zonaParaEditar by viewModel.zonaParaEditar.collectAsState()

    var mostrarFormulario by remember { mutableStateOf(false) }
    var zonaAEliminar by remember { mutableStateOf<ZonaFrecuente?>(null) }

    // Al guardar exitosamente, cerrar formulario
    LaunchedEffect(guardadoExitoso) {
        if (guardadoExitoso) {
            mostrarFormulario = false
            viewModel.limpiarEdicion()
        }
    }

    // Diálogo de confirmación de eliminación
    zonaAEliminar?.let { zona ->
        AlertDialog(
            onDismissRequest = { zonaAEliminar = null },
            title = { Text("Eliminar zona") },
            text = { Text("¿Eliminar \"${zona.nombre}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(zona.id)
                    zonaAEliminar = null
                }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { zonaAEliminar = null }) { Text("Cancelar") }
            }
        )
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
                        text = "Zonas frecuentes",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        viewModel.limpiarEdicion()
                        mostrarFormulario = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.White)
                    }
                }
            }
        }

        if (mostrarFormulario) {
            FormularioZonaFrecuente(
                zonaInicial = zonaParaEditar,
                ubicacionPreseleccionada = ubicacionPreseleccionada,
                onSeleccionarUbicacion = onSeleccionarUbicacion,
                onGuardar = { nombre, latitud, longitud ->
                    val zona = zonaParaEditar
                    if (zona != null) {
                        viewModel.editar(zona.id, nombre, latitud, longitud)
                    } else {
                        viewModel.agregar(nombre, latitud, longitud)
                    }
                },
                onCancelar = {
                    mostrarFormulario = false
                    viewModel.limpiarEdicion()
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    cargando -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    zonas.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("No tienes zonas frecuentes", color = Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { mostrarFormulario = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F3864))
                            ) {
                                Text("Agregar zona")
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(zonas, key = { it.id }) { zona ->
                                TarjetaZonaFrecuente(
                                    zona = zona,
                                    onEditar = {
                                        viewModel.seleccionarParaEditar(zona)
                                        mostrarFormulario = true
                                    },
                                    onEliminar = { zonaAEliminar = zona }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaZonaFrecuente(
    zona: ZonaFrecuente,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF1F3864),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(zona.nombre, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "%.4f, %.4f".format(zona.latitud, zona.longitud),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF1F3864))
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
            }
        }
    }
}

@Composable
private fun FormularioZonaFrecuente(
    zonaInicial: ZonaFrecuente?,
    ubicacionPreseleccionada: LatLng?,
    onSeleccionarUbicacion: () -> Unit,
    onGuardar: (nombre: String, latitud: Double, longitud: Double) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf(zonaInicial?.nombre ?: "") }
    var errorNombre by remember { mutableStateOf(false) }
    var errorUbicacion by remember { mutableStateOf(false) }

    val ubicacionFinal = ubicacionPreseleccionada
        ?: zonaInicial?.let { LatLng(it.latitud, it.longitud) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (zonaInicial != null) "Editar zona" else "Nueva zona frecuente",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                errorNombre = false
            },
            label = { Text("Nombre (ej. Casa, Escuela)") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorNombre,
            singleLine = true
        )
        if (errorNombre) {
            Text("El nombre es obligatorio", color = Color.Red, fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = onSeleccionarUbicacion,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (ubicacionFinal != null)
                    "%.4f, %.4f".format(ubicacionFinal.latitude, ubicacionFinal.longitude)
                else "Seleccionar en el mapa"
            )
        }
        if (errorUbicacion) {
            Text("Selecciona una ubicación", color = Color.Red, fontSize = 12.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier.weight(1f)
            ) { Text("Cancelar") }

            Button(
                onClick = {
                    errorNombre = nombre.isBlank()
                    errorUbicacion = ubicacionFinal == null
                    if (!errorNombre && !errorUbicacion) {
                        onGuardar(nombre, ubicacionFinal!!.latitude, ubicacionFinal.longitude)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F3864))
            ) { Text("Guardar") }
        }
    }
}