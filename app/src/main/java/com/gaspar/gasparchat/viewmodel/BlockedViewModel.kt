package com.gaspar.gasparchat.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaspar.gasparchat.BlocklistChangedEvent
import com.gaspar.gasparchat.GasparChatApplication
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.SnackbarDispatcher
import com.gaspar.gasparchat.model.User
import com.gaspar.gasparchat.model.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

@HiltViewModel
class BlockedViewModel @Inject constructor(
    val snackbarDispatcher: SnackbarDispatcher,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
): ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _currentUser = MutableStateFlow(User())
    val currentUser: StateFlow<User> = _currentUser

    private val _blockedUsers = MutableStateFlow(listOf<User>())
    val blockedUsers: StateFlow<List<User>> = _blockedUsers

    init {
        EventBus.getDefault().register(this)
        getCurrentUserAndBlocks()
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    /**
     * Called when another component signaled that the user's block list changed.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBlockListChangedEvent(event: BlocklistChangedEvent) {
        //update state
        getCurrentUserAndBlocks()
    }

    fun getCurrentUserAndBlocks() {
        _loading.value = true
        userRepository.getCurrentUser().addOnCompleteListener { currentUserResult ->
            if(currentUserResult.isSuccessful) {
                _currentUser.value = currentUserResult.result!!.toObjects(User::class.java)[0]
                //get contact users
                if(_currentUser.value.blockedUsers.isNotEmpty()) {
                    userRepository.getUsersByUid(currentUser.value.blockedUsers).addOnCompleteListener { blocksQueryResult ->
                        _loading.value = false
                        if(blocksQueryResult.isSuccessful && blocksQueryResult.result != null) {
                            _blockedUsers.value = blocksQueryResult.result!!.toObjects(User::class.java)
                        } else {
                            //failed to get blocklist
                            showBlocklistLoadErrorSnackbar()
                        }
                    }
                } else {
                    _loading.value = false
                    //no users to query
                    _blockedUsers.value = listOf()
                }
            } else {
                _loading.value = false
                //failed to get current user
                showBlocklistLoadErrorSnackbar()
            }
        }
    }

    /**
     * Called when the user unblocks a blocked user.
     * @param position The position of the unblocked user in [blockedUsers]
     */
    fun onUnblockClicked(position: Int) {
        val unblockedUser = blockedUsers.value[position]
        val unblockUid = unblockedUser.uid
        val unblockDisplayName = unblockedUser.displayName
        userRepository.removeUserBlock(currentUser.value, unblockUid)
            ?.addOnCompleteListener { unblockResult ->
                if(unblockResult.isSuccessful) {
                    //the database was updated, now update the state
                    _currentUser.value = currentUser.value.copy(blockedUsers = currentUser.value.blockedUsers - unblockUid)
                    //update list
                    val mutableBlockList = mutableListOf<User>()
                    var indexOfUnblocked = 0
                    blockedUsers.value.forEachIndexed { index, blockedUser ->
                        if(blockedUser.uid == unblockUid) {
                            //this was the user who was unblocked
                            indexOfUnblocked = index
                        } else {
                            //this was not the unblocked, add it
                            mutableBlockList.add(blockedUser)
                        }
                    }
                    //reassign the block list state
                    _blockedUsers.value = mutableBlockList.toList()
                    //show success snackbar with re-block option
                    val message = context.getString(R.string.home_unblock_success, unblockDisplayName)
                    val actionLabel = context.getString(R.string.undo)
                    val onActionClicked = { reBlockUser(indexOfUnblocked, unblockedUser) }
                    showSnackbar(
                        message = message,
                        actionLabel = actionLabel,
                        duration = SnackbarDuration.Long,
                        onActionClicked = onActionClicked
                    )
                } else {
                    val message = context.getString(R.string.home_unblock_failed)
                    showSnackbar(message)
                }
            }
    }

    private fun reBlockUser(indexOfUnblocked: Int, unblockedUser: User) {
        val mutableBlockList = mutableListOf<User>()
        mutableBlockList.addAll(blockedUsers.value) //this does not include unblocked user anymore
        //re add it
        mutableBlockList.add(indexOfUnblocked, unblockedUser)
        //update state
        _blockedUsers.value = mutableBlockList.toList()
        //send to database
        userRepository.addUserBlock(user = currentUser.value, unblockedUser.uid)
    }

    private fun showBlocklistLoadErrorSnackbar() {
        val message = context.getString(R.string.home_block_error)
        val actionLabel = context.getString(R.string.retry)
        val onActionClicked = { getCurrentUserAndBlocks() } //retry action
        showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long,
            onActionClicked = onActionClicked,
        )
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