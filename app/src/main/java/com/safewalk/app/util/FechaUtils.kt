package com.safewalk.app.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun formatearFecha(fechaIso: String): String {
    return try {
        // Normalizar el string — quitar microsegundos y convertir timezone
        val normalizada = fechaIso
            .replace(Regex("\\.\\d+"), "")  // quitar microsegundos
            .replace("+00:00", "Z")          // normalizar timezone UTC

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val fecha = sdf.parse(normalizada.replace("Z", "+0000"))

        val sdfOut = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "MX"))
        sdfOut.timeZone = TimeZone.getDefault() // hora local del dispositivo
        sdfOut.format(fecha ?: fechaIso)
    } catch (e: Exception) {
        fechaIso
    }
}