package com.gaspar.gasparchat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GasparChatApplication @Inject constructor(): Application()

/**
 * Name of the application's shared preferences.
 */
const val GASPAR_CHAT_PREFERENCES = "gaspar_chat_preferences"