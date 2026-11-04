package com.financas.app.domain

import com.financas.app.domain.usecase.UpdateSignatureCheck
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSignatureCheckTest {

    @Test
    fun sameSigner_isCompatible() {
        assertTrue(UpdateSignatureCheck.isCompatible(setOf("aa", "bb"), setOf("bb")))
    }

    @Test
    fun differentSigner_isIncompatible() {
        assertFalse(UpdateSignatureCheck.isCompatible(setOf("aa"), setOf("bb")))
    }

    @Test
    fun unknownSigners_doNotBlockTheInstall() {
        assertTrue(UpdateSignatureCheck.isCompatible(emptySet(), setOf("bb")))
        assertTrue(UpdateSignatureCheck.isCompatible(setOf("aa"), emptySet()))
    }
}
