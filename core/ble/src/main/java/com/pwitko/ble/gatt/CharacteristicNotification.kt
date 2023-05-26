package com.pwitko.ble.gatt

import com.pwitko.ble.utils.CompositeId
import io.reactivex.rxjava3.core.Observable

internal data class CharacteristicNotification(
    val id: CompositeId,
    val observable: Observable<Observable<ByteArray>>
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CharacteristicNotification

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}