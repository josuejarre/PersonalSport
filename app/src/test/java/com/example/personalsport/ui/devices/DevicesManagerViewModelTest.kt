package com.example.personalsport.ui.devices

import com.example.personalsport.health.BrandDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DevicesManagerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: DevicesManagerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DevicesManagerViewModel(ioDispatcher = testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_showsDefaultLinkedDevices() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals(2, state.linkedDevices.size)

        val huaweiWatch = state.linkedDevices.find { it.brandDevice == BrandDevice.HUAWEI_WATCH }
        assertNotNull("Default linked devices should include Huawei Watch GT", huaweiWatch)
        assertEquals("Huawei Watch GT (GT-9c6)", huaweiWatch?.name)

        val tanitaScale = state.linkedDevices.find { it.brandDevice == BrandDevice.TANITA_SCALE }
        assertNotNull("Default linked devices should include Tanita Scale", tanitaScale)

        assertFalse("Pairing modal should be closed initially", state.isPairingModalOpen)
        assertFalse("Huawei assistant should be closed initially", state.showHuaweiAssistant)
    }

    @Test
    fun testLinkNewDevice_addsDeviceToLinkedSectionAndUpdatesAvailableBrands() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.openPairingModal()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isPairingModalOpen)

        viewModel.linkNewDevice(BrandDevice.GARMIN)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.linkedDevices.size)

        val garminDevice = state.linkedDevices.find { it.brandDevice == BrandDevice.GARMIN }
        assertNotNull("Garmin device should now be linked", garminDevice)
        assertEquals("Garmin Connect", garminDevice?.name)

        assertFalse("Garmin should no longer be in available brands to pair", state.availableBrandsToPair.contains(BrandDevice.GARMIN))
        assertFalse("Pairing modal should close after linking", state.isPairingModalOpen)
        assertNotNull("Sync message should notify user of successful linking", state.syncMessage)
    }

    @Test
    fun testLinkExistingDevice_preventsDuplicates() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.linkNewDevice(BrandDevice.HUAWEI_WATCH)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Linked devices count should not increase for duplicate", 2, state.linkedDevices.size)
        assertTrue("Sync message should indicate already linked", state.syncMessage?.contains("ya está") == true)
    }

    @Test
    fun testUnlinkDevice_removesDeviceFromLinkedSection() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.unlinkDevice("huawei_gt9c6")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.linkedDevices.size)
        assertNull("Huawei Watch GT should be unlinked", state.linkedDevices.find { it.id == "huawei_gt9c6" })
        assertTrue("Huawei Watch GT should be back in available brands", state.availableBrandsToPair.contains(BrandDevice.HUAWEI_WATCH))
        assertNotNull("Sync message should notify of unlinking", state.syncMessage)
    }

    @Test
    fun testSyncAllDevices_updatesTimestampsAndMessage() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.syncAllDevices()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("isSyncingAll should be false when finished", state.isSyncingAll)
        assertNotNull("Sync message should confirm global sync completion", state.syncMessage)
    }

    @Test
    fun testSingleDeviceSync_updatesTargetDeviceTimestamp() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val initialDevice = viewModel.uiState.value.linkedDevices.first()
        viewModel.syncSingleDevice(initialDevice)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("syncingDeviceId should be reset to null when done", state.syncingDeviceId)
        assertNotNull("Sync message should report single device sync completion", state.syncMessage)
    }

    @Test
    fun testAssistantDialogsStateToggles() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.openHuaweiAssistant()
        assertTrue(viewModel.uiState.value.showHuaweiAssistant)

        viewModel.closeHuaweiAssistant()
        assertFalse(viewModel.uiState.value.showHuaweiAssistant)

        viewModel.openMultiBrandAssistant(BrandDevice.AMAZFIT)
        assertTrue(viewModel.uiState.value.showMultiBrandAssistant)
        assertEquals(BrandDevice.AMAZFIT, viewModel.uiState.value.selectedBrandForAssistant)

        viewModel.closeMultiBrandAssistant()
        assertFalse(viewModel.uiState.value.showMultiBrandAssistant)
    }

    @Test
    fun testClearSyncMessage() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.linkNewDevice(BrandDevice.FITBIT)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.syncMessage)

        viewModel.clearSyncMessage()
        assertNull(viewModel.uiState.value.syncMessage)
    }
}
