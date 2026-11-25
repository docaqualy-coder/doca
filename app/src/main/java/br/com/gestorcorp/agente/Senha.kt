package br.com.gestorcorp.agente

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// A senha da política é validada offline, com o mesmo hash PBKDF2-SHA256 que o
// painel gera. O aparelho nunca recebe a senha em texto.
object Senha {
    fun confere(senha: String, bloqueio: BloqueioSenha?): Boolean {
        if (bloqueio == null || !bloqueio.ativo) return false
        if (bloqueio.salt.isBlank() || bloqueio.hash.isBlank()) return false
        val especificacao = PBEKeySpec(
            senha.toCharArray(),
            deHex(bloqueio.salt),
            bloqueio.iteracoes.coerceAtLeast(1),
            256,
        )
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(especificacao)
            .encoded
        return hex(bytes).equals(bloqueio.hash, ignoreCase = true)
    }

    private fun deHex(valor: String): ByteArray {
        val pares = valor.chunked(2)
        return ByteArray(pares.size) { indice -> pares[indice].toInt(16).toByte() }
    }

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { byte -> "%02x".format(byte) }
}