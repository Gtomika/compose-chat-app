package com.gaspar.gasparchat.model

import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject

/**
 * Contains method that can upload and download images using firebase storage.
 */
class PictureRepository @Inject constructor(
    private val firebaseStorage: FirebaseStorage
) {

}