package com.pwitko.feature.blecomm.ui

import androidx.lifecycle.ViewModel
import com.pwitko.feature.blecomm.device.ble.BleConstant
import com.pwitko.feature.blecomm.navigation.NavDirections
import com.pwitko.navigation.NavManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class HomeViewModel @Inject constructor(private val navManager: NavManager): ViewModel() {

    fun heartSensorButtonClicked() {
        navManager.navigate(NavDirections.Scan.create(
            BleConstant.HeartRateService.HR_SERVICE_UUID.toString()))
    }
}