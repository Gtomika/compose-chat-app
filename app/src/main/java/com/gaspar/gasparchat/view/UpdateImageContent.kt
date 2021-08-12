package com.gaspar.gasparchat.view

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.viewmodel.UpdatePictureViewModel
import com.gaspar.gasparchat.viewmodel.VoidMethod

@Composable
@ExperimentalAnimationApi
fun UpdatePictureContent(viewModel: UpdatePictureViewModel = hiltViewModel()) {
    val loading = viewModel.loading.collectAsState()
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.alpha(if (loading.value) 0.3f else 1f)
            ) {
                UpdatePictureTitle()
                UpdatePictureDetail()
                UpdatePicturePreviewAndDelete(viewModel = viewModel)
                UpdatePictureButtons(
                    onSelectClicked = viewModel::onPictureSelectClicked,
                    onCaptureClicked = viewModel::onPictureCaptureClicked
                )
                val newImageSelected = viewModel.newImageSelected.collectAsState()
                UpdatePictureSaveButton(
                    onSaveClicked = viewModel::onSaveClicked,
                    enabled = newImageSelected.value
                )
            }
        }
        //loading indicator
        AnimatedVisibility(visible = loading.value, modifier = Modifier.align(Alignment.Center)) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun UpdatePictureTitle() {
    Text(
        text = stringResource(id = R.string.profile_update_picture),
        style = MaterialTheme.typography.subtitle1,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun UpdatePictureDetail() {
    Text(
        text = stringResource(id = R.string.profile_update_picture_preview),
        style = MaterialTheme.typography.caption,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@ExperimentalAnimationApi
@Composable
fun UpdatePicturePreviewAndDelete(viewModel: UpdatePictureViewModel) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            val loading = viewModel.loading.collectAsState()
            val displayName = viewModel.displayName.collectAsState()
            AnimatedVisibility(
                visible = !loading.value,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                //not loading anymore: value hasDefaultPicture is now valid
                val hasDefaultPicture = viewModel.hasDefaultPicture.collectAsState()
                if(hasDefaultPicture.value) {
                    //this user has the default picture
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(id = R.string.search_profile_image_description,
                            formatArgs = arrayOf(displayName.value)),
                        modifier = Modifier.padding(end = 8.dp, top = 8.dp, bottom = 8.dp)
                    )
                } else {
                    //no default picture, custom one is in picture state
                    val picture = viewModel.picture.collectAsState()
                    ProfilePicture(picture = picture.value!!, displayName = displayName.value)
                }
            }
            //profile display name
            Text(
                text = displayName.value,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        //at the end place delete icon button
        val hasDefaultPicture = viewModel.hasDefaultPicture.collectAsState()
        IconButton(
            onClick = viewModel::onDeleteClicked,
            enabled = !hasDefaultPicture.value,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.CenterEnd)
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.profile_picture_delete))
        }
    }
}

@Composable
fun UpdatePictureButtons(
    onSelectClicked: VoidMethod,
    onCaptureClicked: VoidMethod
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        //select from gallery button
        TextButton(
            onClick = onSelectClicked,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.profile_update_picture_select))
        }
        //capture image with camera button
        TextButton(
            onClick = onCaptureClicked,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.profile_update_picture_capture))
        }
    }
}

@Composable
fun UpdatePictureSaveButton(
    onSaveClicked: VoidMethod,
    enabled: Boolean
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onSaveClicked,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.profile_save))
        }
    }
}

@Composable
fun ProfilePicture(
    picture: Bitmap,
    displayName: String
) {
    Image(
        bitmap = picture.asImageBitmap(),
        contentDescription = stringResource(id = R.string.search_profile_image_description,
            formatArgs = arrayOf(displayName)),
        modifier = Modifier
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .clip(CircleShape)
            .requiredSize(40.dp)
    )
}