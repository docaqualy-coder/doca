package br.com.gestorcorp.agente

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings

// Permissões do Android que o funcionário precisa autorizar no aparelho.
object Permissoes {
    private val NECESSARIAS = listOf("acessibilidade", "administrador_dispositivo")

    fun acessibilidadeAtiva(contexto: Context): Boolean {
        val servico = ComponentName(contexto, AgenteServico::class.java).flattenToString()
        val habilitados = Settings.Secure.getString(
            contexto.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return habilitados.split(':').any { it.equals(servico, ignoreCase = true) }
    }

    fun adminAtivo(contexto: Context): Boolean {
        val gerenciador = contexto.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return gerenciador.isAdminActive(ComponentName(contexto, AdminReceiver::class.java))
    }

    fun concedidas(contexto: Context): List<String> = buildList {
        if (acessibilidadeAtiva(contexto)) add("acessibilidade")
        if (adminAtivo(contexto)) add("administrador_dispositivo")
    }

    fun pendentes(contexto: Context): List<String> = NECESSARIAS.filterNot { concedidas(contexto).contains(it) }
}