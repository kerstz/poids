// SPDX-License-Identifier: GPL-3.0-or-later
package org.tally.app.settings

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.ParcelFileDescriptor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tally.core.unit.DistanceUnit
import org.tally.core.unit.EnergyUnit
import org.tally.core.unit.LengthUnit
import org.tally.core.unit.MassUnit
import org.tally.domain.data.BackupRepository
import org.tally.domain.data.BackupSummary
import org.tally.domain.data.CsvExporter
import org.tally.domain.data.DataWipeRepository
import org.tally.domain.data.RestoreError
import org.tally.domain.data.RestoreResult
import org.tally.domain.settings.SettingsRepository
import org.tally.domain.settings.UnitPreferences

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
// Robolectric ne fournit pas encore l'android-all du targetSdk ; 35 est disponible hors ligne.
@Config(sdk = [35])
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private class FakeSettings : SettingsRepository {
        val prefs = MutableStateFlow(UnitPreferences())
        val off = MutableStateFlow(true)
        override val unitPreferences = prefs
        override val offEnabled = off
        override suspend fun setOffEnabled(enabled: Boolean) { off.value = enabled }
        override suspend fun setMassUnit(unit: MassUnit) {}
        override suspend fun setLengthUnit(unit: LengthUnit) {}
        override suspend fun setDistanceUnit(unit: DistanceUnit) {}
        override suspend fun setEnergyUnit(unit: EnergyUnit) {}
    }

    private class FakeBackup : BackupRepository {
        var restoreCalls = 0
        override suspend fun export(out: OutputStream) = BackupSummary(mapOf("a" to 1))
        override suspend fun restore(input: InputStream): RestoreResult {
            restoreCalls++
            return RestoreResult.Success(mapOf("a" to 2))
        }
        override suspend fun undoLastRestore(): RestoreResult = throw IllegalStateException("boom")
    }

    private class FakeCsv : CsvExporter {
        override suspend fun exportWeights(out: OutputStream) = 0
        override suspend fun exportJournal(out: OutputStream) = 0
    }

    private class FakeWipe : DataWipeRepository {
        var calls = 0
        var fail = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun wipeAll() {
            calls++
            gate?.await()
            if (fail) throw java.io.IOException("disk")
        }
    }

    private companion object {
        var failure: RuntimeException? = null
    }

    private val settings = FakeSettings()
    private val backup = FakeBackup()
    private val wipe = FakeWipe()
    /** Fournisseur dont l'ouverture échoue avec [failure] (sinon sert un fichier vide) (permission d'URI révoquée, URI invalide…). */
    class FailingProvider : ContentProvider() {
        override fun onCreate() = true
        override fun query(u: Uri, p: Array<String>?, s: String?, a: Array<String>?, o: String?): Cursor? = null
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, s: String?, a: Array<String>?) = 0
        override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?) = 0
        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
            failure?.let { throw it }
            val file = File.createTempFile("backup", ".zip").apply { deleteOnExit() }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }

    private fun viewModel(): SettingsViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return SettingsViewModel(settings, backup, FakeCsv(), wipe, context)
    }

    private fun failOpeningWith(e: RuntimeException) {
        failure = e
        Robolectric.setupContentProvider(FailingProvider::class.java, "tally.test")
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private val uri: Uri get() = Uri.parse("content://tally.test/backup.zip")

    @Test fun wipeCallsRepositoryAndReportsWiped() = runTest(dispatcher) {
        val vm = viewModel()
        vm.wipe()
        assertEquals(DataStatus.Busy, vm.data.value)
        advanceUntilIdle()
        assertEquals(1, wipe.calls)
        assertEquals(DataStatus.Wiped, vm.data.value)
    }

    @Test fun wipeFailureReportsWipeFailed() = runTest(dispatcher) {
        wipe.fail = true
        val vm = viewModel()
        vm.wipe()
        advanceUntilIdle()
        assertEquals(DataStatus.WipeFailed, vm.data.value)
    }

    @Test fun restoreWithRevokedPermissionFails() = runTest(dispatcher) {
        failOpeningWith(SecurityException("revoked"))
        val vm = viewModel()
        vm.restore(uri)
        advanceUntilIdle()
        assertEquals(DataStatus.Failed(RestoreError.IO), vm.data.value)
        assertEquals(0, backup.restoreCalls)
    }

    @Test fun restoreWithIllegalArgumentFails() = runTest(dispatcher) {
        failOpeningWith(IllegalArgumentException("bad uri"))
        val vm = viewModel()
        vm.restore(uri)
        advanceUntilIdle()
        assertEquals(DataStatus.Failed(RestoreError.IO), vm.data.value)
    }

    @Test fun restoreSucceeds() = runTest(dispatcher) {
        failure = null
        Robolectric.setupContentProvider(FailingProvider::class.java, "tally.test")
        val vm = viewModel()
        vm.restore(uri)
        advanceUntilIdle()
        assertEquals(DataStatus.Restored(2), vm.data.value)
    }

    @Test fun undoRestoreExceptionBecomesFailed() = runTest(dispatcher) {
        val vm = viewModel()
        vm.undoRestore()
        advanceUntilIdle()
        assertEquals(DataStatus.Failed(RestoreError.IO), vm.data.value)
    }

    @Test fun busyBlocksSecondOperation() = runTest(dispatcher) {
        wipe.gate = CompletableDeferred()
        val vm = viewModel()
        vm.wipe()
        advanceUntilIdle()
        assertEquals(DataStatus.Busy, vm.data.value)
        vm.wipe()
        vm.restore(uri)
        advanceUntilIdle()
        assertEquals(1, wipe.calls)
        assertEquals(0, backup.restoreCalls)
        wipe.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(DataStatus.Wiped, vm.data.value)
    }

    @Test fun offEnabledReflectsRepository() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.offEnabled.collect { } }
        advanceUntilIdle()
        assertTrue(vm.offEnabled.value)
        vm.setOffEnabled(false)
        advanceUntilIdle()
        assertEquals(false, settings.off.value)
        assertEquals(false, vm.offEnabled.value)
    }
}
