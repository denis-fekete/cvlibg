package com.fekete.bangdemo.utils

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.navOptions

/**
 * Navigates to another fragment using its layout ID. If [destinationId] already exists, everything on the back stack
 * will be popped, returning to the [destinationId], this way, no duplicate fragments and their views will be created.
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


/**
 * Navigates to [destinationId] using its [args]. If the current destination is already at [destinationId], navigation
 * is skipped.
 */
fun NavController.navigateDestination(destinationId: Int, args: Bundle? = null) {
    if (currentDestination?.id == destinationId) return

    navigate(destinationId, args, navOptions {
        launchSingleTop = true
    })
}

/**
 * Navigates using save args to [directions]. Prevents duplicate destination at the top of the stack.
 */
fun NavController.navigateAction(directions: NavDirections) {
    navigate(directions, navOptions {
        launchSingleTop = true
        restoreState = false
    })
}