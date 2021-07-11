package com.gaspar.gasparchat

import androidx.navigation.NavController
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

/**
 * Alias for navigation commands that user [NavController].
 */
typealias NavigationCommand = (NavController) -> Unit

/**
 * This class dispatches navigation events to the navigation controller, which observes [navigationEmitter].
 */
@ActivityRetainedScoped
class NavigationDispatcher @Inject constructor() {

    val navigationEmitter: SingleLiveEvent<NavigationCommand> = SingleLiveEvent()

    fun dispatchNavigationCommand(navigationCommand: NavigationCommand) {
        navigationEmitter.value = navigationCommand
    }

}