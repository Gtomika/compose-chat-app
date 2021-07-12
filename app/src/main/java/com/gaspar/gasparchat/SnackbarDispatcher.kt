package com.gaspar.gasparchat

import androidx.compose.material.SnackbarHostState
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val _snackbarEmitter: MutableStateFlow<SnackbarCommand?> = MutableStateFlow(null)
    val snackbarEmitter: StateFlow<SnackbarCommand?> = _snackbarEmitter

    /**
     * Emit a new snackbar show event.
     */
    fun dispatchSnackbarCommand(snackbarCommand: SnackbarCommand) {
        _snackbarEmitter.value = snackbarCommand
    }

}