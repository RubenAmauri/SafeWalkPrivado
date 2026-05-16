package com.safewalk.app.repository

import com.safewalk.app.SupabaseClient
import com.safewalk.app.model.Reporte
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ReporteRepository {
    suspend fun enviarDenuncia(reporte: Reporte): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.postgrest
                .from("reportes_contenido")
                .insert(reporte)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}