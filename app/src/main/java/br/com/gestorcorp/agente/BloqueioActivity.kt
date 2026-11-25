package br.com.gestorcorp.agente

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

// Tela de bloqueio mostrada no lugar do aplicativo restrito.
class BloqueioActivity : Activity() {
    override fun onCreate(estado: Bundle?) {
        super.onCreate(estado)
        setContentView(R.layout.activity_bloqueio)

        val pacote = intent.getStringExtra(EXTRA_PACOTE).orEmpty()
        val nome = intent.getStringExtra(EXTRA_NOME)?.takeIf { it.isNotBlank() } ?: pacote
        val motivo = intent.getStringExtra(EXTRA_MOTIVO).orEmpty()
        val politica = Preferencias.politica(this)
        val senha = politica?.senha

        findViewById<TextView>(R.id.titulo).text = getString(R.string.bloqueio_titulo, nome)
        findViewById<TextView>(R.id.pacote).text = pacote
        findViewById<TextView>(R.id.motivo).text =
            motivo.ifBlank { getString(R.string.bloqueio_motivo_padrao) }

        val campo = findViewById<EditText>(R.id.senha)
        val botao = findViewById<Button>(R.id.desbloquear)
        val bloqueadoPorTentativas = Preferencias.tentativasBloqueadasAte(this, pacote) > System.currentTimeMillis()

        if (senha == null || !senha.ativo || bloqueadoPorTentativas) {
            campo.visibility = View.GONE
            botao.visibility = View.GONE
            findViewById<TextView>(R.id.motivo).append("\n\n" + getString(R.string.bloqueio_sem_senha))
        } else {
            botao.text = getString(R.string.desbloquear_minutos, senha.desbloqueioMin)
            botao.setOnClickListener {
                if (Senha.confere(campo.text.toString(), senha)) {
                    Preferencias.liberar(this, pacote, senha.desbloqueioMin)
                    Preferencias.limparErros(this, pacote)
                    finish()
                } else {
                    Preferencias.registrarErro(this, pacote, senha.tentativas)
                    campo.setText("")
                    Toast.makeText(this, R.string.senha_incorreta, Toast.LENGTH_SHORT).show()
                    if (Preferencias.tentativasBloqueadasAte(this, pacote) > System.currentTimeMillis()) {
                        recreate()
                    }
                }
            }
        }

        findViewById<Button>(R.id.voltar).setOnClickListener { irParaInicio() }
    }

    override fun onBackPressed() {
        irParaInicio()
    }

    private fun irParaInicio() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    companion object {
        const val EXTRA_PACOTE = "pacote"
        const val EXTRA_NOME = "nome"
        const val EXTRA_MOTIVO = "motivo"
    }
}