package com.pwitko.feature.blecomm.ui

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.*
import com.pwitko.ble.BleClient
import com.pwitko.common.Failure
import com.pwitko.common.Loading
import com.pwitko.common.Success
import com.pwitko.feature.blecomm.domain.Device
import com.pwitko.feature.blecomm.ui.utils.BleRequestPermissions
import com.pwitko.feature.blecomm.ui.utils.DisposableEffectWithLifeCycle
import com.pwitko.feature.blecomm.ui.utils.TurnOnBtAdapterScreen
import com.pwitko.feature.blecomm.ui.utils.TurnOnLocationServicesScreen

@Composable
fun ScanScreen() {
    val viewModel: BleScanViewModel = hiltViewModel()
    val btState = viewModel.btStateObservable.subscribeAsState(initial = viewModel.btStateObservable.value)
    DisposableEffectWithLifeCycle(onResume = { viewModel.refreshBtState() })
    when(btState.value) {
        BleClient.BtState.BLUETOOTH_ADAPTER_OFF -> TurnOnBtAdapterScreen()
        BleClient.BtState.BT_PERMISSIONS_NOT_GRANTED -> {
            CheckScanPermissions {
                ScreenContent(viewModel)
            }
        }
        BleClient.BtState.LOCATION_SERVICES_OFF -> TurnOnLocationServicesScreen()
        BleClient.BtState.READY -> ScreenContent(viewModel)
        else -> Unit
    }
}

@Composable
private fun ScreenContent(viewModel: BleScanViewModel) {
    DisposableEffectWithLifeCycle(
        onResume = { viewModel.discoverDevices() },
        onPause = { viewModel.stopDiscovery() }
    )
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Scanning for devices....")
            CircularProgressIndicator()
        }
        Spacer(modifier = Modifier.height(16.dp))
        ScanResultList(viewModel = viewModel)
    }
}

@Composable
private fun ScanResultList(viewModel: BleScanViewModel) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        val deviceList by viewModel.foundDevices.subscribeAsState(initial = Loading)

        when(val result = deviceList) {
            is Loading -> Unit
            is Failure -> Text(text = "Error scanning for BT devices")
            is Success<*> -> {
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(result.data as List<Device>) { device ->
                        ScanListItem(device, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanListItem(device: Device, viewModel: BleScanViewModel) {
    Box(modifier = Modifier
        .border(width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(4.dp))
        .padding(16.dp)
        .fillMaxWidth()
        .clickable { viewModel.deviceSelected(device) } ) {
        Column {
            Text(text = "Name: ${device.name ?: "Unknown"}")
            Text(text = "Address: ${device.address}")
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CheckScanPermissions(content: @Composable () -> Unit) {
    if (SDK_INT < VERSION_CODES.S) {
        val locationPermissions = listOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        val locationPermissionState = rememberMultiplePermissionsState(locationPermissions)
        val text =  "Bluetooth scan requires ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permission. Please grant " +
                "them to access functionality"
        BleRequestPermissions(locationPermissionState, text, content)
    } else {
        val bluetoothPermissions = listOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
        )
        val scanPermissionState = rememberMultiplePermissionsState(bluetoothPermissions)
        val text = "Bluetooth scan requires BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions. Please grant " +
                "them to access functionality"
        BleRequestPermissions(scanPermissionState, text, content)
    }
}


