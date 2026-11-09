package com.financas.app.domain

import com.financas.app.domain.usecase.UpdateSignatureCheck
import com.financas.app.domain.usecase.UpdateUrlPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSignatureCheckTest {

    private val official = UpdateSignatureCheck.RELEASE_CERT_SHA256.first()

    @Test
    fun sameSigner_isCompatible() {
        assertTrue(UpdateSignatureCheck.isCompatible(setOf("aa", "bb"), setOf("bb")))
    }

    @Test
    fun differentSigner_isIncompatible() {
        assertFalse(UpdateSignatureCheck.isCompatible(setOf("aa"), setOf("bb")))
    }

    @Test
    fun unknownSigners_blockTheInstall() {
        assertFalse(UpdateSignatureCheck.isCompatible(emptySet(), setOf("bb")))
        assertFalse(UpdateSignatureCheck.isCompatible(setOf("aa"), emptySet()))
    }

    @Test
    fun onlyPinnedCertificate_isOfficial() {
        assertTrue(UpdateSignatureCheck.isOfficialRelease(setOf(official)))
        assertFalse(UpdateSignatureCheck.isOfficialRelease(setOf("deadbeef")))
        assertFalse(UpdateSignatureCheck.isOfficialRelease(emptySet()))
    }

    @Test
    fun pinnedCertificate_isLowercaseSha256Hex() {
        assertTrue(official.matches(Regex("^[0-9a-f]{64}$")))
    }

    @Test
    fun upgrade_requiresSamePackageAndHigherVersion() {
        assertTrue(UpdateSignatureCheck.isSamePackageUpgrade("com.financas.app", 27, "com.financas.app", 28))
        assertFalse(UpdateSignatureCheck.isSamePackageUpgrade("com.financas.app", 27, "com.financas.app", 27))
        assertFalse(UpdateSignatureCheck.isSamePackageUpgrade("com.financas.app", 27, "com.financas.app", 26))
        assertFalse(UpdateSignatureCheck.isSamePackageUpgrade("com.financas.app", 27, "com.evil.app", 99))
        assertFalse(UpdateSignatureCheck.isSamePackageUpgrade("com.financas.app", 27, null, 99))
    }

    @Test
    fun releaseUrl_mustBeThisRepositoryOnGithub() {
        assertTrue(UpdateUrlPolicy.isAllowedReleaseUrl("https://github.com/pauloribeiro16/finflow/releases/download/v2.5.3/Finflow-v2.5.3.apk"))
        assertFalse(UpdateUrlPolicy.isAllowedReleaseUrl("http://github.com/pauloribeiro16/finflow/releases/download/v2.5.3/Finflow-v2.5.3.apk"))
        assertFalse(UpdateUrlPolicy.isAllowedReleaseUrl("https://github.com/attacker/finflow/releases/download/v9/Finflow.apk"))
        assertFalse(UpdateUrlPolicy.isAllowedReleaseUrl("https://github.com.evil.com/pauloribeiro16/finflow/releases/download/v9/x.apk"))
        assertFalse(UpdateUrlPolicy.isAllowedReleaseUrl("https://evil.com/pauloribeiro16/finflow/releases/download/v9/x.apk"))
        assertFalse(UpdateUrlPolicy.isAllowedReleaseUrl("https://github.com:8443/pauloribeiro16/finflow/releases/download/v9/x.apk"))
        assertFalse(UpdateUrlPolicy.isAllowedReleaseUrl("https://github.com/pauloribeiro16/finflow/releases/download/../../attacker/x.apk"))
        assertFalse(UpdateUrlPolicy.isAllowedReleaseUrl("not a url"))
    }

    @Test
    fun redirects_onlyToGithubAssetHosts() {
        assertTrue(UpdateUrlPolicy.isAllowedRedirect("https://objects.githubusercontent.com/github-production-release-asset/1/2?x=y"))
        assertTrue(UpdateUrlPolicy.isAllowedRedirect("https://release-assets.githubusercontent.com/github-production-release-asset/1/2"))
        assertFalse(UpdateUrlPolicy.isAllowedRedirect("http://objects.githubusercontent.com/a"))
        assertFalse(UpdateUrlPolicy.isAllowedRedirect("https://evil.githubusercontent.com.attacker.io/a"))
        assertFalse(UpdateUrlPolicy.isAllowedRedirect("https://example.com/a"))
    }
}
