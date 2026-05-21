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
    feedViewModel: FeedViewModel = viewModel(),
    crearViewModel: CrearAvistamientoViewModel = viewModel(),
    validacionViewModel: ValidacionViewModel = viewModel(),
    historialViewModel: HistorialViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var ubicacionReporte by remember { mutableStateOf<LatLng?>(null) }
    var ubicacionEdicion by remember { mutableStateOf<LatLng?>(null) }

    val session by SupabaseClient.client.auth.sessionStatus.collectAsState()

    NavHost(navController = navController, startDestination = "principal") {

        composable("principal") {
            PantallasPrincipales(
                mapaViewModel = mapaViewModel,
                feedViewModel = feedViewModel,
                validacionViewModel = validacionViewModel,
                onCrearReporte = { navController.navigate("crear_reporte") },
                onVerDetalleDesdeFeed = { avistamiento ->
                    feedViewModel.seleccionarAvistamiento(avistamiento)
                    navController.navigate("detalle_avistamiento")
                },
                onVerDetalleDesdeMapaHack = { avistamiento ->
                    feedViewModel.seleccionarAvistamiento(avistamiento)
                    navController.navigate("detalle_avistamiento")
                },
                onIrAHistorial = { navController.navigate("historial") },
                onIrADashboard = { navController.navigate("dashboard") }
            )
        }


        composable("crear_reporte") {
            val guardadoExitoso by crearViewModel.guardadoExitoso.collectAsState()
            val guardando by crearViewModel.guardando.collectAsState()

            LaunchedEffect(guardadoExitoso) {
                if (guardadoExitoso) {
                    crearViewModel.limpiarFormulario()
                    ubicacionReporte = null
                    navController.popBackStack()
                }
            }

            CrearReporteScreen(
                crearViewModel = crearViewModel,
                ubicacionPreseleccionada = ubicacionReporte,
                onGuardar = { avistamiento ->
                    crearViewModel.guardarReporte(avistamiento, context)
                },
                onSeleccionarUbicacion = {
                    navController.navigate("seleccionar_ubicacion")
                },
                onRegresar = {
                    crearViewModel.limpiarFormulario()
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
                    validacionViewModel = validacionViewModel,
                    onRegresar = {
                        feedViewModel.limpiarSeleccion()
                        navController.popBackStack()
                    },
                    onVerEnMapa = { lat, lng ->
                        mapaViewModel.marcarAvistamiento(it)
                        mapaViewModel.navegarAUbicacion(lat, lng)
                        feedViewModel.limpiarSeleccion()
                        navController.navigate("principal") {
                            popUpTo("principal") { inclusive = true }
                        }
                    }
                )
            }
        }

        composable("historial") {
            val inactivosIds by feedViewModel.inactivosIds.collectAsState()
            HistorialScreen(
                viewModel = historialViewModel,
                inactivosIds = inactivosIds,
                onVolver = {
                    feedViewModel.setInactivosIds(emptySet())
                    navController.popBackStack()
                },
                onVerDetalle = { avistamiento ->
                    feedViewModel.seleccionarAvistamiento(avistamiento)
                    navController.navigate("detalle_avistamiento")
                },
                onEditar = { avistamiento ->
                    ubicacionEdicion = null
                    historialViewModel.seleccionarParaEditar(avistamiento)
                    navController.navigate("editar_reporte")
                }
            )
        }
        composable("dashboard") {
            val dashboardViewModel: DashboardViewModel = viewModel()
            DashboardScreen(
                viewModel = dashboardViewModel,
                onRegresar = { navController.popBackStack() },
                onVerHistorial = {
                    val ids = dashboardViewModel.datos.value?.reportesInactivos?.map { it.id }?.toSet() ?: emptySet()
                    feedViewModel.setInactivosIds(ids)
                    navController.navigate("historial")
                }
            )
        }
        composable("editar_reporte") {
            val avistamiento = historialViewModel.avistamientoParaEditar.collectAsState().value
            avistamiento?.let {
                EditarAvistamientoScreen(
                    avistamiento = it,
                    viewModel = historialViewModel,
                    ubicacionPreseleccionada = ubicacionEdicion,
                    onSeleccionarUbicacion = {
                        navController.navigate("seleccionar_ubicacion_edicion")
                    },
                    onRegresar = {
                        ubicacionEdicion = null
                        navController.popBackStack()
                    }
                )
            }
        }

        composable("seleccionar_ubicacion_edicion") {
            SeleccionarUbicacionScreen(
                onUbicacionSeleccionada = {
                    ubicacionEdicion = it
                    navController.popBackStack()
                },
                onRegresar = { navController.popBackStack() }
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun PantallasPrincipales(
    mapaViewModel: MapaViewModel,
    feedViewModel: FeedViewModel,
    validacionViewModel: ValidacionViewModel,
    onCrearReporte: () -> Unit,
    onVerDetalleDesdeFeed: (Avistamiento) -> Unit,
    onVerDetalleDesdeMapaHack: (Avistamiento) -> Unit,
    onIrAHistorial: () -> Unit,
    onIrADashboard: () -> Unit
) {
    val tabActivo by feedViewModel.tabActivo.collectAsState()
    var tab by remember { mutableIntStateOf(tabActivo) }
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
    LaunchedEffect(tab) {
        feedViewModel.setTabActivo(tab)
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
                    onVerDetalle = onVerDetalleDesdeMapaHack,
                    validacionViewModel = validacionViewModel
                )
                1 -> FeedScreen(
                    viewModel = feedViewModel,
                    validacionViewModel = validacionViewModel,
                    latitud = ubicacionActual?.latitude ?: 22.7709,
                    longitud = ubicacionActual?.longitude ?: -102.5832,
                    onVerDetalle = onVerDetalleDesdeFeed,
                    onIrAHistorial = onIrAHistorial,
                    onIrADashboard = onIrADashboard
                )
            }
        }
    }
}
