package com.pwitko.feature.blecomm.domain

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.BluetoothDevice
import com.google.common.truth.Truth
import com.pwitko.ble.BleClient
import com.pwitko.ble.connection.BleConnection
import com.pwitko.ble.device.BleDevice
import com.pwitko.ble.exceptions.BleAdapterOff
import com.pwitko.ble.exceptions.BleConnectionPermissionsNotGranted
import com.pwitko.common.IResult
import com.pwitko.common.Success
import com.pwitko.feature.blecomm.device.ble.BleConstant
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.verify
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

private val SERVICE_UUID: UUID = BleConstant.HeartRateService.HR_SERVICE_UUID

class BleInteractorTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    lateinit var bleClient: BleClient

    @RelaxedMockK
    lateinit var device: BluetoothDevice

    @RelaxedMockK
    lateinit var bleDevice: BleDevice

    @RelaxedMockK
    lateinit var bleConnection: BleConnection

    private val fakeBLeDevices: List<BleDevice> by lazy {
        listOf(
            FakeBleDevice("FakeDevice1", "11:22:33:44:55:66", device),
            FakeBleDevice("FakeDevice2", "66:55:44:33:22:11", device),
            FakeBleDevice("FakeDevice3", "12:34:56:78:98:76", device)
        )
    }

    private lateinit var sut: BleUseCases

    @Before
    fun setUp() {
        sut = BleInteractor(bleClient, Schedulers.trampoline())
    }

    @Test
    fun btStateChanges_btStatePropertyChangesToo() {
        every { bleClient.btState } returns BleClient.BtState.READY
        Truth.assertThat(sut.btState).isEqualTo(bleClient.btState)

        every { bleClient.btState } returns BleClient.BtState.BLUETOOTH_ADAPTER_OFF
        Truth.assertThat(sut.btState).isEqualTo(bleClient.btState)
    }

    @Test
    fun getDevicesWithService_returnAsAList() {
        every { bleClient.scanForBleDevicesWithServices(any()) } returns
                Observable.fromIterable(fakeBLeDevices)
                    .delay(2, TimeUnit.SECONDS)

        val observer = TestObserver<IResult<List<Device>>>()
        sut.getDevicesWithService(SERVICE_UUID)
            .firstOrError()
            .subscribe(observer)

        observer.await()
        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertValueCount(1)
        observer.assertValue {
            (it as Success<List<Device>>).data.zip(fakeBLeDevices).all {
                    pair -> pair.first.address == pair.second.address
            }
        }
    }

    @Test
    fun getDevicesWithService_noDevicesFound() {
        every { bleClient.scanForBleDevicesWithServices(any()) } returns Observable.never()
        val observer = TestObserver<IResult<List<Device>>>()
        sut.getDevicesWithService(SERVICE_UUID)
            .subscribe(observer)

        // Simulate scanning stop
        observer.dispose()

        observer.assertNotComplete()
        observer.assertNoValues()
        observer.assertNoErrors()
    }

    @Test
    fun connectBleDevice_successfullyComplete() {
        every { bleClient.getBleDevice("12:34:56:78:98:76") } returns bleDevice
        every { bleDevice.connect(any(), any()) } returns Single.just(bleConnection)

        val observer = TestObserver<Void>()
        sut.connectToBleDevice("12:34:56:78:98:76")
            .subscribe(observer)

        observer.assertComplete()
        observer.assertNoErrors()

        verify { bleClient.getBleDevice("12:34:56:78:98:76") }
        verify { bleDevice.connect(any(), any()) }
    }

    @Test
    fun connectBleDevice_failWithError() {
        every { bleClient.getBleDevice("12:34:56:78:98:76") } returns bleDevice
        every { bleDevice.connect(any(), any()) } returns Single.error(BleAdapterOff)

        val observer = TestObserver<Void>()
        sut.connectToBleDevice("12:34:56:78:98:76")
            .subscribe(observer)

        observer.await()
        observer.assertError(BleAdapterOff)

        verify { bleClient.getBleDevice("12:34:56:78:98:76") }
        verify { bleDevice.connect(any(), any()) }
    }

    @Test
    fun disconnect_completeSuccessfully() {
        every { bleClient.getBleDevice("12:34:56:78:98:76") } returns bleDevice
        every { bleDevice.disconnect() } returns Completable.complete()

        val observer = TestObserver<Void>()
        sut.disconnect("12:34:56:78:98:76")
            .subscribe(observer)

        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertNoValues()

        verify { bleClient.getBleDevice("12:34:56:78:98:76") }
        verify { bleDevice.disconnect() }
    }

    @Test
    fun disconnect_failWithException() {
        val permissionException = BleConnectionPermissionsNotGranted(listOf(BLUETOOTH_CONNECT))
        every { bleClient.getBleDevice("12:34:56:78:98:76") } returns bleDevice
        every { bleDevice.disconnect() } returns Completable.error(permissionException)

        val observer = TestObserver<Void>()
        sut.disconnect("12:34:56:78:98:76")
            .subscribe(observer)

        observer.await()
        observer.assertError(permissionException)

        verify { bleClient.getBleDevice("12:34:56:78:98:76") }
        verify { bleDevice.disconnect() }
    }

    @Test
    fun observeBtState_verifyBtEventsSubscribed() {
        every { bleClient.observeBluetoothState() } returns Observable.never<BleClient.BtState>()
            .startWith(Observable.just(BleClient.BtState.LOCATION_SERVICES_OFF, BleClient.BtState.READY))
        val observer = TestObserver<BleClient.BtState>()
        sut.observeBtState()
            .subscribe(observer)

        observer.assertNoErrors()
            .assertNotComplete()
            .assertValues(BleClient.BtState.LOCATION_SERVICES_OFF, BleClient.BtState.READY)

        verify { bleClient.observeBluetoothState() }
    }

    @Test
    fun observeConnectionState_verifyClientConnectionStateSubscribed() {
        every { bleClient.getBleDevice("12:34:56:78:98:76") } returns bleDevice
        every { bleDevice.observeConnectionState() } returns Observable
            .never<BleConnection.State>()
            .startWith(Observable.just(BleConnection.State.CONNECTING, BleConnection.State.CONNECTED))

        val observer = TestObserver<BleConnection.State>()
        sut.observeConnectionState("12:34:56:78:98:76")
            .subscribe(observer)

        observer.assertNoErrors()
            .assertNotComplete()
            .assertValueCount(2)
            .assertValues(BleConnection.State.CONNECTING, BleConnection.State.CONNECTED)

        verify { bleClient.getBleDevice("12:34:56:78:98:76") }
        verify { bleDevice.observeConnectionState() }
    }
}