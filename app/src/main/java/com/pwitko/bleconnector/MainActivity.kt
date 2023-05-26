package com.pwitko.bleconnector

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.remember
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pwitko.bleconnector.theme.ComposeappTheme
import com.pwitko.feature.blecomm.navigation.NavDirections
import com.pwitko.feature.blecomm.ui.HeartRateScreen
import com.pwitko.feature.blecomm.ui.HomeScreen
import com.pwitko.feature.blecomm.ui.ScanScreen
import com.pwitko.navigation.NavCommand
import com.pwitko.navigation.NavManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    internal lateinit var navManager: NavManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeappTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val navController = rememberNavController()
                    val initial = remember {
                        object: NavCommand {
                            override val arguments: List<NamedNavArgument> = emptyList()
                            override val route: String = ""
                        }
                    }
                    navManager.commands.subscribeAsState(initial).value.also { command ->
                        if (command.route.isNotEmpty()) {
                            navController.navigate(command.route) {
                                launchSingleTop = true
                                popUpTo(NavDirections.Home.route)
                            }
                        }
                    }
                    NavHost(navController, NavDirections.Home.route) {
                        composable(NavDirections.Home.route) { HomeScreen() }

                        composable(
                            NavDirections.Scan.route,
                            NavDirections.Scan.arguments
                        ) { ScanScreen() }

                        composable(
                            NavDirections.HeartRate.route,
                            NavDirections.HeartRate.arguments
                        ) { HeartRateScreen() }
                    }
                }
            }
        }
    }
}