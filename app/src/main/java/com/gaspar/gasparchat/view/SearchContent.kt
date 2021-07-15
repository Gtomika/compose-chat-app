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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.model.InputField
import com.gaspar.gasparchat.model.User
import com.gaspar.gasparchat.viewmodel.SearchViewModel
import com.gaspar.gasparchat.viewmodel.StringMethod
import com.gaspar.gasparchat.viewmodel.VoidMethod
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Composable
fun SearchContent(viewModel: SearchViewModel) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { SearchTopBar(onBackClicked = viewModel::goBack) },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                LoadingIndicator(viewModel.loading)
                SearchBody(viewModel = viewModel)
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
fun SearchBody(viewModel: SearchViewModel) {
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
                    SearchResultContent(
                        user = user,
                        position = position,
                        onSearchResultClicked = viewModel::onSearchResultClicked
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
    onSearchResultClicked: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clickable { onSearchResultClicked.invoke(position) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
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
    }
}