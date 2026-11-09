package com.financas.app.domain.usecase

import java.net.URI

object UpdateSignatureCheck {

    val RELEASE_CERT_SHA256: Set<String> = setOf(
        "8104259d37aedb3557a0255e6518c0430a3fea535151789a5671ce709a9792a5"
    )

    fun isCompatible(installedDigests: Set<String>, candidateDigests: Set<String>): Boolean {
        if (installedDigests.isEmpty() || candidateDigests.isEmpty()) return false
        return installedDigests.intersect(candidateDigests).isNotEmpty()
    }

    fun isOfficialRelease(candidateDigests: Set<String>, pinned: Set<String> = RELEASE_CERT_SHA256): Boolean {
        if (candidateDigests.isEmpty()) return false
        return candidateDigests.intersect(pinned).isNotEmpty()
    }

    fun isSamePackageUpgrade(
        installedPackage: String,
        installedVersionCode: Long,
        candidatePackage: String?,
        candidateVersionCode: Long
    ): Boolean = candidatePackage == installedPackage && candidateVersionCode > installedVersionCode

    const val MISMATCH_MESSAGE =
        "Esta versão foi assinada com outra chave e o Android não a instala por cima da app atual. " +
            "Exporta o backup (Conta → Preferências → Exportar Backup), desinstala a app, " +
            "instala a nova versão e restaura o backup."

    const val UNTRUSTED_MESSAGE =
        "A atualização descarregada não está assinada pela chave oficial do Finflow e foi bloqueada."

    const val INVALID_PACKAGE_MESSAGE =
        "O ficheiro descarregado não é uma versão mais recente do Finflow e foi bloqueado."
}

object UpdateUrlPolicy {

    const val RELEASE_DOWNLOAD_PATH_PREFIX = "/pauloribeiro16/finflow/releases/download/"

    private val REDIRECT_HOSTS = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com"
    )

    fun isAllowedReleaseUrl(url: String): Boolean {
        val uri = parse(url) ?: return false
        return uri.scheme == "https" &&
            uri.host.equals("github.com", ignoreCase = true) &&
            uri.port == -1 &&
            uri.rawPath.orEmpty().startsWith(RELEASE_DOWNLOAD_PATH_PREFIX) &&
            !uri.rawPath.orEmpty().contains("..")
    }

    fun isAllowedRedirect(url: String): Boolean {
        val uri = parse(url) ?: return false
        return uri.scheme == "https" &&
            uri.port == -1 &&
            REDIRECT_HOSTS.any { it.equals(uri.host, ignoreCase = true) }
    }

    private fun parse(url: String): URI? = try {
        URI(url).takeIf { it.host != null }
    } catch (_: Exception) {
        null
    }
}
