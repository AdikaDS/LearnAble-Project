package com.adika.learnable.util

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan

class CenteredImageSpan(drawable: Drawable) : ImageSpan(drawable) {

    @SuppressLint("UseKtx")
    override fun draw(
        canvas: Canvas, text: CharSequence, start: Int, end: Int,
        x: Float, top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        val b = drawable

        val transY = (top + bottom - b.bounds.height()) / 2f
        canvas.save()
        canvas.translate(x, transY)
        b.draw(canvas)
        canvas.restore()
    }

    override fun getSize(
        paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?
    ): Int {
        val rect = drawable.bounds
        if (fm != null) {
            val pfm = paint.fontMetricsInt
            val drH = rect.height()
            val fontH = pfm.descent - pfm.ascent
            val adjust = (drH - fontH) / 2
            fm.ascent = pfm.ascent - adjust
            fm.descent = pfm.descent + adjust
            fm.top = fm.ascent
            fm.bottom = fm.descent
        }
        return rect.right
    }
}