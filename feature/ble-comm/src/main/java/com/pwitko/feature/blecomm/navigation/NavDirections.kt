package com.pwitko.feature.blecomm.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.pwitko.navigation.NavCommand

object NavDirections {
    object Home: NavCommand {
        override val route: String = "home"
        override val arguments: List<NamedNavArgument> = emptyList()
    }

    object Scan: NavCommand {
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument("serviceUuid") { type = NavType.StringType }
        )
        override val route: String = "scanList/{serviceUuid}"

        internal fun create(serviceUuid: String) = object : NavCommand {
            override val arguments: List<NamedNavArgument> = this@Scan.arguments
            override val route: String = "scanList/$serviceUuid"
        }
    }

    val battery = object: NavCommand {
        override val arguments: List<NamedNavArgument> = emptyList()
        override val route: String = "battery/{deviceAddress}"
    }

    object HeartRate: NavCommand {
        override val arguments: List<NamedNavArgument> = listOf(
            navArgument("deviceAddress") { type = NavType.StringType }
        )
        override val route: String = "heartRate/{deviceAddress}"

        internal fun create(deviceAddress: String) = object : NavCommand {
            override val arguments: List<NamedNavArgument> = this@HeartRate.arguments
            override val route: String = "heartRate/$deviceAddress"
        }
    }
}