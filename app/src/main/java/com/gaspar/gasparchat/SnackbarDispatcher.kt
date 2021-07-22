package com.gaspar.gasparchat

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.gaspar.gasparchat.viewmodel.VoidMethod
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
/**
 * Can be used to show snackbar in composables when the command comes from a view model.
 */
class SnackbarDispatcher {

    private val _showSnackbar = MutableStateFlow(false)
    val showSnackbar: StateFlow<Boolean> = _showSnackbar

    private val _snackbarMessage = MutableStateFlow("")
    val snackbarMessage: StateFlow<String> = _snackbarMessage

    private val _snackbarActionLabel = MutableStateFlow<String?>(null)
    val snackbarActionLabel: StateFlow<String?> = _snackbarActionLabel

    private val _snackbarAction = MutableStateFlow {}
    val snackbarAction: StateFlow<VoidMethod> = _snackbarAction

    fun setSnackbarMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun createOnlyMessageSnackbar(message: String) {
        setSnackbarMessage(message)
        setSnackbarLabel(null)
        setSnackbarAction {  }
    }

    fun setSnackbarLabel(label: String?) {
        _snackbarActionLabel.value = label
    }

    fun setSnackbarAction(action: VoidMethod) {
        _snackbarAction.value = action
    }

    fun showSnackbar() {
        _showSnackbar.value = true
    }

    fun hideSnackbar() {
        _showSnackbar.value = false
    }
}

@Composable
fun WatchForSnackbar(snackbarDispatcher: SnackbarDispatcher, snackbarHostState: SnackbarHostState) {
    val showSnackbar = snackbarDispatcher.showSnackbar.collectAsState()
    val message = snackbarDispatcher.snackbarMessage.collectAsState()
    val actionLabel = snackbarDispatcher.snackbarActionLabel.collectAsState()
    val action = snackbarDispatcher.snackbarAction.collectAsState()

    if(showSnackbar.value) {
        LaunchedEffect(showSnackbar.value) {
            val result = snackbarHostState.showSnackbar(
                message = message.value,
                actionLabel = actionLabel.value,
                duration = SnackbarDuration.Short
            )
            when(result) {
                SnackbarResult.Dismissed -> {
                    snackbarDispatcher.hideSnackbar()
                }
                SnackbarResult.ActionPerformed -> {
                    snackbarDispatcher.hideSnackbar()
                    action.value.invoke()
                }
            }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
class SnackbarModule {

    @Provides
    fun provideSnackbarDispatcher(): SnackbarDispatcher {
        return SnackbarDispatcher()
    }

}