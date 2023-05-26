package com.pwitko.ble.exceptions

import com.pwitko.ble.ops.BleOpType
import java.util.*

sealed class BleException(msg: String? = null): Exception(msg)

data class BleScanError(val errorCode: Int): BleException()
class BleConnectionError(msg: String): BleException(msg)
data class BleInvalidDeviceAddress(val deviceAddress: String): BleException()
data class BleConnectionPermissionsNotGranted(val missingPermissions: List<String>): BleException()
data class BleScanPermissionsNotGranted(val missingPermissions: List<String>): BleException()
data class BleDisconnection(val deviceAddress: String, val status: Int): BleException()
object BleAdapterOff: BleException()

sealed class BleGattException: BleException()
data class BleGattServiceNotFound(val serviceUUID: UUID): BleGattException()
data class BleGattCharacteristicNotFound(val characteristicUUID: UUID): BleGattException()
data class BleGattDescriptorNotFound(val descriptorUUID: UUID): BleGattException()
data class BleGattOperationFailed(val operationType: BleOpType): BleGattException()
data class BleGattDisconnection(val deviceAddress: String, val status: Int): BleGattException()