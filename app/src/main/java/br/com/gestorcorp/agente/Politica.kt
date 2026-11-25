package br.com.gestorcorp.agente

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class RegraApp(
    val pacote: String,
    val nome: String,
    val tipo: String,
    val motivo: String,
    val protecao: String,
    val excecaoAte: Long?,
)

data class BloqueioSenha(
    val ativo: Boolean,
    val salt: String,
    val hash: String,
    val iteracoes: Int,
    val desbloqueioMin: Int,
    val tentativas: Int,
)

// Política recebida do painel, como o aparelho precisa dela.
data class Politica(
    val nome: String,
    val versao: Int,
    val apps: List<RegraApp>,
    val senha: BloqueioSenha?,
) {
    val restritos: List<RegraApp> get() = apps.filter { it.tipo == "restrito" }

    /** Devolve a regra quando o aplicativo deve ser bloqueado agora. */
    fun bloqueadoAgora(pacote: String, liberadoAte: Long): RegraApp? {
        val regra = apps.firstOrNull { it.pacote == pacote } ?: return null
        if (regra.tipo != "restrito") return null
        val agora = System.currentTimeMillis()
        if (regra.excecaoAte != null && regra.excecaoAte > agora) return null
        if (liberadoAte > agora) return null
        return regra
    }

    companion object {
        fun deJson(texto: String?): Politica? {
            if (texto.isNullOrBlank()) return null
            val raiz = runCatching { JSONObject(texto) }.getOrNull() ?: return null
            val itens = raiz.optJSONArray("aplicativos")
            val apps = if (itens == null) {
                emptyList()
            } else {
                (0 until itens.length()).mapNotNull { indice ->
                    val item = itens.optJSONObject(indice) ?: return@mapNotNull null
                    val pacote = item.optString("pacote")
                    if (pacote.isBlank()) return@mapNotNull null
                    RegraApp(
                        pacote = pacote,
                        nome = item.optString("nome").ifBlank { pacote },
                        tipo = item.optString("tipo", "permitido"),
                        motivo = item.optString("motivo"),
                        protecao = item.optString("protecao", "livre"),
                        excecaoAte = item.optString("excecao_ate")
                            .takeIf { it.isNotBlank() && it != "null" }
                            ?.let(::dataIso),
                    )
                }
            }
            val senhaJson = raiz.optJSONObject("bloqueio_senha")
            val senha = senhaJson?.takeIf { it.optBoolean("ativo") }?.let {
                BloqueioSenha(
                    ativo = true,
                    salt = it.optString("salt"),
                    hash = it.optString("hash"),
                    iteracoes = it.optInt("iteracoes", 120000),
                    desbloqueioMin = it.optInt("desbloqueio_min", 15),
                    tentativas = it.optInt("tentativas", 5),
                )
            }
            return Politica(raiz.optString("nome"), raiz.optInt("versao", 1), apps, senha)
        }

        private fun dataIso(valor: String): Long? = runCatching {
            val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
            formato.timeZone = TimeZone.getTimeZone("UTC")
            formato.parse(valor)?.time
        }.getOrNull()
    }
}