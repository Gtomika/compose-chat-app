package com.gaspar.gasparchat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GasparChatApplication @Inject constructor(): Application()