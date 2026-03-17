package de.muenchen.appcenter.nimux.util.recognition

import android.graphics.Bitmap
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector
import de.muenchen.appcenter.nimux.util.recognition.utils.FaceValidations
import javax.inject.Inject

@OptIn(ExperimentalGetImage::class)
class FaceProcessingAnalyzer @Inject constructor(
    private val detector: FaceDetector
) : ImageAnalysis.Analyzer {

    var onFaceCropped: ((Bitmap) -> Unit)? = null
    var onFacesUpdated: ((List<Face>, Int, Int) -> Unit)? = null

    private var isProcessing = false

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(image)
            .addOnSuccessListener { faces ->

                val validFaces = faces.filter {
                    FaceValidations.isValidFace(
                        it,
                        imageProxy.width,
                        imageProxy.height
                    )
                }

                onFacesUpdated?.invoke(
                    validFaces,
                    imageProxy.width,
                    imageProxy.height
                )

                if (validFaces.isNotEmpty() && !isProcessing) {

                    val biggestFace =
                        validFaces.maxByOrNull { it.boundingBox.width() }

                    biggestFace?.let { face ->

                        val bitmap = imageProxy.toBitmap()
                        val faceBitmap =
                            FaceValidations.cropFace(bitmap, face.boundingBox)
                                ?: return@let

                        isProcessing = true
                        onFaceCropped?.invoke(faceBitmap)
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun resetProcessing() {
        isProcessing = false
    }
}