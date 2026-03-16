package de.muenchen.appcenter.nimux.util.recognition.utils

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark

object FaceValidations {

    fun cropFace(source: Bitmap, box: Rect): Bitmap? {
        val left = box.left.coerceIn(0, source.width - 1)
        val top = box.top.coerceIn(0, source.height - 1)
        val right = box.right.coerceIn(left + 1, source.width)
        val bottom = box.bottom.coerceIn(top + 1, source.height)

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) {
            Log.w("AddUsersFace", "Ungültige BoundingBox: $box")
            return null
        }

        val cropped = Bitmap.createBitmap(source, left, top, width, height)
        return Bitmap.createScaledBitmap(cropped, 112, 112, true)
    }

    fun isValidFace(
        face: Face,
        imageWidth: Int,
        imageHeight: Int
    ): Boolean {

        val box = face.boundingBox

        val width = box.width().toFloat()
        val height = box.height().toFloat()
        val aspectRatio = width / height

        if (aspectRatio < 0.7f || aspectRatio > 1.3f) return false

        if (width < imageWidth * 0.15f) return false

        if (box.left < 0 || box.top < 0 ||
            box.right > imageWidth || box.bottom > imageHeight
        ) return false

        if (face.getLandmark(FaceLandmark.LEFT_EYE) == null) return false
        if (face.getLandmark(FaceLandmark.RIGHT_EYE) == null) return false

        return true
    }
}