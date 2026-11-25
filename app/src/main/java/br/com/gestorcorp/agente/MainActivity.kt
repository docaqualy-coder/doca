package br.com.gestorcorp.agente

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Vínculo do aparelho, autorizações do Android e sincronização da política.
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var campoCodigo: EditText

    override fun onCreate(estado: Bundle?) {
        super.onCreate(estado)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        campoCodigo = findViewById(R.id.codigo)

        findViewById<Button>(R.id.vincular).setOnClickListener { vincular() }
        findViewById<Button>(R.id.sincronizar).setOnClickListener { sincronizar() }
        findViewById<Button>(R.id.acessibilidade).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.admin).setOnClickListener { pedirAdministrador() }
        findViewById<Button>(R.id.desvincular).setOnClickListener {
            Preferencias.esquecer(this)
            avisar("Aparelho desvinculado neste celular.")
        }

        SyncWorker.agendar(this)
        atualizarStatus()
    }

    override fun onResume() {
        super.onResume()
        atualizarStatus()
    }

    private fun vincular() {
        val codigo = campoCodigo.text.toString().trim().uppercase()
        if (codigo.isBlank()) {
            avisar("Informe o código gerado no painel.")
            return
        }
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val identificador = "${Build.MANUFACTURER}-${Build.MODEL}-${androidId.takeLast(6)}"
        executar("Vinculando…") {
            val dados = Api.vincular(codigo, identificador, Build.MODEL, Build.VERSION.RELEASE)
            Preferencias.salvarCredencial(this, dados.optString("credencial"))
            sincronizarAgora()
        }
    }

    private fun sincronizar() {
        if (Preferencias.credencial(this) == null) {
            avisar("Vincule o aparelho primeiro.")
            return
        }
        executar("Sincronizando…") { sincronizarAgora() }
    }

    private fun sincronizarAgora() {
        val credencial = Preferencias.credencial(this) ?: return
        val dados = Api.sincronizar(credencial)
        Preferencias.salvarSincronizacao(
            this,
            dados.optJSONObject("politica")?.toString(),
            dados.optJSONObject("empresa")?.optString("nome").orEmpty(),
        )
        runCatching {
            Api.reportarPermissoes(credencial, Permissoes.concedidas(this), Permissoes.pendentes(this))
        }
    }

    private fun pedirAdministrador() {
        val componente = ComponentName(this, AdminReceiver::class.java)
        val gerenciador = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (gerenciador.isAdminActive(componente)) {
            avisar("Administrador do aparelho já está ativo.")
            return
        }
        startActivity(
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componente)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Permite ao GestorCorp aplicar a política da empresa neste aparelho.",
                )
            },
        )
    }

    private fun executar(mensagem: String, bloco: () -> Unit) {
        status.text = mensagem
        Thread {
            try {
                bloco()
            } catch (erro: Exception) {
                runOnUiThread { avisar(erro.message ?: "Não foi possível falar com o painel.") }
            } finally {
                runOnUiThread { atualizarStatus() }
            }
        }.start()
    }

    private fun atualizarStatus() {
        if (Preferencias.credencial(this) == null) {
            status.text = "Aparelho não vinculado."
            return
        }
        val politica = Preferencias.politica(this)
        val restritos = politica?.restritos?.joinToString(", ") { it.nome }.orEmpty()
        status.text = buildString {
            appendLine("Empresa: ${Preferencias.empresa(this@MainActivity).ifBlank { "—" }}")
            appendLine("Política: ${politica?.let { "${it.nome} v${it.versao}" } ?: "nenhuma recebida"}")
            appendLine("Aplicativos restritos: ${restritos.ifBlank { "nenhum" }}")
            appendLine("Acessibilidade: ${if (Permissoes.acessibilidadeAtiva(this@MainActivity)) "autorizada" else "pendente"}")
            appendLine("Administrador do aparelho: ${if (Permissoes.adminAtivo(this@MainActivity)) "ativo" else "pendente"}")
            append("Última sincronização: ${hora(Preferencias.sincronizadoEm(this@MainActivity))}")
        }
    }

    private fun hora(instante: Long): String {
        if (instante <= 0L) return "nunca"
        return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(instante))
    }

    private fun avisar(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
        atualizarStatus()
    }
}