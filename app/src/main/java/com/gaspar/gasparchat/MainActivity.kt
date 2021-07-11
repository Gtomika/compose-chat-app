package com.gaspar.gasparchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gaspar.gasparchat.ui.theme.GasparChatTheme
import com.gaspar.gasparchat.view.HomeContent
import com.gaspar.gasparchat.view.LoginContent
import com.gaspar.gasparchat.view.RegisterContent
import com.gaspar.gasparchat.viewmodel.LoginViewModel
import com.gaspar.gasparchat.viewmodel.RegisterViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Handles navigation commands.
     */
    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

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
@Composable
fun MainActivityContent(
    navigationDispatcher: NavigationDispatcher,
    lifecycleOwner: LifecycleOwner
) {
    GasparChatTheme {
        Surface(color = MaterialTheme.colors.background) {
            //create nav controller
            val navController = rememberNavController()
            //build nav host, start is HOME, but it redirects to LOGIN if needed
            NavHost(navController = navController, startDestination = NavDest.HOME) {
                //redirect to login screen
                composable(route = NavDest.LOGIN) {
                    val viewModel = hiltViewModel<LoginViewModel>()
                    LoginContent(viewModel, lifecycleOwner)
                }
                //redirect to register screen
                composable(route = NavDest.REGISTER) {
                    val viewModel = hiltViewModel<RegisterViewModel>()
                    RegisterContent(viewModel, lifecycleOwner)
                }
                //redirect to home
                composable(route = NavDest.HOME) {
                    HomeContent(lifecycleOwner = lifecycleOwner)
                }
            }
            //observe incoming navigation commands
            navigationDispatcher.navigationEmitter.observe(lifecycleOwner) { navigationCommand ->
                navigationCommand.invoke(navController)
            }
        }
    }
}