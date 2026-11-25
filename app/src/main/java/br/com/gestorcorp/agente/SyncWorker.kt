package br.com.gestorcorp.agente

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// Sincroniza a política a cada 15 minutos e informa ao painel quais permissões
// estão autorizadas neste aparelho.
class SyncWorker(contexto: Context, parametros: WorkerParameters) : Worker(contexto, parametros) {
    override fun doWork(): Result {
        val credencial = Preferencias.credencial(applicationContext) ?: return Result.success()
        return try {
            val dados = Api.sincronizar(credencial)
            Preferencias.salvarSincronizacao(
                applicationContext,
                dados.optJSONObject("politica")?.toString(),
                dados.optJSONObject("empresa")?.optString("nome").orEmpty(),
            )
            runCatching {
                Api.reportarPermissoes(
                    credencial,
                    Permissoes.concedidas(applicationContext),
                    Permissoes.pendentes(applicationContext),
                )
            }
            Result.success()
        } catch (erro: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun agendar(contexto: Context) {
            val pedido = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(
                "gestorcorp-sync",
                ExistingPeriodicWorkPolicy.KEEP,
                pedido,
            )
        }
    }
}