package com.pwitko.ble.gatt

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.pwitko.ble.exceptions.BleGattCharacteristicNotFound
import com.pwitko.ble.exceptions.BleGattDescriptorNotFound
import com.pwitko.ble.exceptions.BleGattServiceNotFound
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import java.util.*

class BleServices(val services: Set<BluetoothGattService>) {

    fun getService(uuid: UUID): Single<BluetoothGattService> {
        return Maybe.fromCallable{
            services.firstOrNull { gattService -> gattService.uuid == uuid }
        }.switchIfEmpty(Single.error(BleGattServiceNotFound(uuid)))
    }

    fun getCharacteristic(
        serviceUUID: UUID,
        characteristicUUID: UUID
    ): Single<BluetoothGattCharacteristic> {
        return getService(serviceUUID)
            .map { gattService ->
                gattService.getCharacteristic(characteristicUUID) ?:
                throw BleGattCharacteristicNotFound(characteristicUUID)
            }
    }

    fun getCharacteristic(uuid: UUID): Single<BluetoothGattCharacteristic> {
        return Maybe.fromCallable {
            val service = services.find { it.getCharacteristic(uuid) != null }
            service?.getCharacteristic(uuid)
        }.switchIfEmpty(Single.error(BleGattCharacteristicNotFound(uuid)))
    }

    fun getDescriptor(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        descriptorUUID: UUID
    ): Single<BluetoothGattDescriptor> {
        return getCharacteristic(serviceUUID, characteristicUUID)
            .map { gattCharacteristic ->
                gattCharacteristic.getDescriptor(descriptorUUID) ?:
                throw BleGattDescriptorNotFound(descriptorUUID)
            }
    }
}