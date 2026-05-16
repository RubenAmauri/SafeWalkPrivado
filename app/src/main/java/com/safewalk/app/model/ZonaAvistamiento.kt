package com.safewalk.app.model

import com.google.android.gms.maps.model.LatLng

data class ZonaAvistamiento(
    val id: String,
    val centro: LatLng,
    val avistamientos: List<Avistamiento>,
    val nivelPromedio: NivelAgresividad
)