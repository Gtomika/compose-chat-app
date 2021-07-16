package com.gaspar.gasparchat.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gaspar.gasparchat.NavDest
import com.gaspar.gasparchat.R

/**
 * These are the possible states of the home screen sub navigation.
 */
sealed class HomeNavigationItem(

    /**
     * Sub navigation route (in home screen).
     */
    var route: String,

    /**
     * Image icon of this menu item.
     */
    @DrawableRes
    var icon: Int,

    /**
     * Title of this menu item.
     */
    @StringRes
    var title: Int

) {

    object Chats : HomeNavigationItem(
        route = NavDest.HOME_CHATS,
        icon = R.drawable.icon_chat,
        title = R.string.home_chats
    )

    object Contacts: HomeNavigationItem(
        route = NavDest.HOME_CONTACTS,
        icon = R.drawable.icon_contacts,
        title = R.string.home_contacts
    )

    object Blocked: HomeNavigationItem(
        route = NavDest.HOME_BLOCKED,
        icon = R.drawable.icon_block,
        title = R.string.home_blocked
    )
}