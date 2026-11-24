package com.linuxandroid.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/**
 * Image helper utilities
 */
object ImageHelper {

    /**
     * Create a placeholder image with text
     * Used when an image is not available
     */
    fun createPlaceholderImage(
        context: Context,
        text: String,
        width: Int = 300,
        height: Int = 150,
        boxColor: Int = Color.parseColor("#1e1e1e"),
        textColor: Int = Color.parseColor("#00e676"),
        imageSize: Int = 120
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background box
        val paint = Paint().apply {
            color = boxColor
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Draw image placeholder
        val imageBitmap = createImagePlaceholderBox(imageSize, imageSize, Color.parseColor("#333333"))
        val imageX = (width - imageSize) / 2
        val imageY = (height - imageSize) / 2
        canvas.drawBitmap(imageBitmap, imageX.toFloat(), imageY.toFloat(), paint)

        // Draw text (if provided)
        if (text.isNotEmpty()) {
            val textPaint = Paint().apply {
                color = textColor
                isAntiAlias = true
                textSize = 14f
                textAlign = Paint.Align.CENTER
            }

            val textX = (width / 2).toFloat()
            val textY = (height - 16).toFloat()

            canvas.drawText(
                text.substring(0, minOf(text.length, 20)),
                textX,
                textY,
                textPaint
            )
        }

        return bitmap
    }

    /**
     * Create a simple image placeholder box
     */
    private fun createImagePlaceholderBox(
        width: Int,
        height: Int,
        color: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val rectPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
        }

        val padding = 2f
        canvas.drawRoundRect(
            padding,
            padding,
            width.toFloat() - padding,
            height.toFloat() - padding,
            8f,
            8f,
            rectPaint
        )

        return bitmap
    }

    /**
     * Apply a tint color to an image
     */
    fun applyTintColor(bitmap: Bitmap, color: Int): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            this.color = color
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }

        canvas.drawRect(0f, 0f, mutableBitmap.width.toFloat(), mutableBitmap.height.toFloat(), paint)
        return mutableBitmap
    }

    /**
     * Get the head of a string
     */
    fun getHead(text: String, maxLength: Int = 50): String {
        if (text.length <= maxLength) {
            return text
        }
        return text.substring(0, maxLength) + "..."
    }

    /**
     * Get the tail of a string
     */
    fun getTail(text: String, maxLength: Int = 50): String {
        if (text.length <= maxLength) {
            return text
        }
        return "..." + text.substring(text.length - maxLength)
    }

    /**
     * Assess suitability
     */
    fun assessSuitability(imagePath: String): Boolean {
        return when {
            imagePath.contains("placeholder") -> false
            imagePath.contains("missing") -> false
            imagePath.endsWith(".svg") -> false
            else -> true
        }
    }

    /**
     * Create a rectangular icon
     */
    fun createIcon(context: Context, size: Int = 64): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            color = Color.parseColor("#00e676")
            isAntiAlias = true
        }

        canvas.drawRoundRect(2f, 2f, (size - 2).toFloat(), (size - 2).toFloat(), 12f, 12f, paint)

        paint.color = Color.parseColor("#1e1e1e")
        canvas.drawRoundRect(6f, 6f, (size - 6).toFloat(), (size - 6).toFloat(), 8f, 8f, paint)

        return bitmap
    }
}
