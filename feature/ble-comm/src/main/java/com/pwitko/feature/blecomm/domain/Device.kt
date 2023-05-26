package com.pwitko.feature.blecomm.domain

internal interface Device {
    val name: String?
    val address: String
}

//internal interface BleController {
//    fun getBtStateObservable(): Observable<BleClient.BtState>
//
//    fun getConnectionStateObservable(device: Device): Observable<BleConnection.State>
//
//    fun getDevicesWithUuid(uuid: UUID): Observable<Device>
//
//    fun establishConnection(device: Device): Single<BleConnection>
//
//    fun establishConnection(deviceAddress: String): Single<BleConnection>
//
//    fun disconnect(device: Device): Completable
//
////    fun getServices(device: Device): Observable<BleServices>
//
////    fun readBytes(uuid: UUID): Single<ByteArray>
////
////    fun observeBytes(uuid: UUID): Observable<ByteArray>
////
////    fun writeBytes(uuid: UUID, data: ByteArray): Single<ByteArray>
//}


