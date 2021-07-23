package com.gaspar.gasparchat.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.gaspar.gasparchat.*
import com.gaspar.gasparchat.model.ChatRoom
import com.gaspar.gasparchat.model.ChatRoomRepository
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
class GroupsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRoomRepository: ChatRoomRepository,
    val snackbarDispatcher: SnackbarDispatcher,
    private val firebaseAuth: FirebaseAuth,
    private val navigationDispatcher: NavigationDispatcher
): ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /**
     * IF the create new groups dialog should be displayed.
     */
    private val _displayGroupDialog = MutableStateFlow(false)
    val displayGroupDialog: StateFlow<Boolean> = _displayGroupDialog

    /**
     * Groups of the user.
     */
    private val _groups = MutableStateFlow(listOf<ChatRoom>())
    val groups: StateFlow<List<ChatRoom>> = _groups

    init {
        EventBus.getDefault().register(this)
        loadGroups()
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGroupsChangedEvent(event: GroupsChangedEvent) {
        Log.d(TAG, "Received groups updated event, reloading groups!")
        loadGroups()
    }

    private fun loadGroups() {
        _loading.value = true
        val userUid = firebaseAuth.currentUser!!.uid
        chatRoomRepository.getGroupsOfUser(userUid).addOnCompleteListener { groupsResult ->
            if(groupsResult.isSuccessful && groupsResult.result != null) {
                val rawData = groupsResult.result!!.toObjects(ChatRoom::class.java)
                _groups.value = rawData.sortedBy { it.chatRoomName }
            } else {
                val message = context.getString(R.string.home_groups_fail)
                snackbarDispatcher.createOnlyMessageSnackbar(message)
                snackbarDispatcher.showSnackbar()
            }
            _loading.value = false
        }
    }


    fun showGroupDialog() {
        _displayGroupDialog.value = true
    }

    fun hideGroupDialog() {
        _displayGroupDialog.value = false
    }

    fun onGroupCreated(groupName: String, userUidList: List<String>) {
        hideGroupDialog()
        Log.d(TAG, "Creating group $groupName with ${userUidList.size} members...")
        _loading.value = true
        chatRoomRepository.createGroupChatRoom(
            chatRoomName = groupName,
            userUidList = userUidList,
            adminUid = firebaseAuth.currentUser!!.uid
        ).addOnCompleteListener { result ->
            _loading.value = false
            if(result.isSuccessful) {
                Log.d(TAG, "Group $groupName has been created!")
                val message = context.getString(R.string.group_create_success, groupName)
                snackbarDispatcher.createOnlyMessageSnackbar(message)
                snackbarDispatcher.showSnackbar()
                //update groups as they have changed
                EventBus.getDefault().post(GroupsChangedEvent)
            } else {
                val message = context.getString(R.string.group_create_fail)
                snackbarDispatcher.createOnlyMessageSnackbar(message)
                snackbarDispatcher.showSnackbar()
            }
        }
    }

    fun onGroupClicked(position: Int) {
        //open chat room of selected group
        val selectedGroup = groups.value[position]
        navigateToChatRoom(selectedGroup.chatUid)
    }

    private fun navigateToChatRoom(chatRoomUid: String) {
        Log.d(TAG, "Sending new chat room event, with chat room UID $chatRoomUid...")
        //send event to load chat room
        val event = ChatRoomChangedEvent(chatRoomUid)
        EventBus.getDefault().post(event)
        //navigate there
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.navigate(NavDest.CHAT_ROOM)
        }
    }
}