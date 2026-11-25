package br.com.gestorcorp.agente

import android.content.Context
import android.content.SharedPreferences

// Guarda no aparelho a credencial do vínculo, a última política recebida e as
// liberações temporárias feitas com a senha.
object Preferencias {
    private fun prefs(contexto: Context): SharedPreferences =
        contexto.applicationContext.getSharedPreferences("gestorcorp", Context.MODE_PRIVATE)

    fun credencial(contexto: Context): String? = prefs(contexto).getString("credencial", null)

    fun salvarCredencial(contexto: Context, valor: String) {
        prefs(contexto).edit().putString("credencial", valor).apply()
    }

    fun esquecer(contexto: Context) {
        prefs(contexto).edit().clear().apply()
    }

    fun politica(contexto: Context): Politica? = Politica.deJson(prefs(contexto).getString("politica", null))

    fun salvarSincronizacao(contexto: Context, politicaJson: String?, empresa: String) {
        prefs(contexto).edit()
            .putString("politica", politicaJson.orEmpty())
            .putString("empresa", empresa)
            .putLong("sincronizado_em", System.currentTimeMillis())
            .apply()
    }

    fun empresa(contexto: Context): String = prefs(contexto).getString("empresa", "").orEmpty()

    fun sincronizadoEm(contexto: Context): Long = prefs(contexto).getLong("sincronizado_em", 0L)

    fun liberadoAte(contexto: Context, pacote: String): Long =
        prefs(contexto).getLong("liberado:$pacote", 0L)

    fun liberar(contexto: Context, pacote: String, minutos: Int) {
        val ate = System.currentTimeMillis() + minutos.coerceAtLeast(1) * 60000L
        prefs(contexto).edit().putLong("liberado:$pacote", ate).apply()
    }

    fun tentativasBloqueadasAte(contexto: Context, pacote: String): Long =
        prefs(contexto).getLong("tentativas:$pacote", 0L)

    fun registrarErro(contexto: Context, pacote: String, limite: Int) {
        val prefs = prefs(contexto)
        val erros = prefs.getInt("erros:$pacote", 0) + 1
        if (erros >= limite.coerceAtLeast(1)) {
            prefs.edit()
                .putInt("erros:$pacote", 0)
                .putLong("tentativas:$pacote", System.currentTimeMillis() + 5 * 60000L)
                .apply()
        } else {
            prefs.edit().putInt("erros:$pacote", erros).apply()
        }
    }

    fun limparErros(contexto: Context, pacote: String) {
        prefs(contexto).edit().putInt("erros:$pacote", 0).putLong("tentativas:$pacote", 0L).apply()
    }
}