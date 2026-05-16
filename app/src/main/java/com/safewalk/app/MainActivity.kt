package com.safewalk.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.safewalk.app.model.Avistamiento
import com.safewalk.app.ui.screens.*
import com.safewalk.app.viewmodel.*
import io.github.jan.supabase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { SafeWalkNavigation() }
    }
}

@Composable
fun SafeWalkNavigation(
    mapaViewModel: MapaViewModel = viewModel(),
    feedViewModel: FeedViewModel = viewModel()
) {
    val navController = rememberNavController()
    var ubicacionReporte by remember { mutableStateOf<LatLng?>(null) }

    val session by SupabaseClient.client.auth.sessionStatus.collectAsState()
    val currentUserId = remember(session) {
        SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: ""
    }

    NavHost(navController = navController, startDestination = "principal") {

        composable("principal") {
            PantallasPrincipales(
                mapaViewModel = mapaViewModel,
                feedViewModel = feedViewModel,
                onCrearReporte = { navController.navigate("crear_reporte") },
                onVerDetalle = { avistamiento ->
                    feedViewModel.seleccionarAvistamiento(avistamiento)
                    navController.navigate("detalle_avistamiento")
                },
                onReportar = { avistamiento ->
                    navController.navigate("reportar_contenido/${avistamiento.id}")
                },
                onIrAHistorial = {
                    navController.navigate("historial")
                }
            )
        }

        composable("crear_reporte") {
            CrearReporteScreen(
                mapaViewModel = mapaViewModel,
                ubicacionPreseleccionada = ubicacionReporte,
                onGuardar = {
                    mapaViewModel.agregarAvistamiento(it)
                    mapaViewModel.limpiarFormularioReporte()
                    ubicacionReporte = null
                    navController.popBackStack()
                },
                onSeleccionarUbicacion = {
                    navController.navigate("seleccionar_ubicacion")
                },
                onRegresar = {
                    mapaViewModel.limpiarFormularioReporte()
                    ubicacionReporte = null
                    navController.popBackStack()
                }
            )
        }

        composable("seleccionar_ubicacion") {
            SeleccionarUbicacionScreen(
                onUbicacionSeleccionada = {
                    ubicacionReporte = it
                    navController.popBackStack()
                },
                onRegresar = { navController.popBackStack() }
            )
        }

        composable("detalle_avistamiento") {
            val avistamiento = feedViewModel.avistamientoSeleccionado.collectAsState().value
            avistamiento?.let {
                DetalleAvistamientoScreen(
                    avistamiento = it,
                    onRegresar = {
                        feedViewModel.limpiarSeleccion()
                        navController.popBackStack()
                    }
                )
            }
        }

        composable("reportar_contenido/{avistamientoId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("avistamientoId") ?: ""

            ReporteScreen(
                reporteId = id,
                userId = currentUserId,
                onRegresar = { navController.popBackStack() }
            )
        }

        composable("historial") {
            val historialViewModel: HistorialViewModel = viewModel()
            HistorialScreen(
                viewModel = historialViewModel,
                onVolver = { navController.popBackStack() },
                onVerDetalle = { avistamiento ->
                    feedViewModel.seleccionarAvistamiento(avistamiento)
                    navController.navigate("detalle_avistamiento")
                }
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun PantallasPrincipales(
    mapaViewModel: MapaViewModel,
    feedViewModel: FeedViewModel,
    onCrearReporte: () -> Unit,
    onVerDetalle: (Avistamiento) -> Unit,
    onReportar: (Avistamiento) -> Unit,
    onIrAHistorial: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var ubicacionActual by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            LocationServices.getFusedLocationProviderClient(context)
                .lastLocation.addOnSuccessListener {
                    it?.let {
                        ubicacionActual = LatLng(it.latitude, it.longitude)
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            Column {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(Color(0xFF1F3864))
                )
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = Color(0xFF1F3864),
                    contentColor = Color.White
                ) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Mapa") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Comunidad") })
                }
            }
        }
    ) { padding ->

        Box(modifier = Modifier.padding(padding)) {

            when (tab) {

                0 -> MapaScreen(
                    mapaViewModel = mapaViewModel,
                    onCrearReporte = onCrearReporte,
                    onReportar = onReportar
                )

                1 -> FeedScreen(
                    viewModel = feedViewModel,
                    latitud = ubicacionActual?.latitude ?: 22.7709,
                    longitud = ubicacionActual?.longitude ?: -102.5832,
                    onVerDetalle = onVerDetalle,
                    onReportar = onReportar,
                    onIrAHistorial = onIrAHistorial
                )
            }
        }
    }
}
