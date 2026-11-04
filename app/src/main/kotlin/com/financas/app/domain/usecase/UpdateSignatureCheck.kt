package com.financas.app.domain.usecase

object UpdateSignatureCheck {

    fun isCompatible(installedDigests: Set<String>, candidateDigests: Set<String>): Boolean {
        if (installedDigests.isEmpty() || candidateDigests.isEmpty()) return true
        return installedDigests.intersect(candidateDigests).isNotEmpty()
    }

    const val MISMATCH_MESSAGE =
        "Esta versão foi assinada com outra chave e o Android não a instala por cima da app atual. " +
            "Foi guardado um backup automático em Downloads. Exporta o backup (Conta → Preferências), " +
            "desinstala a app, instala a nova versão e restaura o backup."
}
