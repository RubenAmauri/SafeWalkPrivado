package com.safewalk.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safewalk.app.viewmodel.DashboardData
import com.safewalk.app.viewmodel.DashboardViewModel
import com.safewalk.app.viewmodel.ReporteInteraccion

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onRegresar: () -> Unit,
    onVerHistorial: () -> Unit
) {
    val datos by viewModel.datos.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val inactivos = datos?.reportesInactivos ?: emptyList()

    LaunchedEffect(Unit) { viewModel.cargar() }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                    Text(
                        text = "Mi actividad",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    // Botón de alerta
                    if (inactivos.isNotEmpty()) {
                        Box {
                            IconButton(onClick = onVerHistorial) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Reportes inactivos",
                                    tint = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color.Red, CircleShape)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        when {
            cargando -> Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            datos == null -> Box(Modifier.fillMaxSize()) {
                Text("Error al cargar datos", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            }
            else -> {
                val d = datos!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Tarjetas de totales
                    item {
                        Text("Resumen general", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TarjetaEstadistica(
                                    titulo = "Mis reportes",
                                    valor = d.totalReportes.toString(),
                                    icono = Icons.Default.Report,
                                    color = Color(0xFF1F3864),
                                    modifier = Modifier.weight(1f)
                                )
                                TarjetaEstadistica(
                                    titulo = "Validaciones",
                                    valor = d.totalValidaciones.toString(),
                                    icono = Icons.Default.CheckCircle,
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TarjetaEstadistica(
                                    titulo = "Invalidaciones",
                                    valor = d.totalInvalidaciones.toString(),
                                    icono = Icons.Default.Cancel,
                                    color = Color(0xFFF44336),
                                    modifier = Modifier.weight(1f)
                                )
                                TarjetaEstadistica(
                                    titulo = "Comentarios",
                                    valor = d.totalComentarios.toString(),
                                    icono = Icons.Default.Comment,
                                    color = Color(0xFF9C27B0),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            TarjetaEstadistica(
                                titulo = "Veces compartido",
                                valor = d.totalCompartidos.toString(),
                                icono = Icons.Default.Share,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Barras por semana + Donut
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1.6f)) {
                                Text("Reportes por semana", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                                Spacer(Modifier.height(8.dp))
                                GraficaBarras(semanas = d.reportesPorSemana)
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Por agresividad", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                                Spacer(Modifier.height(8.dp))
                                GraficaDonut(bajo = d.bajo, medio = d.medio, alto = d.alto)
                            }
                        }
                    }

                    // Expandable interacciones por reporte
                    item {
                        SeccionInteracciones(interacciones = d.interacciones)
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TarjetaEstadistica(
    titulo: String,
    valor: String,
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            Column {
                Text(valor, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
                Text(titulo, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun GraficaBarras(semanas: List<Pair<String, Int>>) {
    val maxVal = semanas.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val alturaMaxima = 72.dp

    Row(
        modifier = Modifier.fillMaxWidth().height(alturaMaxima + 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        semanas.forEach { (etiqueta, count) ->
            val altura = if (count == 0) 2.dp else maxOf(2.dp, alturaMaxima * count.toFloat() / maxVal.toFloat())
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.width(32.dp)
            ) {
                if (count > 0) {
                    Text("$count", fontSize = 9.sp, color = Color.Gray)
                }
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(altura)
                        .background(Color(0xFF1F3864), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                )
                Spacer(Modifier.height(4.dp))
                Text(etiqueta, fontSize = 8.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun GraficaDonut(bajo: Int, medio: Int, alto: Int) {
    val total = (bajo + medio + alto).coerceAtLeast(1).toFloat()

    Canvas(modifier = Modifier.size(90.dp)) {
        val stroke = 22.dp.toPx()
        val padding = stroke / 2 + 2.dp.toPx()
        val diameter = size.minDimension - stroke - 4.dp.toPx()
        val topLeft = Offset(padding, padding)
        val arcSize = Size(diameter, diameter)
        var startAngle = -90f

        listOf(
            bajo to Color(0xFF4CAF50),
            medio to Color(0xFFFF9800),
            alto to Color(0xFFF44336)
        ).forEach { (count, color) ->
            if (count > 0) {
                val sweep = 360f * count / total
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
    }

    Spacer(Modifier.height(6.dp))

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(
            Triple("Bajo", Color(0xFF4CAF50), bajo),
            Triple("Medio", Color(0xFFFF9800), medio),
            Triple("Alto", Color(0xFFF44336), alto)
        ).forEach { (label, color, count) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text("$label: $count", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun SeccionInteracciones(interacciones: List<ReporteInteraccion>) {
    var expandido by remember { mutableStateOf(false) }
    var visibles by remember { mutableIntStateOf(5) }
    val maxTotal = interacciones.maxOfOrNull { it.total }?.coerceAtLeast(1) ?: 1

    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Interacciones por reporte", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                IconButton(
                    onClick = {
                        expandido = !expandido
                        if (!expandido) visibles = 5
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            if (expandido) {
                Spacer(Modifier.height(8.dp))
                if (interacciones.isEmpty()) {
                    Text("Sin reportes aún", color = Color.Gray, fontSize = 13.sp)
                } else {
                    interacciones.take(visibles).forEach { item ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                text = item.avistamiento.ubicacionAproximada,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            listOf(
                                Triple("✅ Val.", item.validaciones, Color(0xFF4CAF50)),
                                Triple("❌ Inv.", item.invalidaciones, Color(0xFFF44336)),
                                Triple("💬 Com.", item.comentarios, Color(0xFF9C27B0)),
                                Triple("📤 Com.", item.compartidos, Color(0xFF2196F3))
                            ).forEach { (label, valor, color) ->
                                BarraHorizontal(label = label, value = valor, maxValue = maxTotal, color = color)
                            }
                        }
                        HorizontalDivider()
                    }

                    if (visibles < interacciones.size) {
                        TextButton(
                            onClick = { visibles += 5 },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                "Ver ${minOf(5, interacciones.size - visibles)} más",
                                fontSize = 12.sp,
                                color = Color(0xFF1F3864)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarraHorizontal(label: String, value: Int, maxValue: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.width(52.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(5.dp))
        ) {
            if (value > 0 && maxValue > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(value.toFloat() / maxValue.toFloat())
                        .background(color, RoundedCornerShape(5.dp))
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Text("$value", fontSize = 10.sp, modifier = Modifier.width(20.dp))
    }
}