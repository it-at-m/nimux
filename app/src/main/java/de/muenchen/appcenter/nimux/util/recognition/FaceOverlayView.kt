package de.muenchen.appcenter.nimux.util.recognition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face

class FaceOverlayView(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {

    private var faces: List<Face> = emptyList()
    private var imageWidth = 0
    private var imageHeight = 0

    private val paint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    fun setFaces(
        faces: List<Face>,
        imgWidth: Int,
        imgHeight: Int
    ) {
        this.faces = faces
        this.imageWidth = imgWidth
        this.imageHeight = imgHeight
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (imageWidth == 0 || imageHeight == 0) return

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        for (face in faces) {
            val rect = face.boundingBox

            val left = rect.left * scaleX
            val top = rect.top * scaleY
            val right = rect.right * scaleX
            val bottom = rect.bottom * scaleY

            canvas.drawRect(left, top, right, bottom, paint)
        }
    }
}