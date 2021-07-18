package com.gaspar.gasparchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gaspar.gasparchat.ui.theme.GasparChatTheme
import com.gaspar.gasparchat.view.*
import com.gaspar.gasparchat.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Handles navigation commands.
     */
    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    @ExperimentalComposeUiApi
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
           MainActivityContent(
               navigationDispatcher = navigationDispatcher,
               lifecycleOwner = this
           )
        }
    }
}

/**
 * Navigation component that is displayed in the single activity. See classes in the view package
 * for the displayed [Composable]s.
 * @param navigationDispatcher Object used to listen for incoming navigation events.
 * @param lifecycleOwner The activity's lifecycle owner.
 */
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun MainActivityContent(
    navigationDispatcher: NavigationDispatcher,
    lifecycleOwner: LifecycleOwner
) {
    GasparChatTheme {
        Surface(color = MaterialTheme.colors.background) {
            //create nav controller
            val navController = rememberNavController()
            //create chat room view model, this needs to exist ASAP
            val chatRoomViewModel = hiltViewModel<ChatRoomViewModel>()
            //build nav host, start is HOME, but it redirects to LOGIN if needed
            NavHost(navController = navController, startDestination = NavDest.HOME) {
                //redirect to login screen
                composable(route = NavDest.LOGIN) {
                    val viewModel = hiltViewModel<LoginViewModel>() //this view model will always reset when showing this screen
                    LoginContent(viewModel)
                }
                //redirect to register screen
                composable(route = NavDest.REGISTER) {
                    val viewModel = hiltViewModel<RegisterViewModel>() //this view model will always reset when showing this screen
                    RegisterContent(viewModel)
                }
                //redirect to home
                composable(route = NavDest.HOME) {
                    HomeContent()
                }
                //redirect to profile
                composable(route = NavDest.PROFILE) {
                    ProfileContent()
                }
                //redirect to search
                composable(route = NavDest.SEARCH) {
                    val viewModel = hiltViewModel<SearchViewModel>() //this view model will always reset when showing this screen
                    SearchContent(viewModel = viewModel)
                }
                //redirect to a chat room: REQUIRES CHAT UID
                composable(route = NavDest.CHAT_ROOM) {
                    ChatRoomContent(viewModel = chatRoomViewModel)
                }
            }
            //observe incoming navigation commands
            navigationDispatcher.navigationEmitter.observe(lifecycleOwner) { navigationCommand ->
                navigationCommand.invoke(navController)
            }
        }
    }
}