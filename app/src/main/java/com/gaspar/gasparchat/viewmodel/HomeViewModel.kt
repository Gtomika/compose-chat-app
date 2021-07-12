package com.gaspar.gasparchat.viewmodel

import androidx.lifecycle.ViewModel
import com.gaspar.gasparchat.NavDest
import com.gaspar.gasparchat.NavigationDispatcher
import com.gaspar.gasparchat.SnackbarDispatcher
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    val snackbarDispatcher: SnackbarDispatcher,
    val firebaseAuth: FirebaseAuth
): ViewModel() {

    private val _loading = MutableStateFlow<Boolean>(true)
    val loading: StateFlow<Boolean> = _loading

    fun redirectToLogin() {
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.popBackStack()
            navController.navigate(NavDest.LOGIN)
        }
    }

    fun redirectToProfile() {
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.navigate(NavDest.PROFILE)
        }
    }

    fun redirectToSearch() {
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.navigate(NavDest.SEARCH)
        }
    }

}