package com.pwitko.feature.blecomm.ui

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.pwitko.ble.BleClient
import com.pwitko.ble.connection.BleConnection
import com.pwitko.common.Failure
import com.pwitko.common.Loading
import com.pwitko.common.Success
import com.pwitko.feature.blecomm.ui.utils.BleRequestPermission
import com.pwitko.feature.blecomm.ui.utils.DisposableEffectWithLifeCycle
import com.pwitko.feature.blecomm.ui.utils.TurnOnBtAdapterScreen
import com.pwitko.feature.blecomm.ui.utils.TurnOnLocationServicesScreen

@Composable
fun HeartRateScreen() {
    val viewModel: HeartRateSensorViewModel = hiltViewModel()
    val btState = viewModel.btStateObservable.subscribeAsState(initial = viewModel.btStateObservable.value)
    DisposableEffectWithLifeCycle(
        onStart = { viewModel.refreshBtState() },
        onStop = { }
    )
    when(btState.value) {
        BleClient.BtState.BLUETOOTH_ADAPTER_OFF -> TurnOnBtAdapterScreen()
        BleClient.BtState.BT_PERMISSIONS_NOT_GRANTED -> {
            CheckConnectPermissions {
                HeartRateScreenContent(viewModel)
            }
        }
        BleClient.BtState.LOCATION_SERVICES_OFF -> TurnOnLocationServicesScreen()
        BleClient.BtState.READY -> HeartRateScreenContent(viewModel)
        else -> Unit
    }
}

@Composable
private fun HeartRateScreenContent(viewModel: HeartRateSensorViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            val connectionState = viewModel.connectionState.subscribeAsState(BleConnection.State.DISCONNECTED)
            Text(text = "Heart rate sensor on device ${viewModel.deviceAddress}")
            ConnectionStatus(connectionState.value)
            Spacer(modifier = Modifier.height(8.dp))
            HeartRateNotificationStatus(viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            HeartRateValue(viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            ReadBodyLocationValue(viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            WriteControlPoint(viewModel)
            DisconnectButton(connectionState.value, viewModel)
        }
    }
}

@Composable
private fun  HeartRateNotificationStatus(viewModel: HeartRateSensorViewModel) {
    val notificationsEnabled = viewModel.heartRateNotificationsEnabled.subscribeAsState(false)
    val status = if (notificationsEnabled.value) { "ENABLED" } else { "DISABLED" }
    Text(text = "Heart rate notifications: $status")
}

@Composable
private fun HeartRateValue(viewModel: HeartRateSensorViewModel) {
    val heartRate = viewModel.heartRate.subscribeAsState(initial = Loading)
    when(val rate = heartRate.value) {
        is Success -> Text(text = "Current heart rate: ${rate.data}")
        is Failure -> Text(text = "Error subscribing to heart rate notification")
        else -> Text(text = "Current heart rate: waiting for notification...")
    }
}

@Composable
private fun ConnectionStatus(connectionState: BleConnection.State) {
    Text(text = "Connection state: $connectionState")
}

@Composable
private fun DisconnectButton(
    connectionState: BleConnection.State,
    viewModel: HeartRateSensorViewModel
) {
    if (connectionState == BleConnection.State.CONNECTED) {
        Box {
            Button(onClick = { viewModel.disconnectClicked() }) {
                Text(text = "Disconnect")
            }
        }
    } else {
        Box {
            Button(onClick = { viewModel.connectClicked() }) {
                Text(text = "Connect")
            }
        }
    }
}

@Composable
private fun ReadBodyLocationValue(viewModel: HeartRateSensorViewModel) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val bodyLocation = viewModel.bodyLocation.subscribeAsState(initial = Loading)
        when (val result = bodyLocation.value) {
            is Success -> Text(text = "Sensor body location: ${result.data}", modifier = Modifier.weight(1f))
            is Failure -> Text(text = "Cannot read sensor body location...", modifier = Modifier.weight(1f))
            Loading -> Text(text = "Sensor body location: press button to read...", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(onClick = { viewModel.readBodyLocationClicked() }) {
            Text(text = "Read")
        }
    }
}

@Composable
private fun WriteControlPoint(viewModel: HeartRateSensorViewModel) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val controlPoint = viewModel.controlPoint.subscribeAsState(initial = Loading)
        when (val result = controlPoint.value) {
            is Success -> Text(text = "Control point: ${result.data}")
            is Failure -> Text(text = "Cannot write...")
            Loading -> Text(text = "Control point: none")
        }
        val value = rememberSaveable { mutableStateOf("") }
        Spacer(modifier = Modifier.width(16.dp))
        TextField(
            value = value.value,
            onValueChange = { value.value = it.take(2) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .border(width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.width(16.dp))
        val enableButton = value.value.isDigitsOnly() && value.value.isNotEmpty()
        Button(enabled = enableButton , onClick = { viewModel.writeClicked(value.value.toInt()) }) {
            Text(text = "Write")
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CheckConnectPermissions(content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val locationPermissionState = rememberPermissionState(android.Manifest.permission.BLUETOOTH_CONNECT)
        val text =
            "Bluetooth connection BLUETOOTH_CONNECT permission. Please grant " +
                    "it to access functionality"
        BleRequestPermission(locationPermissionState, text, content)
    } else {
        content()
    }
}