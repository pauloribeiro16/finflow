package com.financas.app.core.version

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppReleaseManagerTest {

    @Test
    fun latestRelease_matchesCurrentVersionAndBuild() {
        val latest = AppReleaseManager.latestRelease()
        assertEquals(AppReleaseManager.CURRENT_VERSION, latest.version)
        assertEquals(AppReleaseManager.CURRENT_BUILD, latest.buildCode)
        assertEquals(ReleaseType.MINOR, latest.type)
    }

    @Test
    fun releases_areOrderedChronologicallyDescendingByBuildCode() {
        val releases = AppReleaseManager.RELEASES
        assertTrue("Deve haver lançamentos registados", releases.size >= 6)

        for (i in 0 until releases.size - 1) {
            assertTrue(
                "Build codes devem ser estritamente decrescentes: ${releases[i].buildCode} > ${releases[i + 1].buildCode}",
                releases[i].buildCode > releases[i + 1].buildCode
            )
        }
    }

    @Test
    fun eachRelease_hasValidContentAndBulletPoints() {
        for (release in AppReleaseManager.RELEASES) {
            assertTrue("Versão deve começar com número: ${release.version}", release.version.first().isDigit())
            assertTrue("Título não pode estar vazio", release.title.isNotBlank())
            assertTrue("Descrição não pode estar vazia", release.description.isNotBlank())
            assertFalse("Deve conter lista de alterações", release.changes.isEmpty())
            for (change in release.changes) {
                assertTrue("Alteração não pode estar vazia", change.isNotBlank())
            }
        }
    }
}
