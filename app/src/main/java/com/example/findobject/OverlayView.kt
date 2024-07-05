package com.example.findobject

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: ObjectDetectorResult? = null
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var scaleFactor: Float = 1f
    private var bounds = Rect()
    private var outputWidth = 0
    private var outputHeight = 0
    private var outputRotate = 0
    private var runningMode: RunningMode = RunningMode.IMAGE

    // Define the real height of the object and the camera parameters
    private val realObjectHeight = 20.0 // in cm, replace with actual height of the object you're measuring
    private val focalLength = 4.0 // in mm, replace with actual focal length of your camera
    private val sensorHeight = 4.55 // in mm, replace with actual sensor height of your camera

    init {
        initPaints()
    }

    fun clear() {
        results = null
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    fun setRunningMode(runningMode: RunningMode) {
        this.runningMode = runningMode
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.mp_primary)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.detections()?.map {
            val boxRect = RectF(
                it.boundingBox().left,
                it.boundingBox().top,
                it.boundingBox().right,
                it.boundingBox().bottom
            )
            val matrix = Matrix()
            matrix.postTranslate(-outputWidth / 2f, -outputHeight / 2f)

            // Rotate box.
            matrix.postRotate(outputRotate.toFloat())

            // If the outputRotate is 90 or 270 degrees, the translation is
            // applied after the rotation. This is because a 90 or 270 degree rotation
            // flips the image vertically or horizontally, respectively.
            if (outputRotate == 90 || outputRotate == 270) {
                matrix.postTranslate(outputHeight / 2f, outputWidth / 2f)
            } else {
                matrix.postTranslate(outputWidth / 2f, outputHeight / 2f)
            }
            matrix.mapRect(boxRect)
            boxRect
        }?.forEachIndexed { index, floats ->

            val top = floats.top * scaleFactor
            val bottom = floats.bottom * scaleFactor
            val left = floats.left * scaleFactor
            val right = floats.right * scaleFactor

            // Draw bounding box around detected objects
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // Create text to display alongside detected objects
            val category = results?.detections()!![index].categories()[0]
            val drawableText =
                category.categoryName() + " " + String.format(
                    "%.2f",
                    category.score()
                )

            // Draw rect behind display text
            textBackgroundPaint.getTextBounds(
                drawableText,
                0,
                drawableText.length,
                bounds
            )
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Draw text for detected object
            canvas.drawText(
                drawableText,
                left,
                top + bounds.height(),
                textPaint
            )

            // Determine approximate position
            val approximatePosition = determineApproximatePosition(
                floats,
                width,
                height
            )

            // Display the approximate position
            val positionText = "Pos: $approximatePosition"
            canvas.drawText(
                positionText,
                left,
                top + bounds.height() + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textPaint
            )

            // Calculate distance from camera to object center
            val centerX = (left + right) / 2
            val centerY = (top + bottom) / 2
            val objectPixelHeight = bottom - top
            val distanceFromCamera = calculateDistanceFromCamera(
                realObjectHeight,
                focalLength,
                sensorHeight,
                height.toDouble(), // image height in pixels
                objectPixelHeight.toDouble()
            )

            // Display the distance from camera
            val distanceText = "Dist from Camera: %.2f cm".format(distanceFromCamera)
            canvas.drawText(
                distanceText,
                left,
                top + bounds.height() + textHeight * 2 + BOUNDING_RECT_TEXT_PADDING * 2,
                textPaint
            )

            // Calculate distance between detected objects (if there are more than one)
            if (index > 0) {
                val previousCenter = PointF(
                    (results!!.detections()[index - 1].boundingBox().left +
                            results!!.detections()[index - 1].boundingBox().right) / 2 * scaleFactor,
                    (results!!.detections()[index - 1].boundingBox().top +
                            results!!.detections()[index - 1].boundingBox().bottom) / 2 * scaleFactor
                )
                val currentCenter = PointF(
                    (left + right) / 2,
                    (top + bottom) / 2
                )

                val distance = calculateDistance(previousCenter, currentCenter)

                // Display the distance between objects
                val objectDistanceText = "Dist to Prev: %.2f".format(distance)
                canvas.drawText(
                    objectDistanceText,
                    left,
                    top + bounds.height() + textHeight * 3 + BOUNDING_RECT_TEXT_PADDING * 3,
                    textPaint
                )
            }
        }
    }

    fun setResults(
        detectionResults: ObjectDetectorResult,
        outputHeight: Int,
        outputWidth: Int,
        imageRotation: Int
    ) {
        results = detectionResults
        this.outputWidth = outputWidth
        this.outputHeight = outputHeight
        this.outputRotate = imageRotation

        // Calculates the new width and height of an image after it has been rotated.
        // If `imageRotation` is 0 or 180, the new width and height are the same
        // as the original width and height.
        // If `imageRotation` is 90 or 270, the new width and height are swapped.
        val rotatedWidthHeight = when (imageRotation) {
            0, 180 -> Pair(outputWidth, outputHeight)
            90, 270 -> Pair(outputHeight, outputWidth)
            else -> return
        }

        // Images, videos are displayed in FIT_START mode.
        // Camera live streams is displayed in FILL_START mode. So we need to scale
        // up the bounding box to match with the size that the images/videos/live streams being
        // displayed.
        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(
                    width * 1f / rotatedWidthHeight.first,
                    height * 1f / rotatedWidthHeight.second
                )
            }

            RunningMode.LIVE_STREAM -> {
                max(
                    width * 1f / rotatedWidthHeight.first,
                    height * 1f / rotatedWidthHeight.second
                )
            }
        }

        invalidate()
    }

    private fun determineApproximatePosition(boundingBox: RectF, imageWidth: Int, imageHeight: Int): String {
        // Calculate bounding box center point
        val centerX = boundingBox.left + boundingBox.width() / 2
        val centerY = boundingBox.top + boundingBox.height() / 2

        // Tolerance for "In-Front" based on bounding box size and image size
        val toleranceX = boundingBox.width() * 0.1f
        val toleranceY = boundingBox.height() * 0.1f

        // Check if center point is close enough to image center (considering tolerances)
        if (centerX < imageWidth / 2 + toleranceX && centerY < imageHeight / 2 + toleranceY) {
            return "In-Front"
        }

        val isTopHalf = boundingBox.top < imageHeight / 2
        val isLeftHalf = boundingBox.left < imageWidth / 2

        // Determine approximate position based on quadrant and half
        return if (isTopHalf) {
            if (isLeftHalf) {
                "Front-Left"
            } else {
                "Front-Right"
            }
        } else {
            if (isLeftHalf) {
                "Behind-Left"
            } else {
                "Behind-Right"
            }
        }
    }

    private fun calculateDistance(point1: PointF, point2: PointF): Double {
        val dx = point2.x - point1.x
        val dy = point2.y - point1.y
        return sqrt((dx * dx + dy * dy).toDouble())
    }

    private fun calculateDistanceFromCamera(realHeight: Double, focalLength: Double, sensorHeight: Double, imageHeight: Double, pixelHeight: Double): Double {
        // Convert focal length from mm to cm
        val focalLengthCm = focalLength / 10.0
        return (realHeight * focalLengthCm * imageHeight) / (sensorHeight * pixelHeight)
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
