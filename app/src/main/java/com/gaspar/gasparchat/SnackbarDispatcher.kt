package com.gaspar.gasparchat

import androidx.compose.material.SnackbarHostState
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

typealias SnackbarCommand = (SnackbarHostState) -> Unit

/**
 * Can be used to show snackbar in composables that observe [snackbarEmitter]
 */
@ActivityRetainedScoped
class SnackbarDispatcher @Inject constructor() {

    /**
     * Emits snackbar show events.
     */
    val snackbarEmitter: SingleLiveEvent<SnackbarCommand> = SingleLiveEvent()

    /**
     * Emit a new snackbar show event.
     */
    fun dispatchSnackbarCommand(snackbarCommand: SnackbarCommand) {
        snackbarEmitter.value = snackbarCommand
    }

}