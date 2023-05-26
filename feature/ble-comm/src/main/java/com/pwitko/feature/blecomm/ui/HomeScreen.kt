package com.pwitko.feature.blecomm.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen() {
    HomeScreenContent()
}

@Composable
internal fun HomeScreenContent(viewModel: HomeViewModel = hiltViewModel()) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Column {
            Text(text = "Please select service to interact with:")
            Button(onClick = { viewModel.heartSensorButtonClicked() }) {
                Text(text = "Heart Rate Sensor")
            }
        }
    }
}