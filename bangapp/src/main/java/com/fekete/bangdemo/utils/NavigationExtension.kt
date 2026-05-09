package com.fekete.bangdemo.utils

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.navOptions

/**
 * Navigates to another fragment using its layout ID.
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
fun NavController.navigateMain(destinationId: Int) {
    if (currentDestination?.id == destinationId) return

    // pop all destinations between current state and desired destinationId, which returns user to that view
    val popped = popBackStack(destinationId, inclusive = false)

    // nothing was removed, meaning it was not accessed before, move to it
    if (!popped) {
        navigate(destinationId, null, navOptions {
            launchSingleTop = true
            restoreState = false
        })
    }
}

fun NavController.navigateDestination(destinationId: Int, args: Bundle? = null) {
    if (currentDestination?.id == destinationId) return

    navigate(destinationId, args, navOptions {
        launchSingleTop = true
    })
}


fun NavController.navigateAction(directions: NavDirections) {
    navigate(directions, navOptions {
        launchSingleTop = true
        restoreState = false
    })
}