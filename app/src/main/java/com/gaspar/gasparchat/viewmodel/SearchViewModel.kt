package com.gaspar.gasparchat.viewmodel

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaspar.gasparchat.*
import com.gaspar.gasparchat.model.InputField
import com.gaspar.gasparchat.model.User
import com.gaspar.gasparchat.model.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    val snackbarDispatcher: SnackbarDispatcher,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
): ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /**
     * Stores what is currently typed to the search bar.
     */
    private val _searchBar = MutableStateFlow(InputField())
    val searchBar: StateFlow<InputField> = _searchBar

    /**
     * List of users who are displayed as search result.
     */
    private val _searchResults = MutableStateFlow<List<User>>(listOf())
    val searchResults: StateFlow<List<User>> = _searchResults

    /**
     * This timer watches when the user stopped typing.
     */
    private var searchStarterTimer: CountDownTimer? = null

    /**
     * The currently logged in user, or null if for some error the user could not be obtained.
     */
    var user: User? = null
        private set

    //get the current user, to gain access to contacts and other info
    init {
        userRepository.getCurrentUser().addOnCompleteListener { userQueryResult ->
            if(userQueryResult.isSuccessful) {
                if(userQueryResult.result != null) {
                    try {
                        user = userQueryResult.result!!.toObjects(User::class.java)[0]
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

    /**
     * Goes back to home screen.
     */
    fun goBack() {
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.popBackStack(NavDest.HOME, false)
        }
    }

    /**
     * Called when the typed value in the search bar changes. Validates the result.
     */
    fun onSearchBarValueChanged(newValue: String) {
        when {
            newValue.length < SearchValues.MIN_LENGTH -> {
                //too short
                val message = context.getString(R.string.search_too_short, SearchValues.MIN_LENGTH)
                _searchBar.value = searchBar.value.copy(input = newValue, isError = true, errorMessage = message)
            }
            newValue.length > SearchValues.MAX_LENGTH -> {
                //too long
                val message = context.getString(R.string.search_too_long, SearchValues.MAX_LENGTH)
                _searchBar.value = searchBar.value.copy(input = newValue, isError = true, errorMessage = message)
            }
            else -> {
                //no errors
                _searchBar.value = searchBar.value.copy(input = newValue, isError = false)
            }
        }
        //restart the timer
        searchStarterTimer?.cancel()
        searchStarterTimer = object : CountDownTimer(SearchValues.SEARCH_DELAY, SearchValues.SEARCH_DELAY) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                //begin search
                searchForUsers(searchBar.value.input)
            }
        }.start()
    }

    /**
     * If there are no errors, this looks up users whose display name matches the search bar's value.
     * @param searchString The search bar's value.
     */
    fun searchForUsers(searchString: String) {
        if(!searchBar.value.isError) {
            Log.d(TAG, "Search begins with string $searchString...")
            //indicate loading
            _loading.value = true
            val matchingUsers = mutableListOf<User>()
            userRepository.getUsers().addOnCompleteListener { userQueryResult ->
                if(userQueryResult.isSuccessful) {
                    //search successful
                    if(userQueryResult.result != null) {
                        //Getting all users is really bad in a large app, this is NOT scalable
                        val allUsers = userQueryResult.result?.toObjects(User::class.java)
                        if(allUsers != null) {
                            for(user in allUsers) {
                                //if the search string is in the name, save it
                                if(user.displayName.contains(searchString, ignoreCase = true)) {
                                    matchingUsers.add(user)
                                }
                            }
                            //update list
                            _searchResults.value = matchingUsers.toList()
                        } else {
                            //some reason it could not be parsed
                            _searchResults.value = listOf() //this will display the no results text
                        }
                        _loading.value = false
                    } else {
                        //result is null for some reason
                        _searchResults.value = listOf() //this will display the no results text
                        _loading.value = false
                    }
                } else {
                    _loading.value = false
                    //the search failed
                    val message = context.getString(R.string.search_failed)
                    showSnackbar(message)
                }
            }
        }
    }

    /**
     * Called when an search result (describing a [User]) was clicked in the search result list.
     * @param position The position of the result in the list.
     */
    fun onSearchResultClicked(position: Int) {
        Log.d(TAG, "Search result of ${searchResults.value[position].displayName} was clicked!")
    }

    /**
     * Called when one of the search results is added as a contact to the current user.
     */
    fun onAddAsContactClicked(position: Int, isContactState: MutableState<Boolean>) {
        //signal a contact list update
        EventBus.getDefault().post(ContactsChangedEvent)
        Log.d(TAG, "{${user?.displayName}} added ${searchResults.value[position].displayName} as a contact!")
        userRepository.addUserContact(user!!, searchResults.value[position].uid)
            ?.addOnCompleteListener { result ->
               if(result.isSuccessful) {
                   //update the search result list to show the added user as a contact
                   isContactState.value = true
                   //show snackbar
                   val message = context.getString(R.string.search_added_as_contact, searchResults.value[position].displayName)
                   showSnackbar(message)
               }
            }
    }

    /**
     * Called when a search result is selected as blocked user.
     */
    fun onBlockUserClicked(position: Int, isBlockedState: MutableState<Boolean>) {
        //signal a block list update
        EventBus.getDefault().post(BlocklistChangedEvent)
        Log.d(TAG, "{${user?.displayName}} blocked ${searchResults.value[position].displayName}!")
        userRepository.addUserBlock(user!!, searchResults.value[position].uid)
            ?.addOnSuccessListener {
                //update state so screen recomposes
                isBlockedState.value = true
                //show snackbar
                val message = context.getString(R.string.search_block_successful, searchResults.value[position].displayName)
                val actionLabel = context.getString(R.string.undo)
                val onActionClicked = {
                    userRepository.removeUserBlock(user!!, searchResults.value[position].uid)
                        ?.addOnSuccessListener {
                            isBlockedState.value = false
                        }
                }
                showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = SnackbarDuration.Long,
                    onActionClicked =  { onActionClicked.invoke() }
                )
            }
    }

    /**
     * Quick way to show a snackbar.
     * @param message Message to show.
     */
    private fun showSnackbar(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
        actionLabel: String? = null,
        onActionClicked: VoidMethod = {}
    ) {
        snackbarDispatcher.dispatchSnackbarCommand { snackbarHostState ->
            viewModelScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = duration
                )
                when(result) {
                    SnackbarResult.ActionPerformed -> onActionClicked.invoke()
                    SnackbarResult.Dismissed -> { }
                }
            }
        }
    }
}