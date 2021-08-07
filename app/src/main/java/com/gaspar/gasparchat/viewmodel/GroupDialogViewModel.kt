package com.gaspar.gasparchat.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.gaspar.gasparchat.*
import com.gaspar.gasparchat.model.InputField
import com.gaspar.gasparchat.model.User
import com.gaspar.gasparchat.model.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

@HiltViewModel
class GroupDialogViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
): ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /**
     * The dialog will only enable the ok button if this state is true.
     */
    private val _validGroupState = MutableStateFlow(false)
    val validGroupState: StateFlow<Boolean> = _validGroupState

    /**
     * Stores the chat room name that this dialog is creating.
     */
    private val _groupName = MutableStateFlow(InputField())
    val groupName: StateFlow<InputField> = _groupName

    /**
     * Stores the list of user UIDs who are going to be added to the created group. Automatically contains the
     * current user.
     */
    private val _selectedUsers = MutableStateFlow(listOf(firebaseAuth.currentUser!!.uid))
    val selectedUsers: StateFlow<List<String>> = _selectedUsers

    /**
     * All the users friends are loaded in here.
     */
    private val _friendList = MutableStateFlow(listOf<User>())
    val friendList: StateFlow<List<User>> = _friendList

    init {
        EventBus.getDefault().register(this)
        getCurrentUserAndFriends()
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFriendListUpdatedEvent(event: FriendsChangedEvent) {
        //refresh friend list
        getCurrentUserAndFriends()
    }

    /**
     * Loads the current users friends from firestore into [friendList].
     */
    private fun getCurrentUserAndFriends() {
        _loading.value = true
        userRepository.getCurrentUser().addOnCompleteListener { currentUserResult ->
            if (currentUserResult.isSuccessful) {
                val currentUser = currentUserResult.result!!.toObjects(User::class.java)[0]
                //get contact users
                if (currentUser.friends.isNotEmpty()) {
                    userRepository.getUsersByUid(currentUser.friends)
                        .addOnCompleteListener { contactsQueryResult ->
                            _loading.value = false
                            if (contactsQueryResult.isSuccessful && contactsQueryResult.result != null) {
                                _friendList.value = contactsQueryResult.result!!.toObjects(User::class.java)
                            }
                        }
                } else {
                    //no friends
                    _loading.value = false
                }
            } else {
                //failed to load friends
                _loading.value = false
            }
        }
    }

    fun onGroupNameChanged(newName: String) {
        when {
            newName.isBlank() -> {
                val message = context.getString(R.string.login_cannot_be_empty)
                _groupName.value = groupName.value.copy(input = newName, isError = true, errorMessage = message)
                _validGroupState.value = false
            }
            newName.length !in NameLimits.MIN..NameLimits.MAX -> {
                val message = context.getString(R.string.register_name_incorrect_length, NameLimits.MIN, NameLimits.MAX)
                _groupName.value = groupName.value.copy(input = newName, isError = true, errorMessage = message)
                _validGroupState.value = false
            }
            else -> {
                _groupName.value = groupName.value.copy(input = newName, isError = false)
                //for valid state friends need to be selected as well
                _validGroupState.value = selectedUsers.value.size > 1
            }
        }
    }

    fun isFriendSelected(friendUid: String): Boolean {
        return selectedUsers.value.contains(friendUid)
    }

    fun onFriendSelected(position: Int) {
        _selectedUsers.value = selectedUsers.value + friendList.value[position].uid
        Log.d(TAG, "Selected user UIDs: ${selectedUsers.value}")
        //is it valid after?
        _validGroupState.value =
                    selectedUsers.value.size in 2..GroupLimits.MAX_MEMBERS &&
                    !groupName.value.isError && groupName.value.input.isNotBlank()
    }

    fun onFriendUnselected(position: Int) {
        _selectedUsers.value = selectedUsers.value - friendList.value[position].uid
        Log.d(TAG, "Selected user UIDs: ${selectedUsers.value}")
        //is it valid after?
        _validGroupState.value =
            selectedUsers.value.size in 2..GroupLimits.MAX_MEMBERS &&
            !groupName.value.isError && groupName.value.input.isNotBlank()
    }
}