package br.com.gestorcorp.agente

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// Comunicação com o painel. O aparelho usa a credencial individual recebida no
// vínculo; nada de senha de usuário é guardado aqui.
object Api {
    // Endereço do painel GestorCorp.
    const val BASE = "https://poetic-gestor-corp-flow.base44.app"

    private fun post(caminho: String, corpo: JSONObject, credencial: String?): JSONObject {
        val conexao = URL(BASE + caminho).openConnection() as HttpURLConnection
        conexao.requestMethod = "POST"
        conexao.connectTimeout = 15000
        conexao.readTimeout = 15000
        conexao.doOutput = true
        conexao.setRequestProperty("Content-Type", "application/json")
        if (!credencial.isNullOrBlank()) {
            conexao.setRequestProperty("Authorization", "Bearer $credencial")
            conexao.setRequestProperty("x-device-credential", credencial)
        }
        conexao.outputStream.use { it.write(corpo.toString().toByteArray()) }

        val codigo = conexao.responseCode
        val fluxo = if (codigo in 200..299) conexao.inputStream else conexao.errorStream
        val texto = fluxo?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (codigo !in 200..299) {
            val mensagem = runCatching { JSONObject(texto).optString("erro") }.getOrNull()
            throw IOException(mensagem?.takeIf { it.isNotBlank() } ?: "Falha ($codigo)")
        }
        return JSONObject(texto)
    }

    fun vincular(codigo: String, identificador: String, modelo: String, versaoAndroid: String): JSONObject =
        post(
            "/api/device/enroll",
            JSONObject().apply {
                put("codigo", codigo)
                put("identificador", identificador)
                put("modelo", modelo)
                put("versao_android", versaoAndroid)
                put("agente_versao", "1.0")
                put("aviso_aceito", true)
            },
            null,
        )

    fun sincronizar(credencial: String): JSONObject = post("/api/device/sync", JSONObject(), credencial)

    fun reportarPermissoes(credencial: String, concedidas: List<String>, negadas: List<String>): JSONObject =
        post(
            "/api/device/report",
            JSONObject().put(
                "permissoes",
                JSONObject().apply {
                    put("concedidas", JSONArray(concedidas))
                    put("negadas", JSONArray(negadas))
                },
            ),
            credencial,
        )
}