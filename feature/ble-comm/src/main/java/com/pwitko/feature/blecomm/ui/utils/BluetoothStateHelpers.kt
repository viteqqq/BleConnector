package com.pwitko.feature.blecomm.ui.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun BleRequestPermission(
    permissionState: PermissionState,
    text: String,
    content: @Composable () -> Unit
) {
    when {
        permissionState.status.isGranted -> content()
        else -> {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = text)
                Button(onClick = { permissionState.launchPermissionRequest() }) {
                    Text(text = "Grant permission")
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun BleRequestPermissions(
    permissionState: MultiplePermissionsState,
    text: String,
    content: @Composable () -> Unit
) {
    when {
        permissionState.allPermissionsGranted -> content()
        else ->
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = text)
                Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                    Text(text = "Grant permissions")
                }
            }
    }
}

@Composable
internal fun TurnOnLocationServicesScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "To use this feature please turn on Location services to enable bluetooth scan")
    }
}

@Composable
internal fun TurnOnBtAdapterScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "To use this feature please turn on Bluetooth adapter")
    }
}