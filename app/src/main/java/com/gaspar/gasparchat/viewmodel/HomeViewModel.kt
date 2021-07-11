package com.gaspar.gasparchat.viewmodel

import androidx.lifecycle.ViewModel
import com.gaspar.gasparchat.NavDest
import com.gaspar.gasparchat.NavigationDispatcher
import com.gaspar.gasparchat.SnackbarDispatcher
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    val snackbarDispatcher: SnackbarDispatcher,
    val firebaseAuth: FirebaseAuth
): ViewModel() {

    fun redirectToLogin() {
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.popBackStack()
            navController.navigate(NavDest.LOGIN)
        }
    }

}