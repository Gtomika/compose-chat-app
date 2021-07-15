package com.gaspar.gasparchat.view

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gaspar.gasparchat.NavDest
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.model.HomeNavigationItem
import com.gaspar.gasparchat.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Contents of the home screen.
 */
@ExperimentalAnimationApi
@Composable
fun HomeContent() {
    val viewModel = hiltViewModel<HomeViewModel>()
    val firebaseUser = viewModel.firebaseAuth.currentUser
    if(firebaseUser == null) {
        //there is nobody logged in, redirect to home
        viewModel.redirectToLogin()
        return
    }
    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()
    val displayName = viewModel.displayName.collectAsState()
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            HomeTopBar(
                onProfileClicked = viewModel::redirectToProfile,
                onSearchClicked = viewModel::redirectToSearch,
                displayName = displayName.value
            )
        },
        content = {
            Box(modifier = Modifier.fillMaxWidth()) {
                //loading indicator is always on top
                LoadingIndicator(loadingFlow = viewModel.loading)
                //actual content is a navigation host
                NavHost(navController = navController, startDestination = NavDest.HOME_CHATS) {
                    composable(route = NavDest.HOME_CHATS) {
                        ChatsContent()
                    }
                    composable(route = NavDest.HOME_CONTACTS) {
                        ContactsContent()
                    }
                }
            }
        },
        bottomBar = { HomeBottomNavigationBar(navController) }
    )
    //watch for snackbar
    LaunchedEffect(key1 = viewModel, block = {
        launch {
            viewModel.snackbarDispatcher.snackbarEmitter.collect { snackbarCommand ->
                snackbarCommand?.invoke(scaffoldState.snackbarHostState)
            }
        }
    })
}

@Composable
fun HomeTopBar(
    onProfileClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    displayName: String
) {
    TopAppBar(
        title = {
            Text(
                stringResource(
                    id = R.string.home_title,
                    formatArgs = arrayOf(displayName)
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onProfileClicked) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(id = R.string.profile_title)
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClicked) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(id = R.string.search_title)
                )
            }
        }
    )
}

@Composable
fun HomeBottomNavigationBar(
    navController: NavController
) {
    val navItems = listOf(
        HomeNavigationItem.Chats,
        HomeNavigationItem.Contacts
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    BottomNavigation(modifier = Modifier.fillMaxWidth()) {
        navItems.forEach { navItem ->
            //for each navigation item, add a composable
            BottomNavigationItem(
                selected = currentRoute == navItem.route,
                onClick = {
                    navController.navigate(navItem.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        // Avoid multiple copies of the same destination when
                        // re-selecting the same item
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = navItem.icon),
                        contentDescription = stringResource(id = navItem.title)
                    )
                },
                label = {
                    Text(text = stringResource(id = navItem.title))
                },
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
fun ChatsContent() {
    Text(text = "Chats")
    //TODO
}

@Composable
fun ContactsContent() {
    Text(text = "Contacts")
    //TODO
}