package com.gaspar.gasparchat.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import com.gaspar.gasparchat.*
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import javax.inject.Inject
import kotlin.math.min


@HiltViewModel
class UpdatePictureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    firebaseAuth: FirebaseAuth
): ViewModel() {

    /**
     * Picture download indicator.
     */
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    /**
     * Display name for preview.
     */
    private val _displayName = MutableStateFlow(if(firebaseAuth.currentUser != null) firebaseAuth.currentUser!!.displayName!! else "")
    val displayName: StateFlow<String> = _displayName

    /**
     * Picture bitmap used for preview and persisting the image.
     */
    private val _picture = MutableStateFlow<Bitmap?>(null)
    val picture: StateFlow<Bitmap?> = _picture

    /**
     * Stores if there is a new image selected, and this enables saving.
     */
    private val _newImageSelected = MutableStateFlow(false)
    val newImageSelected: StateFlow<Boolean> = _newImageSelected

    /**
     * Stores if the user has the default "Person" picture, or not.
     */
    private val _hasDefaultPicture = MutableStateFlow(true)
    val hasDefaultPicture: StateFlow<Boolean> = _hasDefaultPicture

    init {
        EventBus.getDefault().register(this)
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    /**
     * Opens gallery to allow user to select an image.
     */
    fun onPictureSelectClicked() {
       EventBus.getDefault().post(OpenGalleryEvent)
    }

    /**
     * Opens camera to allow users to take a new image.
     */
    fun onPictureCaptureClicked() {
        EventBus.getDefault().post(OpenCameraEvent)
    }

    /**
     * Called when the user obtains an image by either selecting or capturing. Contains the
     * image [Bitmap].
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: PictureObtainedEvent) {
        //get the center of the image
        val dimension = min(event.picture.width, event.picture.height)
        val centered = ThumbnailUtils.extractThumbnail(event.picture, dimension, dimension, ThumbnailUtils.OPTIONS_RECYCLE_INPUT)
        val scaled = Bitmap.createScaledBitmap(centered, PictureConstants.PROFILE_PICTURE_SIZE,
            PictureConstants.PROFILE_PICTURE_SIZE, true)
        centered.recycle()
        //recycles parameter
        val rotated = rotateImageIfNeeded(context, scaled, event.uri)
        _loading.value = false
        //user selected not default picture
        _hasDefaultPicture.value = false
        _newImageSelected.value = true
        //set new picture
        _picture.value = rotated
    }

    /**
     * Assumes there is a new image to be saved.
     */
    fun onSaveClicked() {
        //TODO: save image, set new image selected to false on success
        if(picture.value == null || !newImageSelected.value) {
            Log.d(TAG, "Error: no new picture when trying to save.")
            return
        }

    }
}

/**
 * This event is sent when the activity should open gallery.
 */
@Keep
object OpenGalleryEvent

/**
 * This event is sent when the activity should open camera.
 */
@Keep
object OpenCameraEvent

/**
 * This event is sent when the user obtains an image either by selecting or capturing.
 */
@Keep
data class PictureObtainedEvent(
    /**
     * Bitmap of the image that the user selected.
     */
    val picture: Bitmap,
    /**
     * URI of the picture.
     */
    val uri: Uri
)

fun registerGalleryResultContract(activity: MainActivity): ActivityResultLauncher<String> {
    //define what should happen on image selected
    return activity.registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            val picture = uriToBitmap(activity, uri)
            if(picture != null) {
                //send event with bitmap
                EventBus.getDefault().post(PictureObtainedEvent(picture = picture, uri = uri))
            }
        } else {
            Log.d(TAG, "No image received after selection.")
        }
    }
}

/**
 * Called by main activity to open a picture select intent with gallery.
 */
fun openGallery(activity: MainActivity) {
    activity.galleryResultContract.launch("image/*")
}

fun registerCameraResultContract(activity: MainActivity): ActivityResultLauncher<Uri> {
    //define what should happen on image captured
    return activity.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        //image was captured successfully: path is in temp URI
        if(success && activity.tempUri != null) {
            val picture = uriToBitmap(activity, activity.tempUri!!)
            if(picture != null) {
                //send event with bitmap
                EventBus.getDefault().post(PictureObtainedEvent(picture = picture, uri = activity.tempUri!!))
            }
        } else {
            Log.d(TAG, "No image received after selection.")
        }
    }
}

/**
 * Called by main activity to open a camera to take a picture.
 */
fun openCamera(activity: MainActivity) {
    activity.tempUri = getTmpFileUri(activity = activity)
    activity.cameraResultContract.launch(activity.tempUri)
}

private fun uriToBitmap(context: Context, selectedFileUri: Uri): Bitmap? {
    try {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(selectedFileUri, "r")
            ?: throw IOException()
        val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    } catch (e: IOException) {
        Log.d(TAG, "Failed to read selected image bitmap!")
        e.printStackTrace()
        return null
    }
}

private fun getTmpFileUri(activity: ComponentActivity): Uri {
    val tmpFile = File.createTempFile("tmp_image_file", ".png", activity.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(activity.applicationContext, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
}

