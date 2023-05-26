package com.pwitko.feature.blecomm.ui

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth
import com.pwitko.ble.BleClient
import com.pwitko.common.IResult
import com.pwitko.common.Success
import com.pwitko.feature.blecomm.device.ble.BleConstant
import com.pwitko.feature.blecomm.domain.BleUseCases
import com.pwitko.feature.blecomm.domain.Device
import com.pwitko.feature.blecomm.domain.FakeDevice
import com.pwitko.navigation.NavManager
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.just
import io.mockk.verify
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class BleScanViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    internal lateinit var bleUseCases: BleUseCases

    @RelaxedMockK
    internal lateinit var navManager: NavManager

    private lateinit var cut: BleScanViewModel

    private val fakeDevicesList = listOf(
        FakeDevice("device1", "12:34:56:78:90:12"),
        FakeDevice("device2", "34:56:78:90:12:23"),
        FakeDevice("device3", "56:78:90:12:23:34")
    )

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpRxSchedulers() {
            RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        }
    }

    @Before
    fun setUp() {
        val savedState = SavedStateHandle().apply {
            set("serviceUuid", BleConstant.BatteryService.BATTERY_SERVICE_UUID.toString())
        }
        cut = BleScanViewModel(savedState, bleUseCases, navManager)
    }

    @Test
    fun refreshBtState_checkUpdatesReceived() {
        every { bleUseCases.btState } returnsMany listOf(
            BleClient.BtState.BT_PERMISSIONS_NOT_GRANTED,
            BleClient.BtState.READY
        )

        val testObserver = TestObserver<BleClient.BtState>()
        cut.btStateObservable
            .subscribe(testObserver)
        cut.refreshBtState()
        cut.refreshBtState()

        testObserver
            .assertNoErrors()
            .assertNotComplete()
            .assertValues(BleClient.BtState.BT_PERMISSIONS_NOT_GRANTED, BleClient.BtState.READY)

        verify (exactly = 2) { bleUseCases.btState }
    }

    @Test
    fun discoverDevices_testDevicesFoundSuccessfully() {
        every { bleUseCases.getDevicesWithService(any()) } returns Observable.never<IResult<List<Device>>>()
            .startWith(Observable.just(Success(fakeDevicesList)))

        val testObserver = TestObserver<IResult<List<Device>>>()
        cut.foundDevices.subscribe(testObserver)
        cut.discoverDevices()

        testObserver
            .assertNoErrors()
            .assertNotComplete()
            .assertValue { it == Success(fakeDevicesList) }

        verify { bleUseCases.getDevicesWithService(any()) }
    }

    @Test
    fun stopDiscovery_checkCompletedAndSubscriptionDisposed() {
        every { bleUseCases.getDevicesWithService(any()) } returns Observable.never<IResult<List<Device>>>()
            .startWith(Observable.just(Success(fakeDevicesList)))

        val testObserver = TestObserver<IResult<List<Device>>>()
        cut.foundDevices.subscribe(testObserver)

        cut.discoverDevices()
        cut.stopDiscovery()

        testObserver.assertNoErrors()
            .assertNoErrors()
            .assertValue { it == Success(fakeDevicesList)  }

        Truth.assertThat(cut.scanDisposable).isNull() // stopped and disposed of
        verify { bleUseCases.getDevicesWithService(any()) }
    }

    @Test
    fun deviceSelected_verifyStoppedDiscoveryAndNavigationTriggered() {
        every { bleUseCases.getDevicesWithService(any()) } returns Observable.never<IResult<List<Device>>>()
            .startWith(Observable.just(Success(fakeDevicesList)))
        every { navManager.navigate(any()) } just Runs

        val testObserver = TestObserver<IResult<List<Device>>>()
        cut.foundDevices.subscribe(testObserver)

        // Make sure discovery ongoing
        cut.discoverDevices()

        // make sure devices are discovered
        testObserver.assertNoErrors()
            .assertNoErrors()
            .assertValue { it == Success(fakeDevicesList)  }

        // simulate user click on first device
        cut.deviceSelected(fakeDevicesList.first())

        Truth.assertThat(cut.scanDisposable).isNull() // ensure discovery stopped
        verify { navManager.navigate(any()) } // ensure navigation triggered
    }

    @Test
    fun onCleared_disposeOfDisposables() {
        every { bleUseCases.getDevicesWithService(any()) } returns Observable.never<IResult<List<Device>>>()
            .startWith(Observable.just(Success(fakeDevicesList)))
        cut.discoverDevices()
        cut.onCleared()

        Truth.assertThat(cut.compositeDisposable.isDisposed).isTrue()
        Truth.assertThat(cut.scanDisposable?.isDisposed).isTrue()
    }
}