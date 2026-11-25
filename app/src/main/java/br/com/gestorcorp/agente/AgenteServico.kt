package br.com.gestorcorp.agente

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

// Intercepta a abertura de aplicativos: se o app está restrito na política, o
// agente volta para a tela inicial e mostra o bloqueio por cima dele.
class AgenteServico : AccessibilityService() {
    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        if (evento?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pacote = evento.packageName?.toString() ?: return
        if (pacote == packageName) return
        if (pacote == "com.android.systemui" || pacote == "android") return

        val politica = Preferencias.politica(this) ?: return
        val regra = politica.bloqueadoAgora(pacote, Preferencias.liberadoAte(this, pacote)) ?: return

        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(
            Intent(this, BloqueioActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(BloqueioActivity.EXTRA_PACOTE, regra.pacote)
                putExtra(BloqueioActivity.EXTRA_NOME, regra.nome)
                putExtra(BloqueioActivity.EXTRA_MOTIVO, regra.motivo)
            },
        )
    }

    override fun onInterrupt() {
        // Nada a interromper: o serviço só reage a mudanças de janela.
    }
}