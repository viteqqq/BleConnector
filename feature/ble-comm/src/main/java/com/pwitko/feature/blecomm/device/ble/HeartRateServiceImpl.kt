package com.pwitko.feature.blecomm.device.ble

import com.pwitko.ble.BleClient
import com.pwitko.feature.blecomm.domain.HeartRateService
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class HeartRateServiceImpl @Inject constructor(
    private val bleClient: BleClient
): HeartRateService {

    override fun writeHeartRateControlPoint(deviceAddress: String, value: Int): Single<Int> {
        return bleClient.getBleDevice(deviceAddress)
            .connect(false)
            .flatMap { connection ->
                connection.bleServices
                    .getCharacteristic(BleConstant.HeartRateService.HEART_RATE_CONTROL_POINT_UUID)
                    .flatMap { characteristic ->
                        connection.write(characteristic, ByteArray(1).apply { set(0, value.toByte()) })
                            .map { bytes -> bytes.first().toInt() }
                    }
            }
    }

    override fun readBodySensorLocation(deviceAddress: String): Single<Int> {
        return bleClient.getBleDevice(deviceAddress)
            .connect(false)
            .flatMap { connection ->
                connection.bleServices
                    .getCharacteristic(BleConstant.HeartRateService.BODY_SENSOR_LOCATION_UUID)
                    .flatMap{ characteristic ->
                        connection.read(characteristic)
                            .map { bytes ->  bytes.first().toInt() }
                    }
            }
    }

    override fun subscribeToHeartRate(deviceAddress: String): Observable<Observable<Int>> {
        return bleClient.getBleDevice(deviceAddress)
            .connect(false)
            .flatMapObservable { connection ->
                connection.bleServices
                    .getCharacteristic(BleConstant.HeartRateService.HEART_RATE_MEASUREMENT_UUID)
                    .flatMapObservable { characteristic ->
                        connection.subscribe(characteristic)
                            .map { bytes -> bytes.map { it[1].toInt() } } // byte at offset 1 contain bpm
                    }
            }
    }
}