package com.gaspar.gasparchat.view

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.model.InputField
import com.gaspar.gasparchat.model.User
import com.gaspar.gasparchat.model.isBlockedBy
import com.gaspar.gasparchat.model.isContactOf
import com.gaspar.gasparchat.viewmodel.SearchViewModel
import com.gaspar.gasparchat.viewmodel.StringMethod
import com.gaspar.gasparchat.viewmodel.VoidMethod
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Composable
fun SearchContent(
    viewModel: SearchViewModel,
    blockListUpdateState: MutableState<Boolean>,
    contactListUpdateState: MutableState<Boolean>
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { SearchTopBar(onBackClicked = viewModel::goBack) },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                LoadingIndicator(viewModel.loading)
                SearchBody(
                    viewModel = viewModel,
                    blockListUpdateState = blockListUpdateState,
                    contactListUpdateState = contactListUpdateState
                )
            }
        }
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
fun SearchTopBar(onBackClicked: VoidMethod) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.profile_back)
                )
            }
        },
        title = { Text(text = stringResource(id = R.string.search_title)) }
    )
}

@ExperimentalComposeUiApi
@Composable
fun SearchBody(
    viewModel: SearchViewModel,
    blockListUpdateState: MutableState<Boolean>,
    contactListUpdateState: MutableState<Boolean>
) {
    Column( //this is NOT the scrollable column
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SearchBar(
            searchBarFlow = viewModel.searchBar,
            loadingFlow = viewModel.loading,
            onSearchBarValueChanged = viewModel::onSearchBarValueChanged
        )
        val searchResult = viewModel.searchResults.collectAsState()
        if(searchResult.value.isNotEmpty()) {
            //there are results to be displayed
            LazyColumn {
                itemsIndexed(searchResult.value) { position: Int, user: User ->
                    val isContact = remember(user) { mutableStateOf(isContactOf(viewModel.user!!, user)) }
                    val isBlocked = remember(user) { mutableStateOf(isBlockedBy(viewModel.user!!, user)) }
                    SearchResultContent(
                        user = user,
                        position = position,
                        onSearchResultClicked = viewModel::onSearchResultClicked,
                        onAddAsContactClicked = viewModel::onAddAsContactClicked,
                        onBlockClicked = viewModel::onBlockUserClicked,
                        isContact = isContact,
                        isBlocked = isBlocked,
                        blockListUpdateState = blockListUpdateState,
                        contactListUpdateState = contactListUpdateState
                    )
                }
            }
        } else {
            //no results
            Text(
                text = stringResource(id = R.string.search_results_empty),
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun SearchBar(
    searchBarFlow: StateFlow<InputField>,
    loadingFlow: StateFlow<Boolean>,
    onSearchBarValueChanged: StringMethod
) {
    val searchBar = searchBarFlow.collectAsState()
    val loading = loadingFlow.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = searchBar.value.input,
        onValueChange = onSearchBarValueChanged,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(id = R.string.search_title)
            )
        },
        isError = searchBar.value.isError,
        label = {
            val text = if(searchBar.value.isError) searchBar.value.errorMessage else stringResource(R.string.search_bar_label)
            Text(text = text)
        },
        maxLines = 1,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { keyboardController?.hide() },
        ),
        enabled = !loading.value //disable when loading
    )
}

@Composable
fun SearchResultContent(
    user: User,
    position: Int,
    onSearchResultClicked: (Int) -> Unit,
    onAddAsContactClicked: (Int, MutableState<Boolean>) -> Unit,
    onBlockClicked: (Int, MutableState<Boolean>) -> Unit,
    isContact: MutableState<Boolean>,
    isBlocked: MutableState<Boolean>,
    blockListUpdateState: MutableState<Boolean>,
    contactListUpdateState: MutableState<Boolean>
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clickable { onSearchResultClicked.invoke(position) },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                //TODO: when implemented, this can be replaced with profile picture
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(id = R.string.search_profile_image_description, formatArgs = arrayOf(user.displayName)),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            when {
                isContact.value -> { //ALREADY know this user: show that instead of buttons
                    Text(
                        text = stringResource(id = R.string.search_already_contact),
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp),
                        style = MaterialTheme.typography.body1
                    )
                }
                isBlocked.value -> { //ALREADY blocked this user: show that instead of buttons
                    Text(
                        text = stringResource(id = R.string.search_blocked),
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp),
                        style = MaterialTheme.typography.body1
                    )
                }
                else -> { //no contact with this user so far
                    Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                        //action icon: add to contacts
                        IconButton(onClick = {
                            onAddAsContactClicked.invoke(position, isContact)
                            contactListUpdateState.value = true //signal other composables that care about contact list
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(id = R.string.search_add_contact_description)
                            )
                        }
                        //action icon: block
                        IconButton(onClick = {
                            onBlockClicked.invoke(position, isBlocked)
                            blockListUpdateState.value = true //signal other composables that care about blocked users
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.icon_block),
                                contentDescription = stringResource(id = R.string.search_blocked)
                            )
                        }
                    }
                }
            }
        }

    }
}