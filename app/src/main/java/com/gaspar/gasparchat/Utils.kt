package com.gaspar.gasparchat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.exifinterface.media.ExifInterface
import com.gaspar.gasparchat.viewmodel.VoidMethod

/**
 * Rotates a bitmap if its rotation is not 0. The rotation is info is read from URI. The
 * original bitmap, [photo] will be recycled.
 * @param context Context.
 * @param photo The bitmap.
 * @param photoPath The URI of the image.
 * @return The rotated bitmap.
 */
fun rotateImageIfNeeded(context: Context, photo: Bitmap, photoPath: Uri): Bitmap {
    val inputStream = context.contentResolver.openInputStream(photoPath)
    if(inputStream == null) {
        Log.d(TAG, "Input stream was null, returning original image.")
        return photo
    }
    val ei = ExifInterface(inputStream)
    val orientation: Int = ei.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_UNDEFINED
    )
    val rotatedBitmap = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(photo, 90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(photo, 180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(photo, 270f)
        ExifInterface.ORIENTATION_NORMAL -> photo
        else -> photo
    }
    photo.recycle()
    return rotatedBitmap
}

/**
 * Rotates bitmap with an angle.
 * @param source The bitmap to be rotated.
 * @param angle The angle of the rotation.
 * @return The rotated bitmap.
 */
private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(
        source, 0, 0, source.width, source.height,
        matrix, true
    )
}

/**
 * Utility method to run code on UI thread from worker threads.
 */
@WorkerThread
fun postToUiThread(runnable: VoidMethod) {
    Handler(Looper.getMainLooper()).post {
        runnable.invoke()
    }
}