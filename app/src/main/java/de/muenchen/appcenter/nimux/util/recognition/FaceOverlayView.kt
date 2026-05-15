package de.muenchen.appcenter.nimux.util.recognition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face
import timber.log.Timber

class FaceOverlayView(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {

    private var faces: List<Face> = emptyList()
    private var imageWidth = 0
    private var imageHeight = 0
    private var rotationDegrees = 0
    private var isFrontCamera = false
    private var glitterAlpha: Float = 1f
    private var showGlitter: Boolean = false
    public var showFaceBounds = true

    private val paint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    fun setFaces(
        faces: List<Face>,
        imgWidth: Int,
        imgHeight: Int,
        rotation: Int,
        isFront: Boolean
    ) {
        this.faces = faces
        this.imageWidth = imgWidth
        this.imageHeight = imgHeight
        this.rotationDegrees = rotation
        this.isFrontCamera = isFront
        invalidate()
    }

    private val glowPaint = Paint().apply {
        color = Color.parseColor("#FFD700") // Gold / Glitzer Farbe
        style = Paint.Style.STROKE
        strokeWidth = 12f
        isAntiAlias = true
        setShadowLayer(30f, 0f, 0f, Color.parseColor("#FFD700"))
    }

    fun setGlitterAlpha(alpha: Float) {
        this.glitterAlpha = alpha
        this.showGlitter = true
        invalidate()
    }

    fun stopGlitter() {
        this.showGlitter = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showGlitter && faces.isNotEmpty()) {

            glowPaint.alpha = (glitterAlpha * 255).toInt()


            glowPaint.alpha = (glitterAlpha * 255).toInt()

            val inset = glowPaint.strokeWidth / 2f
            val rect = RectF(
                inset,
                inset,
                width.toFloat() - inset,
                height.toFloat() - inset
            )

            canvas.drawRect(rect, glowPaint)

        }

        if (imageWidth == 0 || imageHeight == 0) return

        if (showFaceBounds) {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        Timber.d("rotation:" + rotationDegrees.toString())
        val rotatedWidth =
            if (rotationDegrees == 90 || rotationDegrees == 270)
                imageHeight else imageWidth

        val rotatedHeight =
            if (rotationDegrees == 90 || rotationDegrees == 270)
                imageWidth else imageHeight

        val scale = maxOf(
            viewWidth / rotatedWidth.toFloat(),
            viewHeight / rotatedHeight.toFloat()
        )

        val scaledWidth = rotatedWidth * scale
        val scaledHeight = rotatedHeight * scale

        val dx = (viewWidth - scaledWidth) / 2f
        val dy = (viewHeight - scaledHeight) / 2f

        val matrix = Matrix()

        matrix.postScale(scale, scale)
        matrix.postTranslate(dx, dy)

        // Spiegelung
        if (isFrontCamera) {
            matrix.postScale(-1f, 1f, viewWidth / 2f, viewHeight / 2f)
        }

        for (face in faces) {
            val rect = RectF(face.boundingBox)
            matrix.mapRect(rect)
            canvas.drawRect(rect, paint)
        }}
    }
}