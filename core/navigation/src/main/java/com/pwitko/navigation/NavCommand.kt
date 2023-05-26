package com.pwitko.navigation

import androidx.navigation.NamedNavArgument

interface NavCommand {
    val arguments: List<NamedNavArgument>
    val route: String
}