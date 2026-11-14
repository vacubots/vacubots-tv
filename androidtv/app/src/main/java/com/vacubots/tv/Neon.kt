package com.vacubots.tv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View

/**
 * A round touch button in the app's neon style: a dark centre, a white arrow or cross, and the moving light round its edge
 * (the same light as the Play and Settings buttons). Only touch screens get these; a TV's remote has its own Back key.
 */
class NeonButton(ctx: Context, private val close: Boolean, colors: IntArray, size: Int, private val play: Boolean = false) : View(ctx) {
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = Color.WHITE }
    private val path = Path()

    init {
        val ring = maxOf(3f, size * 0.07f)
        val centre = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFF11141A.toInt()) }
        val inset = ring.toInt()
        background = LayerDrawable(arrayOf(ChaseDrawable(colors, size / 2f, ring, 3600L), centre)).also { it.setLayerInset(1, inset, inset, inset, inset) }
        isClickable = true
        isFocusable = false
        contentDescription = if (play) "Play" else if (close) "Close" else "Back"
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val side = minOf(width, height).toFloat()
        val cx = width / 2f; val cy = height / 2f; val s = side * 0.17f
        icon.strokeWidth = side * 0.085f
        if (play) {
            val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.WHITE }
            path.reset(); path.moveTo(cx - s * 0.75f, cy - s * 1.25f); path.lineTo(cx + s * 1.25f, cy); path.lineTo(cx - s * 0.75f, cy + s * 1.25f); path.close()
            c.drawPath(path, fillP)
        } else if (close) {
            c.drawLine(cx - s, cy - s, cx + s, cy + s, icon)
            c.drawLine(cx + s, cy - s, cx - s, cy + s, icon)
        } else {
            path.reset(); path.moveTo(cx + s * 0.55f, cy - s * 1.3f); path.lineTo(cx - s * 0.7f, cy); path.lineTo(cx + s * 0.55f, cy + s * 1.3f)
            c.drawPath(path, icon)
        }
    }
}
