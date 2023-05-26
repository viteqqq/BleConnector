package com.pwitko.feature.blecomm.device.ble

import com.pwitko.ble.device.BleDevice
import com.pwitko.feature.blecomm.domain.Device

internal data class BleDeviceWrapper(private val bleDevice: BleDevice): Device {
    override val name: String?
        get() = bleDevice.name
    override val address: String
        get() = bleDevice.address

    fun internalBleDevice(): BleDevice = bleDevice
}

//internal class BleControllerImpl @Inject constructor(
//    private val bleClient: BleClient
//): BleController {
//    override fun getDevicesWithUuid(uuid: UUID): Observable<Device> {
//        val scanFilter = ScanFilter.Builder()
//            .setServiceUuid(ParcelUuid(uuid))
//            .build()
//        return bleClient.scanForBleDevices(filters = listOf(scanFilter))
//            .map { device -> BleDeviceWrapper(device) }
//    }
//
//    override fun establishConnection(device: Device): Single<BleConnection> {
//        return (device as BleDeviceWrapper).internalBleDevice().connect(true)
//    }
//
//    override fun establishConnection(deviceAddress: String): Single<BleConnection> {
//        return bleClient.getBleDevice(deviceAddress).connect(true)
//    }
//
//    override fun getBtStateObservable(): Observable<BleClient.BtState> {
//        return bleClient.observeBluetoothState()
//    }
//
//    override fun disconnect(device: Device): Completable {
//        TODO("Not yet implemented")
//    }
//
//    override fun getConnectionStateObservable(device: Device): Observable<BleConnection.State> {
//        return (device as BleDeviceWrapper).internalBleDevice()
//            .observeConnectionState()
//    }
//
//    internal fun observeBtState(): Observable<BleClient.BtState> = bleClient.observeBluetoothState()
//
//    //    override fun getServices(device: Device): Observable<BleServices> {
////        return (device as BleDevice).getActiveConnection().switchIfEmpty(connect(device))
////    }
//
////    override fun readBytes(device: Device, uuid: UUID): Single<ByteArray> {
////        return getServices().firstOrError()
////            .flatMap { services -> services.getCharacteristic(uuid) }
////            .flatMap { bleConnection.read(it) }
////    }
////
////    override fun observeBytes(device: Device, uuid: UUID): Observable<ByteArray> {
////        return bleConnection.bleServices.getCharacteristic(uuid)
////            .flatMapObservable { characteristic -> bleConnection.subscribe(characteristic) }
////    }
////
////    override fun writeBytes(device: Device, uuid: UUID, data: ByteArray): Single<ByteArray> {
////        return bleConnection.bleServices.getCharacteristic(uuid)
////            .flatMap{ characteristic -> bleConnection.write(characteristic, data) }
////    }
//}