package com.gaspar.gasparchat.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.gaspar.gasparchat.NavDest
import com.gaspar.gasparchat.NavigationDispatcher
import com.gaspar.gasparchat.SnackbarDispatcher
import com.gaspar.gasparchat.TAG
import com.gaspar.gasparchat.model.User
import com.gaspar.gasparchat.model.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    val snackbarDispatcher: SnackbarDispatcher,
    val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
): ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName

    /**
     * The currently logged in user.
     */
    var user: User? = null

    //get the current user, to gain access to contacts and other info
    init {
        userRepository.getCurrentUser().addOnCompleteListener { userQueryResult ->
            if(userQueryResult.isSuccessful) {
                if(userQueryResult.result != null) {
                    try {
                        user = userQueryResult.result!!.toObjects(User::class.java)[0]
                        //update display name
                        _displayName.value = user!!.displayName
                    } catch (e: Exception) {
                        Log.d(TAG, "Failed to query current user: exception, user possibly not present!")
                    }
                } else {
                    Log.d(TAG, "Failed to query current user: no result included!")
                }
            } else {
                Log.d(TAG, "Failed to query current user!")
            }
        }
    }

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