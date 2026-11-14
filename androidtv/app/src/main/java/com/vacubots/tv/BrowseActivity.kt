package com.vacubots.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.graphics.Canvas
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Matrix
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable.Orientation
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.speech.RecognizerIntent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.inputmethod.EditorInfo
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.graphics.Typeface
import com.bumptech.glide.Glide
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Pure white is harsh on a bright OLED, so "white" in this app is a soft off-white. */
val SOFT_WHITE: Int get() = if (Contrast.on) Contrast.fg else 0xFFE4E7EB.toInt()

/** The two high contrast themes (for low vision): plain black or plain white, steady thick outlines, nothing that moves. Mode 0 is the normal app. */
object Contrast {
    var mode = 0   // 0 normal, 1 black, 2 white
    val on get() = mode != 0
    val fg get() = if (mode == 2) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    val bg get() = if (mode == 2) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
    val sec get() = if (mode == 2) 0xFF333333.toInt() else 0xFFE6E6E6.toInt()
    val focus get() = if (mode == 2) 0xFF0033CC.toInt() else 0xFFFFD60A.toInt()
    /** Text on the focus colour: black on yellow, white on blue. */
    val onFocus get() = if (mode == 2) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
    /** Maps a colour the normal app uses for text to its high contrast counterpart: bright text is the main colour, mid text the secondary colour, dark text (on a light chip) the background colour. */
    fun ink(c: Int): Int {
        if (!on) return c
        if (c == fg) return fg
        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
        return when { r >= 0xE0 && g >= 0xE0 && b >= 0xE0 -> fg; r + g + b > 330 -> sec; else -> bg }
    }
}

/** One video in the site's list. */
data class Clip(val id: String, val title: String, val channel: String, val maker: Boolean, val feed: String, val stamp: Long, val short: Boolean = false, val chan: String = "")

/** The home screen's color bar: the same rainbow as the chasing outline on the Settings panel, sliding sideways in a loop. It reads the clock, so it never drifts out of step with the outlines. */
class MovingBars(c: android.content.Context, private val colors: IntArray, private val ms: Long = 9000L) : EffectView(c) {
    private val paint = Paint()
    private val m = Matrix()
    private var shader: LinearGradient? = null
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        // one full pattern across the screen; the first and last colors match, so repeating it leaves no seam
        shader = LinearGradient(0f, 0f, w.toFloat(), 0f, colors, null, Shader.TileMode.REPEAT)
        paint.shader = shader
    }
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        if (w <= 0f) return
        m.setTranslate((android.os.SystemClock.uptimeMillis() % ms) / ms.toFloat() * w, 0f); shader?.setLocalMatrix(m)
        canvas.drawRect(0f, 0f, w, height.toFloat(), paint)
    }
}

/** Autumn leaves falling behind the whole interface like an animated wallpaper, for the season (October 15 to November 30). They read the clock and never take focus or clicks. */
class FallLeaves(c: android.content.Context, private val n: Int = 20) : EffectView(c) {
    private val colors = intArrayOf(0xFFE8731A.toInt(), 0xFFC8402A.toInt(), 0xFFE9A21B.toInt(), 0xFFB5651D.toInt(), 0xFFD9531E.toInt())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val vein = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0x66000000; strokeWidth = 1.4f }
    private val leaf = android.graphics.Path().apply {   // a pointed oval, drawn in a unit box and scaled per leaf
        moveTo(0f, -1f); cubicTo(0.95f, -0.45f, 0.8f, 0.55f, 0f, 1f); cubicTo(-0.8f, 0.55f, -0.95f, -0.45f, 0f, -1f); close()
    }
    private val seed = IntArray(n) { (it * 7919 + 13) }
    private fun r(i: Int, k: Int): Float = (((seed[i] * (k + 3) * 2654435761L) ushr 8) and 0xFFFF).toFloat() / 65535f
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f) return
        val t = android.os.SystemClock.uptimeMillis() / 1000f
        for (i in 0 until n) {
            val size = w / 1920f * (16f + 18f * r(i, 1))
            val fall = 42f + 40f * r(i, 2)                         // seconds from the top to the bottom edge
            val phase = r(i, 3) * fall
            val prog = ((t + phase) % fall) / fall
            val y = -size * 2 + prog * (h + size * 4)
            val x = r(i, 4) * w + Math.sin((t * (0.35 + 0.3 * r(i, 5)) + r(i, 6) * 6.28).toDouble()).toFloat() * w * 0.03f
            val spin = t * (25f + 40f * r(i, 7)) * (if (r(i, 8) > 0.5f) 1f else -1f) + r(i, 9) * 360f
            val fade = minOf(1f, minOf(prog, 1f - prog) * 6f)      // fade in at the top, out at the bottom
            paint.color = colors[(r(i, 10) * colors.size).toInt().coerceAtMost(colors.size - 1)]
            paint.alpha = (215 * fade).toInt()
            vein.alpha = (110 * fade).toInt()
            canvas.save(); canvas.translate(x, y); canvas.rotate(spin); canvas.scale(size * 0.62f, size)
            canvas.drawPath(leaf, paint)
            vein.strokeWidth = 1.6f * (w / 1920f) / size   // about 1.6 px on a 1080p screen, 3.2 px on a 4K one
            canvas.drawLine(0f, -0.9f, 0f, 0.9f, vein)
            canvas.restore()
        }
    }
}

/** The animated outline colours: taken from the wallpaper that is showing, so the moving light always matches the scene. */
object Glow {
    /** The plain pale blue and white of "No Theme". */
    val plain = intArrayOf(0xFFF2F2F2.toInt(), 0xFF9DB8E8.toInt(), 0xFFF2F2F2.toInt(), 0xFF9DB8E8.toInt(), 0xFFF2F2F2.toInt())
    var current = plain
    val green = intArrayOf(0xFF7CFF3A.toInt(), 0xFF2FD14A.toInt(), 0xFFC6FF8A.toInt(), 0xFF2FD14A.toInt(), 0xFF7CFF3A.toInt())
    private val cache = HashMap<Int, IntArray>()
    fun forImage(ctx: android.content.Context, res: Int): IntArray = cache.getOrPut(res) {
        try {
            val o = BitmapFactory.Options().apply { inSampleSize = 16 }
            val b = BitmapFactory.decodeResource(ctx.resources, res, o) ?: return@getOrPut current
            val score = FloatArray(12); val hsv = FloatArray(3)
            for (x in 0 until b.width step 2) for (y in 0 until b.height step 2) {
                Color.colorToHSV(b.getPixel(x, y), hsv)
                if (hsv[1] > 0.25f && hsv[2] > 0.3f) score[(hsv[0] / 30f).toInt() % 12] += hsv[1] * hsv[2]
            }
            val best = score.indices.maxByOrNull { score[it] } ?: 0
            if (score[best] <= 0f) return@getOrPut current
            val hue = best * 30f + 15f
            fun c(dh: Float, s: Float, v: Float) = Color.HSVToColor(floatArrayOf((hue + dh + 360f) % 360f, s, v))
            intArrayOf(c(0f, 0.55f, 1f), c(25f, 0.45f, 1f), c(0f, 0.12f, 1f), c(-25f, 0.5f, 1f), c(0f, 0.55f, 1f))
        } catch (e: Throwable) { current }
    }
}

/** A line of light that chases around a rounded rectangle, like the LEDs on the site's TV tile. A drawable, so it can never change a button's size. */
class ChaseDrawable(private val colors: IntArray, private val radius: Float, private val stroke: Float, private val ms: Long = 3600L, private val focusRing: Boolean = false) : android.graphics.drawable.Drawable(), Tickable {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke }
    private val rect = RectF()
    private val m = Matrix()
    private var shader: SweepGradient? = null
    override fun onBoundsChange(b: android.graphics.Rect) {
        rect.set(b.left + stroke / 2, b.top + stroke / 2, b.right - stroke / 2, b.bottom - stroke / 2)
        shader = SweepGradient(b.exactCenterX(), b.exactCenterY(), colors, null)
        paint.shader = shader
    }
    override fun draw(canvas: Canvas) {
        if (Contrast.on) {   // high contrast: a steady line, the focus colour when it marks the selected item, nothing moving
            val sw = if (focusRing) stroke * 1.7f else stroke
            paint.shader = null; paint.color = if (focusRing) Contrast.focus else Contrast.fg; paint.strokeWidth = sw
            canvas.drawRoundRect(RectF(bounds.left + sw / 2, bounds.top + sw / 2, bounds.right - sw / 2, bounds.bottom - sw / 2), radius, radius, paint)
            return
        }
        paint.shader = shader; paint.strokeWidth = stroke
        val angle = (android.os.SystemClock.uptimeMillis() % ms) / ms.toFloat() * 360f
        m.setRotate(angle, bounds.exactCenterX(), bounds.exactCenterY()); shader?.setLocalMatrix(m)
        canvas.drawRoundRect(rect, radius, radius, paint)
    }
    // Redraws on the shared effects clock (30 a second, with every other moving effect) while its view is showing, instead of asking for every frame itself.
    override fun tick() { invalidateSelf() }
    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val r = super.setVisible(visible, restart)
        if (visible && !Contrast.on) Effects.add(this) else Effects.remove(this)
        return r
    }
    override fun setAlpha(a: Int) {}
    override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION") override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
}

/** The line under the selected tab: the wallpaper's own colours sliding along it, so it matches the theme and still moves. */
class GlowBar(private val colors: IntArray, private val h: Int, private val ms: Long = 2600L, private val inset: Int = 0) : android.graphics.drawable.Drawable(), Tickable {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val m = Matrix()
    private var shader: LinearGradient? = null
    override fun onBoundsChange(b: android.graphics.Rect) {
        val bw = if (inset > 0) (b.width() - 2 * inset) * 0.5f else b.width().toFloat()
        shader = LinearGradient(0f, 0f, maxOf(1f, bw * 0.8f), 0f, colors, null, Shader.TileMode.MIRROR)
        paint.shader = shader
    }
    override fun draw(canvas: Canvas) {
        val b = bounds; if (h <= 0 || b.width() <= 0) return
        if (Contrast.on) {   // a plain solid line
            paint.shader = null; paint.color = Contrast.fg
            val bw = (b.width() - 2 * inset).toFloat(); val l = b.exactCenterX() - bw / 2f   // the whole width of the word
            canvas.drawRoundRect(l, (b.bottom - h).toFloat(), l + bw, b.bottom.toFloat(), 2f, 2f, paint)
            return
        }
        paint.shader = shader
        // a short bar (half the word's width) centred under the word; the colours slide along it
        val bw = if (inset > 0) (b.width() - 2 * inset) * 0.5f else b.width().toFloat(); val l = b.exactCenterX() - bw / 2f
        val span = maxOf(1f, bw * 0.8f) * 2f
        m.setTranslate((android.os.SystemClock.uptimeMillis() % ms) / ms.toFloat() * span + l, 0f); shader?.setLocalMatrix(m)
        canvas.drawRoundRect(l, (b.bottom - h).toFloat(), l + bw, b.bottom.toFloat(), h / 2f, h / 2f, paint)
    }
    override fun tick() { invalidateSelf() }
    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val r = super.setVisible(visible, restart)
        if (visible && !Contrast.on) Effects.add(this) else Effects.remove(this)
        return r
    }
    override fun setAlpha(a: Int) {}
    override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION") override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
}

/** A plain solid-color icon (a magnifying glass or a gear), drawn in code so it stays crisp at any TV size. */
class IconView(ctx: android.content.Context, private val kind: String, private val color: Int, private val strokeRatio: Float = 0.11f) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = if (Contrast.on) Contrast.fg else this@IconView.color }
    override fun onDraw(canvas: Canvas) {
        val side = minOf(width, height) * 0.5f
        val cx = width / 2f; val cy = height / 2f
        paint.strokeWidth = side * strokeRatio * 2f
        if (kind == "search") {
            // the same glass as the site's top bar (24-unit box: circle at 10,10 radius 7, handle from 15,15 to 21,21, 2-unit round stroke)
            val k = side * 1.25f / 24f
            val ox = cx - 12f * k; val oy = cy - 12f * k
            paint.strokeWidth = 2f * k * 1.15f; paint.strokeJoin = Paint.Join.ROUND
            canvas.drawCircle(ox + 10f * k, oy + 10f * k, 7f * k, paint)
            canvas.drawLine(ox + 15f * k, oy + 15f * k, ox + 21f * k, oy + 21f * k, paint)
        } else if (kind == "bookmark") {
            val k = side * 1.45f / 24f
            val ox = cx - 12f * k; val oy = cy - 12f * k
            paint.strokeWidth = 2f * k * 1.1f; paint.strokeJoin = Paint.Join.ROUND
            val pth = android.graphics.Path()
            pth.moveTo(ox + 7f * k, oy + 4f * k); pth.lineTo(ox + 17f * k, oy + 4f * k); pth.lineTo(ox + 17f * k, oy + 20f * k); pth.lineTo(ox + 12f * k, oy + 16f * k); pth.lineTo(ox + 7f * k, oy + 20f * k); pth.close()
            canvas.drawPath(pth, paint)
        } else if (kind == "mic") {
            val k = side * 1.7f / 24f
            val ox = cx - 12f * k; val oy = cy - 12f * k
            paint.strokeWidth = 2f * k * 0.95f; paint.strokeJoin = Paint.Join.ROUND
            canvas.drawRoundRect(RectF(ox + 9f * k, oy + 2f * k, ox + 15f * k, oy + 14f * k), 3f * k, 3f * k, paint)
            canvas.drawArc(RectF(ox + 5f * k, oy + 7f * k, ox + 19f * k, oy + 19f * k), 0f, 180f, false, paint)
            canvas.drawLine(ox + 12f * k, oy + 19f * k, ox + 12f * k, oy + 22f * k, paint)
            canvas.drawLine(ox + 9f * k, oy + 22f * k, ox + 15f * k, oy + 22f * k, paint)
        } else if (kind == "arrow") {
            // the site's arrow (24-unit box, 2-unit round stroke) inside a ring
            val k = side * 1.7f / 24f
            val ox = cx - 12f * k; val oy = cy - 12f * k
            paint.strokeWidth = 2f * k * 0.9f; paint.strokeJoin = Paint.Join.ROUND
            canvas.drawCircle(cx, cy, 11f * k, paint)
            canvas.drawLine(ox + 7f * k, oy + 12f * k, ox + 17f * k, oy + 12f * k, paint)
            canvas.drawLine(ox + 13f * k, oy + 8f * k, ox + 17f * k, oy + 12f * k, paint)
            canvas.drawLine(ox + 13f * k, oy + 16f * k, ox + 17f * k, oy + 12f * k, paint)
        } else {
            val ring = side * 0.30f
            paint.strokeWidth = side * 0.17f
            canvas.drawCircle(cx, cy, ring, paint)
            paint.strokeWidth = side * 0.19f; paint.strokeCap = Paint.Cap.BUTT
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val c = Math.cos(a).toFloat(); val sn = Math.sin(a).toFloat()
                canvas.drawLine(cx + c * side * 0.40f, cy + sn * side * 0.40f, cx + c * side * 0.55f, cy + sn * side * 0.55f, paint)
            }
            paint.strokeCap = Paint.Cap.ROUND
        }
    }
}

/** The saved-video badge: the Product Finder's bookmark, filled, inside a yellow circle. */
class BookmarkBadge(ctx: android.content.Context) : View(ctx) {
    private val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xFFF2C230.toInt() }
    private val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL_AND_STROKE; color = 0xFF3A2A00.toInt(); strokeWidth = 1.9f; strokeJoin = Paint.Join.ROUND }
    private val path = androidx.core.graphics.PathParser.createPathFromPathData("M7 4h10a1 1 0 0 1 1 1v15l-6-3.8L6 20V5a1 1 0 0 1 1-1z") ?: android.graphics.Path()
    override fun onDraw(canvas: Canvas) {
        val s = minOf(width, height).toFloat()
        canvas.drawCircle(width / 2f, height / 2f, s / 2f, circle)
        val k = s * 0.62f / 24f
        canvas.save(); canvas.translate((width - 24f * k) / 2f, (height - 24f * k) / 2f); canvas.scale(k, k); canvas.drawPath(path, mark); canvas.restore()
    }
}

/** The site's duotone illustrations (a soft tinted fill plus a 3-unit outline on a 96-unit grid), drawn from the same path data as the web page. */
class IllView(ctx: android.content.Context, private val parts: List<Pair<String, Boolean>>, private val tint: Int,
              private val grid: Float = 96f, strokeUnits: Float = 0f, private val faintFrom: Int = Int.MAX_VALUE) : View(ctx) {
    private val paths = parts.map { androidx.core.graphics.PathParser.createPathFromPathData(it.first) ?: android.graphics.Path() }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = if (Contrast.on) Contrast.fg else tint; alpha = if (Contrast.on) 0 else 40 }
    private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = if (Contrast.on) Contrast.fg else tint; strokeWidth = if (strokeUnits > 0f) (if (Contrast.on) strokeUnits * 1.35f else strokeUnits) else if (Contrast.on) 4.6f else 3.4f }
    override fun onDraw(canvas: Canvas) {
        val k = minOf(width, height) / grid
        canvas.save(); canvas.scale(k, k)
        val full = lineP.alpha
        parts.forEachIndexed { i, part ->
            if (part.second) canvas.drawPath(paths[i], fillP)
            lineP.alpha = if (i >= faintFrom && !Contrast.on) full * 4 / 10 else full   // the mower's ground line is drawn at 40% like on the site
            canvas.drawPath(paths[i], lineP)
        }
        lineP.alpha = full
        canvas.restore()
    }
}

object Ill {
    private const val CIRCLE_BIG = "M14 48a34 34 0 1 0 68 0a34 34 0 1 0-68 0z"
    val NEWS = listOf("M12 20c12-4 26-4 36 4v52c-10-8-24-8-36-4zM84 20c-12-4-26-4-36 4v52c10-8 24-8 36-4z" to true)
    val FINDER = listOf("M16 42a26 26 0 1 0 52 0a26 26 0 1 0-52 0z" to true, "M62 62l20 20" to false)
    val DEALS = listOf("M14 48V16h32l36 36-32 32z" to true, "M27 34a5 5 0 1 0 10 0a5 5 0 1 0-10 0z" to false)
    val PARTS = listOf("M14 48a34 34 0 1 0 68 0a34 34 0 1 0-68 0z" to true, "M64 22a14 14 0 0 0-17 18L22 66a6 6 0 0 0 8 8l26-25a14 14 0 0 0 18-17l-9 9-8-3-3-8z" to false)
    val INDOOR = listOf(CIRCLE_BIG to true, "M34 48a14 14 0 1 0 28 0a14 14 0 1 0-28 0z" to false, "M26 30a34 34 0 0 1 44 0" to false)
    val OUTDOOR = listOf("M16 44h46a8 8 0 0 1 8 8v12H16z" to true, "M20 72a8 8 0 1 0 16 0a8 8 0 1 0-16 0zM54 72a8 8 0 1 0 16 0a8 8 0 1 0-16 0z" to false, "M70 52l12-26h8" to false)
    /** Settings panel icons (24-unit grid, outline only). */
    val S_CONTENT = listOf("M5 4h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z" to false, "M10 8.5l5 3.5-5 3.5z" to false)
    val S_LOOK = listOf("M18 3l3 3-9 9-4 1 1-4z" to false, "M7 17c-1 1-2 3-3 4 3 0 5-1 5-3" to false)
    val S_INFO = listOf("M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0z" to false, "M12 11v6M12 7.5v.01" to false)
    val S_PERSON = listOf("M8.4 8a3.6 3.6 0 1 0 7.2 0a3.6 3.6 0 1 0-7.2 0z" to false, "M5 20c1-4 4-6 7-6s6 2 7 6" to false)
    val S_TAG = listOf("M3 12V4h8l10 10-8 8z" to false, "M6.3 8.5a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0-2.4 0z" to false)
    val S_SORT = listOf("M8 19V5M4 9l4-4 4 4M16 5v14M12 15l4 4 4-4" to false)
    val S_SHORTS = listOf("M9 3h6a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H9a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z" to false, "M10.5 9l4 3-4 3z" to false)
    val S_IMAGE = listOf("M5 5h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z" to false, "M7 10a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0z" to false, "M3 17l5-4 4 3 3-3 6 5" to false)
    val S_SOUND = listOf("M4 9h4l5-4v14l-5-4H4z" to false, "M16 9a4 4 0 0 1 0 6M18.5 6.5a8 8 0 0 1 0 11" to false)
    val S_UPDATE = listOf("M19 12a7 7 0 1 1-2-5M19 4v4h-4" to false)
    val S_DOC = listOf("M6 3h8l4 4v14H6z" to false, "M14 3v4h4M9 12h6M9 16h6" to false)
    val S_CHEV = listOf("M9 4l8 8-8 8" to false)
    /** The Product Finder's own category icons (24-unit grid, drawn outline only), so the feed pages match the site. */
    val INDOOR_F = listOf("M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0-17 0z" to false, "M6.4 8.3a8 8 0 0 1 11.2 0" to false, "M9.9 14a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0-4.2 0z" to false)
    val OUTDOOR_F = listOf("M2.5 17l.8-3.1c.3-1.1 1.2-1.8 2.5-2.1l8.4-2.2c3.8-1 6.5.6 7 4.1l.3 3.3z" to false, "M14.4 9.4a2.6 2.2 0 0 1 5.2 0" to false,
        "M7.7 9.9H10.1a.5.5 0 0 1 .5.5V11a.5.5 0 0 1-.5.5H7.7a.5.5 0 0 1-.5-.5v-.6a.5.5 0 0 1 .5-.5z" to false, "M2.5 17h11" to false,
        "M14 17.2a3.2 3.2 0 1 0 6.4 0a3.2 3.2 0 1 0-6.4 0z" to false, "M16.3 17.2a.9.9 0 1 0 1.8 0a.9.9 0 1 0-1.8 0z" to false, "M4.7 18.9a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0-3.2 0z" to false,
        "M2 21.8h20" to false)   // the last line (index 7) is the faint ground line
    val SMART_F = listOf("M4 11.2l8-6.4 8 6.4" to false, "M6 10v9.5h12V10" to false, "M10 19.5v-4.5h4v4.5" to false)
    val COMBO = listOf("M14 34l34-16 34 16-34 16z" to true, "M14 48l34 16 34-16" to false, "M14 62l34 16 34-16" to false)
    val SMART = listOf("M14 46L48 16l34 30v32H14z" to true, "M40 78V58h16v20" to false, "M36 42a16 16 0 0 1 24 0M30 36a24 24 0 0 1 36 0" to false)
    val SHORTS = listOf("M32 12h32a8 8 0 0 1 8 8v56a8 8 0 0 1-8 8H32a8 8 0 0 1-8-8V20a8 8 0 0 1 8-8z" to true, "M42 36l18 12-18 12z" to false)
    val CREATORS = listOf("M34 34a14 14 0 1 0 28 0a14 14 0 1 0-28 0z" to true, "M20 82c2-18 14-28 28-28s26 10 28 28z" to true)
    val SORT = listOf("M32 74V22M18 36l14-14 14 14M64 22v52M50 60l14 14 14-14" to false)
    val UPDATE = listOf("M76 40A30 30 0 1 0 78 58M78 22v20H58" to false)
    val DOC = listOf("M24 10h34l16 16v60H24z" to true, "M58 10v16h16M36 46h28M36 60h28M36 74h18" to false)
    val MAIL = listOf("M10 24h76v50H10z" to true, "M10 28l38 26 38-26" to false)
    val LOCK = listOf("M22 44h52v38H22z" to true, "M32 44V32a16 16 0 0 1 32 0v12M48 58v10" to false)
    val SHIELD = listOf("M48 10l30 10v24c0 20-12 34-30 42C30 78 18 64 18 44V20z" to true, "M36 46l9 9 16-18" to false)
    val HEART = listOf("M48 82C20 62 12 48 12 34a17 17 0 0 1 36-5 17 17 0 0 1 36 5c0 14-8 28-36 48z" to true)
    val COOKIE = listOf("M48 12a36 36 0 1 0 36 36 14 14 0 0 1-15-14 14 14 0 0 1-14-15 8 8 0 0 1-7-7z" to true, "M32 40a3 3 0 1 0 6 0a3 3 0 1 0-6 0zM40 62a3 3 0 1 0 6 0a3 3 0 1 0-6 0zM60 64a3 3 0 1 0 6 0a3 3 0 1 0-6 0zM28 58a2 2 0 1 0 4 0a2 2 0 1 0-4 0z" to false)
    val ACCESS = listOf("M14 48a34 34 0 1 0 68 0a34 34 0 1 0-68 0z" to true, "M43 28a5 5 0 1 0 10 0a5 5 0 1 0-10 0z" to false, "M30 40l18 4 18-4" to false, "M48 44v16M48 60l-9 16M48 60l9 16" to false)
    val UNAVAILABLE = listOf("M12 28a10 10 0 0 1 10-10h52a10 10 0 0 1 10 10v40a10 10 0 0 1-10 10H22a10 10 0 0 1-10-10z" to true, "M40 34l22 14-22 14z" to false, "M16 84L80 12" to false)
    val YOUTUBE = listOf("M12 28a10 10 0 0 1 10-10h52a10 10 0 0 1 10 10v40a10 10 0 0 1-10 10H22a10 10 0 0 1-10-10z" to true, "M40 34l22 14-22 14z" to false)
}

/**
 * A button that measures and draws itself, so its label can never be cut off: width is the label's own width plus padding.
 * With `colors` it is a filled pill with a light chasing around its edge; without, a plain white outline. `play` adds a drawn triangle.
 */
class PillButton(ctx: android.content.Context, private val label: String, private val fillColor: Int, private val textColor: Int,
                 private val colors: IntArray?, textPx: Float, face: android.graphics.Typeface, private val heightPx: Int,
                 private val padPx: Int, private val corner: Float, private val stroke: Float, private val play: Boolean) : View(ctx), Tickable {
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = textColor; textSize = textPx; typeface = face }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; color = SOFT_WHITE }
    private val chase = colors?.let { ChaseDrawable(it, corner, stroke) }
    private val tri = textPx * 0.62f
    private val gap = textPx * 0.55f
    private val rect = RectF()
    private val path = android.graphics.Path()
    /** Empty room around a glowing button so its glow fades out instead of being cut flat at the view's edge. */
    val glow = if (colors != null) 14f * ctx.resources.displayMetrics.widthPixels / 1920f else 0f
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = glow * 0.9f
        color = (colors?.firstOrNull() ?: 0) and 0x00FFFFFF or 0x88000000.toInt()
        maskFilter = android.graphics.BlurMaskFilter(maxOf(glow * 0.8f, 1f), android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    private var glowRest: android.graphics.Bitmap? = null
    private var glowFocus: android.graphics.Bitmap? = null
    // A button with a chasing light redraws on the shared effects clock while it is on screen.
    override fun onAttachedToWindow() { super.onAttachedToWindow(); if (colors != null) Effects.add(this) }
    override fun onDetachedFromWindow() { Effects.remove(this); super.onDetachedFromWindow() }
    override fun tick() { if (!Contrast.on) invalidate() }
    /** The blurred glow never changes shape, so it is painted once into a bitmap instead of being blurred on every frame. */
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        glowRest = null; glowFocus = null
        if (colors == null || w <= 0 || h <= 0) return
        fun make(focused: Boolean): android.graphics.Bitmap {
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            glowPaint.alpha = if (focused) 150 else 50; glowPaint.strokeWidth = glow * (if (focused) 1.0f else 0.6f)
            Canvas(bmp).drawRoundRect(RectF(glow, glow, w - glow, h - glow), corner, corner, glowPaint)
            return bmp
        }
        glowRest = make(false); glowFocus = make(true)
    }
    private fun contentWidth() = textPaint.measureText(label) + if (play) tri + gap else 0f
    /** High contrast: a filled button is the main colour, an outline button is plain; focus fills an outline button with the focus colour. */
    private fun hcFill(): Int = if (chase == null) (if (isFocused) Contrast.focus else Contrast.bg)
        else if (Color.alpha(fillColor) >= 0x80 && Color.red(fillColor) + Color.green(fillColor) + Color.blue(fillColor) > 450) Contrast.fg else Contrast.bg
    private fun hcText(): Int = if (chase == null) (if (isFocused) Contrast.onFocus else Contrast.fg) else (if (hcFill() == Contrast.fg) Contrast.bg else Contrast.fg)

    override fun onMeasure(w: Int, h: Int) {
        // a button given an exact width (the phone's full-width buttons) takes it
        if (MeasureSpec.getMode(w) == MeasureSpec.EXACTLY) { setMeasuredDimension(MeasureSpec.getSize(w), (heightPx + glow * 2).toInt()); return }
        // never wider than 38% of the screen: the text shrinks to fit instead of pushing the button off the edge
        val cap = (resources.displayMetrics.widthPixels * 0.38f)
        while (contentWidth() + padPx * 2 > cap && textPaint.textSize > 12f) textPaint.textSize -= 1f
        setMeasuredDimension((contentWidth() + padPx * 2 + glow * 2).toInt() + 2, (heightPx + glow * 2).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        if (!Contrast.on) (if (isFocused) glowFocus else glowRest)?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        canvas.save(); canvas.translate(glow, glow)
        val w = width.toFloat() - glow * 2; val hh = height.toFloat() - glow * 2
        if (chase != null) {
                        chase.setBounds(0, 0, w.toInt(), hh.toInt()); chase.draw(canvas)
            rect.set(stroke * 0.9f, stroke * 0.9f, w - stroke * 0.9f, hh - stroke * 0.9f)
            fillPaint.color = if (Contrast.on) hcFill() else if (Color.alpha(fillColor) == 0 && isFocused && colors != null) (colors[0] and 0x00FFFFFF) or 0x30000000 else fillColor   // an outline button gets a faint tint when selected
            canvas.drawRoundRect(rect, corner * 0.8f, corner * 0.8f, fillPaint)
        } else {
            rect.set(stroke / 2, stroke / 2, w - stroke / 2, hh - stroke / 2)
            if (isFocused) canvas.drawRoundRect(rect, corner, corner, Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = if (Contrast.on) Contrast.focus else SOFT_WHITE })
            else { if (Contrast.on) canvas.drawRoundRect(rect, corner, corner, Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = Contrast.bg }); canvas.drawRoundRect(rect, corner, corner, linePaint) }
        }
        textPaint.color = if (Contrast.on) hcText() else if (isFocused && chase == null) 0xFF08090B.toInt() else textColor
        if (isFocused) { val ring = stroke * (if (Contrast.on) 2.6f else 1.8f); rect.set(ring / 2, ring / 2, w - ring / 2, hh - ring / 2); canvas.drawRoundRect(rect, corner, corner, Paint(Paint.ANTI_ALIAS_FLAG).also { it.style = Paint.Style.STROKE; it.strokeWidth = ring; it.color = if (Contrast.on) Contrast.focus else SOFT_WHITE }) }
        var x = (w - contentWidth()) / 2f
        val fm = textPaint.fontMetrics
        val base = hh / 2f - (fm.ascent + fm.descent) / 2f
        if (play) {
            path.reset(); val top = hh / 2f - tri * 0.55f
            path.moveTo(x, top); path.lineTo(x + tri, hh / 2f); path.lineTo(x, top + tri * 1.1f); path.close()
            val old = textPaint.color; canvas.drawPath(path, textPaint.also { it.style = Paint.Style.FILL }); textPaint.color = old
            x += tri + gap
        }
        canvas.drawText(label, x, base, textPaint)
        canvas.restore()
    }
}

/**
 * The app's start screen on a TV, drawn to match the site's TV landing page: color bars, top bar with the feed tabs, search and
 * My List, hero card, then a row per feed. Plain native views so it moves the way a TV app should.
 * A video plays in MainActivity, the page with YouTube's own embedded player. A phone or tablet skips this screen.
 */
class BrowseActivity : Activity() {
    private val ui = Handler(Looper.getMainLooper())
    private var u = 1f   // one pixel of a 1920-wide design (a phone: of a 640-wide one, held upright)
    /** A phone gets the same native screens, laid out top to bottom. */
    private val phone by lazy { !MainActivity.isTelevision(this) && !MainActivity.isTablet(this) }
    /** The shared left and right edge, in design pixels: the TV's 5% safe area, a slim margin on a phone. */
    private val E get() = if (phone) 30 else 96
    /** How the help lines say it: a remote holds OK on a video, a touch screen presses and holds it. */
    private val holdOn get() = if (MainActivity.isTelevision(this)) "Hold OK on" else "Press and hold"
    private lateinit var scroll: ScrollView
    private lateinit var column: LinearLayout
    private var clips: List<Clip>? = null
    private var loading = true
    private lateinit var header: LinearLayout
    private var tab = 0
    /** -1 shows the stacked feeds (the launch view); 0..3 shows that one feed as a grid of rows, opened from the top menu. */
    private var gridTab = -1
    /** How many videos a feed page shows; "Show More Videos" adds a page, so the screen never builds hundreds of cards at once. */
    private var gridCount = 24
    private var query = ""
    private var showList = false
    private var creator = ""
    private var hideShorts = false
    private var hideBrands = false
    private var sortMode = 0
    private val hidden = HashSet<String>()
    private val shuffleSeed = System.nanoTime()
    private val saved = LinkedHashSet<String>()
    // Creators you follow (kept on this device, apart from My List) and the videos of theirs you have already seen.
    private val following = LinkedHashSet<String>()
    private val seenIds = HashSet<String>()
    private fun isNew(c: Clip) = following.contains(c.channel) && !seenIds.contains(c.id)
    private fun saveFollow() { getSharedPreferences("follow", MODE_PRIVATE).edit().putStringSet("channels", HashSet(following)).putStringSet("seen", HashSet(seenIds)).apply() }
    private fun setFollow(channel: String, on: Boolean) {
        if (on) { following.add(channel); (clips ?: emptyList()).filter { it.channel == channel }.forEach { seenIds.add(it.id) } }   // only videos from now on count as new
        else following.remove(channel)
        saveFollow()
    }
    private var version = ""
    private var focusTag: String? = null

    private class Feed(val id: String, val label: String, val play: String, val blurb: String, val color: Int)
    private val feeds = listOf(
        Feed("main", "Combo", "combo", "All indoor and outdoor robots in one playlist.", 0xFF4EA6E6.toInt()),
        Feed("indoor", "Indoor", "indoor", "Robot vacuums and window-cleaning robots.", 0xFF3FE3E3.toInt()),
        Feed("outdoor", "Outdoor", "outdoor", "Robotic mowers, pool-cleaning robots and garden tools.", 0xFF3FD84F.toInt()),
        Feed("home", "Smart Home", "home", "Smart home technology: cameras, locks, lighting and hubs.", 0xFFF2E84A.toInt()))
    private fun glow(): IntArray { val t = Wallpapers.byId(theme); Glow.current = if (t != null) Glow.forImage(this, t.img) else Glow.plain; return Glow.current }   // no theme: back to the plain blue, not the last wallpaper's colours
    private val rainbow = intArrayOf(0xFFF2F2F2.toInt(), 0xFFF2E84A.toInt(), 0xFF3FE3E3.toInt(), 0xFF3FD84F.toInt(), 0xFFE24FE2.toInt(), 0xFFF24A45.toInt(), 0xFF5B6CFF.toInt(), 0xFFF2F2F2.toInt())

    private val mute = 0xFFB9BEC6.toInt()
    private fun px(v: Int) = (v * u).toInt()
    private fun text(t: String, size: Int, color: Int, weight: Int = 0) = TextView(this).apply {
        text = t; setTextSize(TypedValue.COMPLEX_UNIT_PX, size * u); setTextColor(if (Contrast.on) Contrast.ink(color) else if (color == Color.WHITE) SOFT_WHITE else color)
        typeface = face(weight)
        includeFontPadding = false
    }
    private fun round(v: View, r: Int) { v.outlineProvider = object : ViewOutlineProvider() { override fun getOutline(view: View, o: Outline) { o.setRoundRect(0, 0, view.width, view.height, r * u) } }; v.clipToOutline = true }
    /** Nothing in the app grows when selected (it ran off the screen edge); focus shows as a highlighted border instead. */
    @Suppress("UNUSED_PARAMETER")
    private fun focusable(v: View, scale: Float) { v.isFocusable = true; v.isClickable = true; v.isFocusableInTouchMode = false }
    private fun fill(c: Int, r: Int, stroke: Int = 0, sc: Int = 0): GradientDrawable {
        if (Contrast.on) return GradientDrawable().apply {   // high contrast: solid background or none, a 3 px line round everything, a thick focus-coloured line where the normal app draws a bright selection border
            cornerRadius = px(r).toFloat()
            setColor(when { Color.alpha(c) < 0x70 -> 0x00000000; Color.red(c) + Color.green(c) + Color.blue(c) > 450 -> Contrast.fg; else -> Contrast.bg })
            if (stroke > 0) {
                val bright = Color.alpha(sc) >= 0xC0 && Color.red(sc) >= 0xD0 && Color.green(sc) >= 0xD0 && Color.blue(sc) >= 0xD0
                if (bright) setStroke(px(7), Contrast.focus) else setStroke(px(3), Contrast.fg)
            }
        }
        return GradientDrawable().apply { setColor(c); cornerRadius = px(r).toFloat(); if (stroke > 0) setStroke(px(stroke), sc) }
    }
    private fun modeOf(id: String) = when (id) { "hcblack" -> 1; "hcwhite" -> 2; else -> 0 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A TV, a tablet (by touch) and a phone all get these native screens: wide on a TV or tablet, upright on a phone.
        requestedOrientation = if (phone) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        if (!MainActivity.isTelevision(this)) {
            @Suppress("DEPRECATION") window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        u = resources.displayMetrics.let { if (phone) minOf(it.widthPixels, it.heightPixels) / 640f else maxOf(it.widthPixels, it.heightPixels) / 1920f }
        version = try {
            val pi = packageManager.getPackageInfo(packageName, 0)
            @Suppress("DEPRECATION") val code = if (android.os.Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
            (pi.versionName ?: "") + " (" + code + ")"
        } catch (e: Exception) { "" }
        getSharedPreferences("settings", MODE_PRIVATE).let { hideShorts = it.getBoolean("hideShorts", false); hideBrands = it.getBoolean("hideBrands", false); sortMode = it.getInt("sort", 0); hidden.addAll(it.getStringSet("hidden", emptySet()) ?: emptySet()) }
        saved.addAll(getSharedPreferences("mylist", MODE_PRIVATE).getStringSet("ids", emptySet()) ?: emptySet())
        getSharedPreferences("follow", MODE_PRIVATE).let { following.addAll(it.getStringSet("channels", emptySet()) ?: emptySet()); seenIds.addAll(it.getStringSet("seen", emptySet()) ?: emptySet()) }
        column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; clipChildren = false }
        theme = (getSharedPreferences("settings", MODE_PRIVATE).getString("theme", "none") ?: "none").let { when (it) { "leaves" -> "autumn"; "snow" -> "rockies"; else -> it } }
        if (phone) theme = "none"   // no themes on a phone
        Contrast.mode = modeOf(theme)
        soundLevel = getSharedPreferences("settings", MODE_PRIVATE).getInt("themeSound", 0)
        themeLayer = FrameLayout(this)
        header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0x00000000) }   // one color from the top of the screen down to the color bar
        scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false; clipChildren = false; isVerticalFadingEdgeEnabled = true; setFadingEdgeLength(px(150)); addView(column) }   // rows fade out toward the top and bottom edges
        setContentView(FrameLayout(this).apply { setBackgroundColor(if (Contrast.on) Contrast.bg else 0xFF08090B.toInt()); addView(themeLayer, -1, -1); addView(LinearLayout(this@BrowseActivity).apply { orientation = LinearLayout.VERTICAL; addView(header, -1, -2); addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)) }, -1, -1) })   // the theme is the wallpaper: first child, behind the whole interface
        applyTheme()
        listenForSwipeBack()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        try { render("start") } catch (e: Exception) { column.addView(text("Couldn't draw the start screen: $e", 24, Color.WHITE).apply { setPadding(px(64), px(60), px(64), 0) }) }
        Thread {
            val got = try { load() } catch (e: Exception) { null }
            ui.post { if (!isFinishing) { clips = got; migrateNames(); loading = false; try { render(focusTag) } catch (e: Exception) { column.addView(text("Couldn't draw the videos: $e", 24, Color.WHITE).apply { setPadding(px(64), px(60), px(64), 0) }) } } }
        }.start()
        ui.post { if (!isFinishing) legalGate() }
        ui.postDelayed({ if (!isFinishing) Updater.check(this) }, 20000)
    }

    // ---- drawing -------------------------------------------------------------------------------------------------------------

    private fun render(focus: String?) {
        column.removeAllViews(); cards.clear(); dimmed = false
        themeLayer.findViewWithTag<View>("gg")?.let { themeLayer.removeView(it) }   // a creator page is a full-screen layer behind the menu
        val keep = scroll.scrollY
        // the color bars and the menu stay put at the top while the rows scroll under them
        header.removeAllViews()
        header.addView(View(this), -1, px(48))   // Google's 5% safe area; keeps the menu off the edge of the screen (TVs crop the border); was 44, moved up 14
        header.addView(topBar())
        header.background = if (Contrast.on) null else GradientDrawable(Orientation.TOP_BOTTOM, intArrayOf(0x88000000.toInt(), 0x55000000.toInt(), 0x00000000))   // no color bar: a soft dark screen from the very top that tones the wallpaper down behind the menu
        header.addView(View(this), -1, px(if (phone) 6 else 34))   // a little air between the menu strip and the content
        val all = clips?.filter { visible(it) }
        val everything = clips ?: emptyList()
        if (creator.isNotEmpty()) {
            val hits = (all ?: emptyList()).filter { it.channel == creator }
            column.addView(section(creator, if (hits.isEmpty()) "No videos from this channel in our feeds right now." else "${hits.size} video" + (if (hits.size == 1) "" else "s") + " from this channel in our feeds", feeds[0].color, null, hits, "res"))
        } else if (eggKey(query) == "thievingginger") {
            if (Contrast.on) {   // high contrast: his videos as plain cards, no artwork behind them
                val gl = gingerVideos.map { (id, t, prod) -> Clip(id, "$prod $t", "ThievingGinger", false, "indoor", System.currentTimeMillis()) }
                column.addView(section("Thieving Ginger", "Robot vacuum tests and reviews", feeds[0].color, null, gl, "res"))
            } else {
                themeLayer.addView(gingerPage(), -1, -1)
                column.addView(View(this), 1, 1)   // nothing to scroll: the page is the whole screen
            }
        } else if (eggKey(query) == "vacubotsholiday") {
            val holiday = Clip("X8ky144jmco", "Happy Holidays From Our Home to Yours | VacuBots 2026", "VacuBots", true, "indoor", System.currentTimeMillis())
            column.addView(section("VacuBots Holiday", "Happy holidays from our home to yours.", feeds[0].color, null, listOf(holiday), "res"))
        } else if (query.isNotEmpty()) {
            val q = query.lowercase()
            val hits = (all ?: emptyList()).filter { matches(it, q) }
            column.addView(section("Results for “$query”", if (hits.isEmpty()) "Nothing matches. Try another word." else "${hits.size} video" + (if (hits.size == 1) "" else "s"), feeds[0].color, null, hits, "res"))
        } else if (showList) {
            val mine = everything.filter { saved.contains(it.id) }
            column.addView(section("My List", if (mine.isEmpty()) "$holdOn any video and choose Save to My List." else "Videos you saved. $holdOn one to remove it.", feeds[0].color, null, mine, "list"))
        } else if (gridTab >= 0) {
            val f = feeds[gridTab]
            val pool = all ?: emptyList()
            val base = if (f.id == "main") pool else pool.filter { it.feed == f.id }
            val list = sorted(base)
            if (all == null) column.addView(text(if (loading) "Loading videos…" else "Couldn't load videos. Play This Feed still works.", 22, mute).apply { setPadding(px(E), px(30), 0, 0) })
            else {
                list.take(gridCount).chunked(if (phone) 2 else 4).forEachIndexed { n, row ->
                    column.addView(section(f.label, f.blurb, f.color, f, row, if (n == 0) "feed" + f.id else "grid$n", list, n == 0))
                }
                if (list.size > gridCount) column.addView(outlineButton("Show More Videos", 28, 84, "more") { gridCount += 24; render("more") }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = px(E); topMargin = px(24) })
                if (list.isEmpty()) column.addView(section(f.label, "No videos in this feed right now.", f.color, f, list, "feed" + f.id, list))
            }
        } else {
            column.addView(hero())
            if (all == null && !loading) column.addView(text("Couldn't load videos. Play This Feed still works.", 22, mute).apply { setPadding(px(E), px(30), 0, 0) })
            else if (all == null) column.addView(text("Loading videos…", 22, mute).apply { setPadding(px(E), px(30), 0, 0) })
            val fresh = (all ?: emptyList()).filter { isNew(it) }.sortedByDescending { it.stamp }
            if (fresh.isNotEmpty()) column.addView(section("New From Creators You Follow", "Videos you haven\u2019t watched yet. $holdOn one to follow or unfollow its creator.", 0xFF3FE3E3.toInt(), null, fresh.take(24), "new", fresh))
            val order = listOf(1, 2, 0, 3)
            val indoor = (all ?: emptyList()).filter { it.feed == "indoor" }; val outdoor = (all ?: emptyList()).filter { it.feed == "outdoor" }
            val mixed = ArrayList<Clip>(); for (i in 0 until maxOf(indoor.size, outdoor.size)) { indoor.getOrNull(i)?.let { mixed.add(it) }; outdoor.getOrNull(i)?.let { mixed.add(it) } }
            order.forEach { i ->
                val f = feeds[i]
                val base = if (f.id == "main") (if (sortMode == 0) mixed else indoor + outdoor) else (all ?: emptyList()).filter { it.feed == f.id }
                try { column.addView(section(f.label, f.blurb, f.color, f, pick(sorted(base)), "feed" + f.id, sorted(base))) }
                catch (e: Exception) { column.addView(text("Couldn't draw " + f.label + ": " + e, 20, mute).apply { setPadding(px(64), px(20), px(64), 0) }) }
            }
        }
        column.addView(View(this), -1, px(120))
        val target = focus?.let { header.findViewWithTag<View>(it) ?: column.findViewWithTag<View>(it) }
        if (target != null) { scroll.post { scroll.scrollTo(0, if (focus == "start" || focus.startsWith("tab")) 0 else keep); target.requestFocus() } }
        else column.findViewWithTag<View>("start")?.requestFocus()
    }

    /** Tag of the menu item for the current view, so focus goes back to it after a redraw. */
    private fun tabTag() = if (gridTab < 0) "tabhome" else "tab$gridTab"

    /** The phone's header: the logo and the round search, My List and Settings buttons on one line, the menu under it on the right. */
    private fun phoneTopBar(): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(px(E), px(6), px(E), px(6)) }
        if (canGoBack()) row.addView(NeonButton(this, false, glow(), px(56)).apply { setOnClickListener { goBack() } }, LinearLayout.LayoutParams(px(56), px(56)).apply { rightMargin = px(14) })
        row.addView(ImageView(this).apply { setImageResource(R.drawable.tv_bot) }, LinearLayout.LayoutParams(px(60), px(50)))
        row.addView(text("VacuBots", 32, Color.WHITE, 900).apply { setPadding(px(10), 0, px(10), 0) })
        row.addView(text("TV", 26, Color.WHITE, 900).apply {
            gravity = Gravity.CENTER; setPadding(px(12), px(4), px(12), px(4))
            background = android.graphics.drawable.LayerDrawable(arrayOf(ChaseDrawable(glow(), px(10).toFloat(), px(3).toFloat(), Long.MAX_VALUE / 4), fill(0xFF12161C.toInt(), 8))).also { it.setLayerInset(1, px(3), px(3), px(3), px(3)) }
        })
        row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        fun rb(kind: String, tag: String, on: Boolean, go: () -> Unit) {
            val v = IconView(this, kind, SOFT_WHITE).apply {
                this.tag = tag
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if (on) 0x33F2C230 else 0x14FFFFFF); setStroke(px(2), if (on) 0xFFF2C230.toInt() else 0x33FFFFFF) }
            }
            focusable(v, 1f); v.setOnClickListener { go() }
            row.addView(v, LinearLayout.LayoutParams(px(56), px(56)).apply { leftMargin = px(10) })
        }
        rb("search", "search", false) { openSearch() }
        rb("bookmark", "mylist", showList) { showList = !showList; query = ""; creator = ""; render("mylist") }
        rb("gear", "settings", false) { openSettings() }
        col.addView(row)
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL or Gravity.END; setPadding(px(E - 14), px(4), px(E - 14), px(8)) }
        listOf(-1, 1, 2, 0, 3).forEach { n ->
            val label = if (n < 0) "Home" else feeds[n].label
            val on = (gridTab == n) && query.isEmpty() && creator.isEmpty() && !showList
            val t = text(label, 21, if (on) Color.WHITE else 0xFFE4E7EC.toInt(), if (on) 700 else 500).apply {
                gravity = Gravity.CENTER; setPadding(px(11), 0, px(11), 0); tag = if (n < 0) "tabhome" else "tab$n"
                background = tabBg(on)
                if (on) setShadowLayer(px(9).toFloat(), 0f, 0f, (Glow.current[0] and 0x00FFFFFF) or 0x8C000000.toInt())
            }
            fadeUnlessFocused(t, on); focusable(t, 1f)
            t.setOnClickListener {
                query = ""; creator = ""; showList = false; gridCount = 24
                if (n < 0) { gridTab = -1; tab = 0 } else { gridTab = n; tab = n }
                render(tabTag())
            }
            tabs.addView(t, LinearLayout.LayoutParams(-2, px(56)))
        }
        col.addView(android.widget.HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; isFillViewport = true; addView(tabs, FrameLayout.LayoutParams(-1, -2)) })
        return col
    }

    private fun topBar(): View {
        if (phone) return phoneTopBar()
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(px(E), px(16), px(E), px(26)) }   // no fill: the menu sits straight on the wallpaper
        // High contrast puts the logo on its own row at the right and the menu on a full-width row under it, so all of it fits on the screen.
        val logoRow = if (Contrast.on) LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END or Gravity.CENTER_VERTICAL; setPadding(px(E), px(16), px(E), px(6)) } else bar
        if (Contrast.on) bar.setPadding(px(E), px(8), px(E), px(20))
        var gaps = 0
        fun gap() { if (Contrast.on && gaps++ > 0) bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f)) }
        // Touch screens: a neon back arrow in the bar's left margin (20 + 60 + 16 = 96 px, so the logo and menu stay where they were).
        if (!MainActivity.isTelevision(this) && canGoBack()) {
            logoRow.setPadding(px(0), logoRow.paddingTop, logoRow.paddingRight, logoRow.paddingBottom)
            logoRow.addView(NeonButton(this, false, glow(), px(60)).apply { setOnClickListener { goBack() } }, LinearLayout.LayoutParams(px(60), px(60)).apply { leftMargin = px(20); rightMargin = px(16) })
        }
        logoRow.addView(ImageView(this).apply { setImageResource(R.drawable.tv_bot); translationY = px(6).toFloat()
            if (Contrast.mode == 2) colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f)))   // the robot is a light picture: flipped to dark on the white theme so it shows
        }, px(76), px(64))   // the picture sits high in its frame (faint marks below the body), so nudge the body onto the text line
        logoRow.addView(text("VacuBots", 46, Color.WHITE, 900).apply { setPadding(px(14), 0, px(14), 0) })
        logoRow.addView(text("TV", 38, Color.WHITE, 900).apply {
            gravity = Gravity.CENTER; setPadding(px(18), px(6), px(18), px(6))
            // a dark plate with the site's color-bar colors as a thin rim, so it reads as part of the brand and stays crisp on a TV
            background = android.graphics.drawable.LayerDrawable(arrayOf(ChaseDrawable(glow(), px(10).toFloat(), px(3).toFloat(), Long.MAX_VALUE / 4), fill(0xFF12161C.toInt(), 8))).also { it.setLayerInset(1, px(3), px(3), px(3), px(3)) }
        })
        if (!Contrast.on) bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        val inactive = 0xFFE4E7EC.toInt()
        // menu: Home first (the stacked start page), then Indoor, Outdoor, Combo and Smart Home (each opens that feed as rows of four)
        val nav = listOf(-1, 1, 2, 0, 3)
        nav.forEach { n ->
            val label = if (n < 0) "Home" else feeds[n].label
            val on = (gridTab == n) && query.isEmpty() && creator.isEmpty() && !showList
            val t = text(label, 29, if (on) Color.WHITE else inactive, if (Contrast.on) (if (on) 800 else 600) else (if (on) 600 else 500)).apply {
                gravity = Gravity.CENTER; setPadding(px(22), 0, px(22), 0); tag = if (n < 0) "tabhome" else "tab$n"
                background = tabBg(on)
                if (on && !Contrast.on) setShadowLayer(px(9).toFloat(), 0f, 0f, (Glow.current[0] and 0x00FFFFFF) or 0x8C000000.toInt())   // a light glow in the wallpaper's colour: kept soft so the letters stay crisp
            }
            fadeUnlessFocused(t, on)
            focusable(t, 1f)
            t.setOnClickListener {
                query = ""; creator = ""; showList = false; gridCount = 24
                if (n < 0) { gridTab = -1; tab = 0 } else { gridTab = n; tab = n }
                render(tabTag())
            }
            gap(); bar.addView(t, LinearLayout.LayoutParams(-2, px(84)))
        }
        val iconBg = { android.graphics.drawable.StateListDrawable().apply { addState(intArrayOf(android.R.attr.state_focused), fill(0x40FFFFFF, 40, 4, Color.WHITE)); addState(intArrayOf(), fill(0x00000000, 40)) } }
        val search = IconView(this, "search", SOFT_WHITE).apply { tag = "search"; background = iconBg() }
        fadeUnlessFocused(search, false); focusable(search, 1f); search.setOnClickListener { openSearch() }
        gap(); bar.addView(search, LinearLayout.LayoutParams(px(60), px(60)).apply { leftMargin = px(18); rightMargin = px(8) })
        val mylist = text("My List", 29, if (showList) Color.WHITE else inactive, if (Contrast.on) (if (showList) 800 else 600) else (if (showList) 600 else 500)).apply {
            gravity = Gravity.CENTER; setPadding(px(22), 0, px(22), 0); tag = "mylist"; background = tabBg(showList)
            if (showList && !Contrast.on) setShadowLayer(px(9).toFloat(), 0f, 0f, (Glow.current[0] and 0x00FFFFFF) or 0x8C000000.toInt())
        }
        fadeUnlessFocused(mylist, showList)
        focusable(mylist, 1f); mylist.setOnClickListener { showList = !showList; query = ""; creator = ""; render("mylist") }
        gap(); bar.addView(mylist, LinearLayout.LayoutParams(-2, px(84)))
        val gear = IconView(this, "gear", SOFT_WHITE).apply { tag = "settings"; background = iconBg() }
        fadeUnlessFocused(gear, false); focusable(gear, 1f); gear.setOnClickListener { openSettings() }
        gap(); bar.addView(gear, LinearLayout.LayoutParams(px(60), px(60)).apply { leftMargin = px(8) })
        if (Contrast.on) return LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(logoRow, -1, -2); addView(bar, -1, -2) }
        return bar
    }

    /** The disclaimer at the bottom of every page: where the videos come from, the YouTube terms, and where to read the privacy policy.
     *  On the web page these are links; a TV cannot open links, so the addresses are written out. */
    private fun footer(): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(px(if (phone) 30 else 200), px(70), px(if (phone) 30 else 200), 0) }
        box.addView(View(this).apply { setBackgroundColor(0x22FFFFFF) }, LinearLayout.LayoutParams(-1, 1).apply { bottomMargin = px(34) })
        fun para(t: String, color: Int) = text(t, 20, color).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.3f); setPadding(0, 0, 0, px(16)) }
        box.addView(para("Videos play through YouTube\u2019s official player, where views and ads can support the original creators. To subscribe to a creator or give a video a thumbs up, open it on YouTube.", 0xFFC4C9D0.toInt()))
        box.addView(para("VacuBots does not own these videos. Their inclusion does not constitute an endorsement of VacuBots by the respective brands or creators. If you are a creator and would rather not appear here, contact us at vacubots.com/contact and we will remove your videos.", mute))
        box.addView(para("VacuBots TV uses YouTube API Services. By watching, you agree to be bound by the YouTube Terms of Service at youtube.com/t/terms. Your saved videos stay on this device and are never sent to VacuBots. Read our Privacy Policy at vacubots.com/privacy.", mute))
        return box
    }

    /** "More From VacuBots": the same four cards as the site's TV page, with the same illustrations. Each opens that page inside the app. */
    private fun moreSection(): View {
        val sec = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; clipChildren = false }
        sec.addView(text("More From VacuBots", 44, Color.WHITE, 800).apply { setPadding(px(E), px(70), px(E), px(18)) })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(px(E - 8), px(6), px(E - 8), px(6)); clipChildren = false }
        data class Promo(val art: List<Pair<String, Boolean>>, val color: Int, val title: String, val blurb: String, val cta: String, val url: String)
        listOf(
            Promo(Ill.NEWS, 0xFF4EA6E6.toInt(), "VacuBots News", "New robots, launches and software updates.", "Read Now", "https://www.vacubots.com/news/"),
            Promo(Ill.FINDER, 0xFFC9A8FF.toInt(), "Product Finder", "Pick your robot. Find what fits.", "Find Mine", "https://www.vacubots.com/product-finder/"),
            Promo(Ill.DEALS, 0xFFFF6A5F.toInt(), "VacuBots Deals", "Hand-checked robot deals, posted as we find them.", "Grab a Deal", "https://www.vacubots.com/deals/"),
            Promo(Ill.PARTS, 0xFFFF9A5C.toInt(), "Parts Finder", "Brushes, filters and mop pads by model.", "Find Parts", "https://www.vacubots.com/parts-finder/")
        ).forEach { pr ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(px(30), px(26), px(30), px(26)); tag = "promo" + pr.title
                background = android.graphics.drawable.StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_focused), fill((pr.color and 0x00FFFFFF) or 0x33000000, 22, 4, SOFT_WHITE))
                    addState(intArrayOf(), fill((pr.color and 0x00FFFFFF) or 0x14000000, 22, 2, (pr.color and 0x00FFFFFF) or 0x77000000))
                }
                layoutParams = LinearLayout.LayoutParams(px(410), -2).apply { rightMargin = px(20) }
            }
            card.addView(IllView(this, pr.art, pr.color), LinearLayout.LayoutParams(px(76), px(76)))
            card.addView(text("FROM VACUBOTS", 17, mute, 800).apply { setPadding(0, px(14), 0, 0); letterSpacing = 0.12f })
            card.addView(text(pr.title, 32, pr.color, 800).apply { setPadding(0, px(6), 0, 0) })
            card.addView(text(pr.blurb, 21, mute).apply { setPadding(0, px(8), 0, px(18)); minLines = 2; setLineSpacing(0f, 1.2f) })
            card.addView(text(pr.cta + "  \u2192", 22, pr.color, 600).apply { gravity = Gravity.CENTER; setPadding(px(26), px(12), px(26), px(12)); background = fill(0x00000000, 10, 3, pr.color) },
                LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.END })
            focusable(card, 1f)
            card.setOnClickListener {
                startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_URL, pr.url))
                @Suppress("DEPRECATION") overridePendingTransition(0, 0)
            }
            row.addView(card)
        }
        sec.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; clipChildren = false; addView(row) }, LinearLayout.LayoutParams(-1, -2))
        return sec
    }

    /** Lifts a dialog panel off the app: a soft drop shadow around it and a dimmed backdrop, not a flat black box. */
    private fun lifted(dlg: android.app.Dialog, panel: View, width: Int, height: Int = -2) {
        panel.elevation = px(36).toFloat()
        panel.outlineProvider = object : ViewOutlineProvider() { override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, px(28).toFloat()) } }
        val m = px(if (phone) 14 else 70)
        val w = if (phone) minOf(width, resources.displayMetrics.widthPixels - 2 * m) else width
        val wrap = FrameLayout(this).apply { clipChildren = false; clipToPadding = false; setPadding(m, m, m, m) }
        wrap.addView(panel, FrameLayout.LayoutParams(-1, -2))
        dlg.setContentView(wrap)
        dlg.window?.apply {
            setBackgroundDrawable(ColorDrawable(0x00000000)); addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND); setDimAmount(0.6f)
            setLayout(w + 2 * m, if (height > 0) height + 2 * m else -2)
        }
    }

    private val LEGAL_VERSION = 2   // 2: the cookie wording and consent were added, so everyone sees the screen once more

    /** The first time the app opens (and again if the terms change), the disclaimers appear in one panel with the site's color bar running
     *  around it. The app cannot be used until "I Agree" is selected; Back or "Exit" closes the app. */
    /** The welcome screen. At first run it asks for agreement; opened from Settings (review = true) it shows the same terms with a Done button. */
    private fun legalGate(review: Boolean = false) {
        val prefs = getSharedPreferences("legal", MODE_PRIVATE)
        if (!review && prefs.getInt("agreed", 0) >= LEGAL_VERSION) return
        val dlg = android.app.Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar)
        dlg.setCancelable(review)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(px(if (phone) 26 else 70), px(if (phone) 34 else 50), px(if (phone) 26 else 70), px(if (phone) 28 else 40))
            background = android.graphics.drawable.LayerDrawable(arrayOf(ChaseDrawable(glow(), px(28).toFloat(), px(6).toFloat(), 9000L), fill(0xFF11141A.toInt(), 24))).also { it.setLayerInset(1, px(6), px(6), px(6), px(6)) }
        }
        panel.addView(text("Privacy and Video Information", 46, Color.WHITE, 800))
        panel.addView(text("Learn how VacuBots TV uses YouTube, stores your preferences and supports accessibility.", 24, mute).apply { setPadding(0, px(8), 0, px(16)) })
        // One page of seven cards. It scrolls under the title; the buttons stay pinned at the bottom.
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun card(art: List<Pair<String, Boolean>>, color: Int, head: String, vararg paras: String) {
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(px(26), px(24), px(26), px(26)); background = fill(0xFF171B22.toInt(), 18, 2, 0xFF232933.toInt()) }
            r.addView(IllView(this, art, color), LinearLayout.LayoutParams(px(76), px(76)).apply { rightMargin = px(24); topMargin = px(2) })
            val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            c.addView(text(head, 29, Color.WHITE, 700))
            paras.forEach { c.addView(text(it, 22, 0xFFC4C9D0.toInt()).apply { setPadding(0, px(12), 0, 0); setLineSpacing(0f, 1.3f) }) }
            r.addView(c, LinearLayout.LayoutParams(0, -2, 1f))
            list.addView(r, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = px(16) })
        }
        card(Ill.YOUTUBE, 0xFFFF6A5F.toInt(), "Videos Provided Through YouTube",
            "VacuBots TV uses YouTube API Services and plays videos through YouTube\u2019s official player, where views and ads can support the original creators.",
            "By using this app, you agree to YouTube\u2019s Terms of Service at youtube.com/t/terms.",
            "YouTube may collect data through its player under Google\u2019s Privacy Policy at policies.google.com/privacy.")
        card(Ill.DOC, 0xFF4EA6E6.toInt(), "Independent of Brands and Creators",
            "VacuBots does not own the third-party videos featured in this app. Featuring a video does not imply affiliation with or endorsement by its creator or any brand.",
            "Creators\u2019 opinions are their own and do not necessarily reflect the views of VacuBots.")
        card(Ill.HEART, 0xFFFF9A5C.toInt(), "Support the Creators Featured in Our App",
            "We encourage VacuBots TV users to support the creators featured in our app.",
            "Long-press a video to open it in the YouTube app, where you can like the video or subscribe to the creator\u2019s channel. Following a creator in VacuBots TV does not subscribe you to their YouTube channel.",
            "Creators can request removal of their videos from VacuBots TV by contacting us at vacubots.com/contact.")
        card(Ill.LOCK, 0xFF3FD84F.toInt(), "Preferences Stored on Your Device",
            "Your My List selections, the creators you follow and your app settings are stored locally on this device and are not sent to VacuBots.",
            "Clearing the app\u2019s data or uninstalling the app deletes these preferences.",
            "YouTube and Google may collect data through the video player under Google\u2019s Privacy Policy. Read the VacuBots privacy policy at vacubots.com/privacy.")
        card(Ill.COOKIE, 0xFFFFC46B.toInt(), "Cookies and Similar Technologies",
            "VacuBots TV may use cookies and similar technologies on this device. Your My List, the creators you follow and your settings are saved on this device only.",
            "Videos play through YouTube\u2019s embedded player. The player may use cookies or similar technologies, for example to count views and show ads. Google\u2019s Cookie and Privacy Policy is at policies.google.com/privacy.",
            "By selecting I Agree, you consent to YouTube\u2019s use of these technologies in the player. If you do not agree, select Exit. You can clear everything stored by deleting the app\u2019s data in your device settings.")
        card(Ill.ACCESS, 0xFFB08CFF.toInt(), "Accessibility",
            "VacuBots TV offers multiple themes to accommodate different viewing preferences, including High Contrast Black and High Contrast White, which you can choose in Settings under Theme. We recognize that some viewers may need additional accessibility options.",
            "Video accessibility features depend on what YouTube\u2019s official player supports, so captions and alternative language options may not be available for all videos.",
            "For accessibility feedback or assistance, contact us at vacubots.com/contact.")
        card(Ill.UNAVAILABLE, 0xFFFFD84D.toInt(), "Video Availability",
            "Some videos may become unavailable or have playback restrictions set by YouTube or their creators.")
        val scroller = ScrollView(this).apply { isVerticalScrollBarEnabled = false; isVerticalFadingEdgeEnabled = !Contrast.on; setFadingEdgeLength(px(60)); addView(list) }
        panel.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END or Gravity.CENTER_VERTICAL; clipChildren = false; setPadding(0, px(14), 0, 0) }
        val hint = text("\u25BC  Press Down to keep reading", 22, mute)
        buttons.addView(hint, LinearLayout.LayoutParams(0, -2, 1f))
        val exit = outlineButton("Exit", 26, 70, "exit") { dlg.dismiss(); finish() }
        var atEnd = review
        val agree = chaseButton("I Agree", 0x00000000, SOFT_WHITE, glow(), 28, 74, "agree") {
            if (!atEnd) Toast.makeText(this, "Please Scroll to the End First. Press Down.", Toast.LENGTH_SHORT).show()
            else { prefs.edit().putInt("agreed", LEGAL_VERSION).apply(); dlg.dismiss(); column.findViewWithTag<View>("start")?.requestFocus() }
        }
        val done = chaseButton("Done", 0x00000000, SOFT_WHITE, glow(), 28, 74, "done") { dlg.dismiss(); openSettings(9) }
        if (review) { hint.visibility = View.GONE; buttons.addView(done, LinearLayout.LayoutParams(-2, -2)) }
        else { buttons.addView(exit, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = px(18) }); buttons.addView(agree, LinearLayout.LayoutParams(-2, -2)) }
        panel.addView(buttons, LinearLayout.LayoutParams(-1, -2))
        // I Agree stays locked (dimmed) until the last card is on the screen.
        fun check() {
            val child = scroller.getChildAt(0) ?: return
            if (child.bottom - (scroller.height + scroller.scrollY) <= px(12) && scroller.height > 0) { atEnd = true; agree.alpha = 1f; hint.visibility = View.INVISIBLE }
        }
        if (!review) agree.alpha = 0.4f
        scroller.setOnScrollChangeListener { _, _, _, _, _ -> check() }
        scroller.viewTreeObserver.addOnGlobalLayoutListener { check() }
        dlg.setContentView(panel, android.view.ViewGroup.LayoutParams(-1, -1))
        dlg.window?.setBackgroundDrawable(ColorDrawable(0xCC000000.toInt()))
        dlg.window?.setLayout(if (phone) -1 else px(1320), (resources.displayMetrics.heightPixels * (if (phone) 0.9f else 0.94f)).toInt())
        dlg.setOnKeyListener { _, code, ev ->
            when {
                code == KeyEvent.KEYCODE_BACK && ev.action == KeyEvent.ACTION_UP -> { dlg.dismiss(); if (review) openSettings(9) else finish(); true }
                code == KeyEvent.KEYCODE_DPAD_DOWN && ev.action == KeyEvent.ACTION_DOWN -> { scroller.smoothScrollBy(0, px(300)); true }
                code == KeyEvent.KEYCODE_DPAD_UP && ev.action == KeyEvent.ACTION_DOWN -> { scroller.smoothScrollBy(0, -px(300)); true }
                else -> false
            }
        }
        dlg.show()
        (if (review) done else agree).requestFocus()
    }

    private fun tabBg(on: Boolean) = android.graphics.drawable.StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), fill(0x40FFFFFF, 12, 3, Color.WHITE))
        // No line under the selected tab any more. Normal themes: the word is bright with a light glow and the other tabs fade (see fadeUnlessFocused).
        // High contrast: a steady box (thinner than the 7 px focus box, so the two stay apart) and nothing fades.
        addState(intArrayOf(), if (on && Contrast.on) GradientDrawable().apply { cornerRadius = px(12).toFloat(); setColor(0x00000000); setStroke(px(4), Contrast.focus) } else ColorDrawableBox(0x00000000, 0))
    }
    /** Unselected nav items sit back at about half strength (normal themes only; high contrast keeps full contrast). Focus brings one back to full. */
    private fun fadeUnlessFocused(v: View, selected: Boolean) {
        if (Contrast.on || selected) return
        v.alpha = 0.55f
        v.setOnFocusChangeListener { x, f -> x.alpha = if (f) 1f else 0.55f }
    }
    /** A line along the bottom edge. */
    private class ColorDrawableBox(private val c: Int, private val h: Int) : android.graphics.drawable.Drawable() {
        private val p = Paint().apply { color = c }
        override fun draw(canvas: Canvas) { if (h > 0) canvas.drawRect(bounds.left.toFloat(), (bounds.bottom - h).toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), p) }
        override fun setAlpha(a: Int) {}
        override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
        @Suppress("OVERRIDE_DEPRECATION") override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
    }

    /** The phone's hero card: the picture on top, fading into the card, then the words and the two buttons stacked. */
    private fun phoneHero(): View {
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = fill(0xFF0A0C10.toInt(), 28, 2, 0x17FFFFFF) }
        val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeResource(resources, R.drawable.tv_hero_card, o)
        val ih = px(330); val iw = if (o.outHeight > 0) ih * o.outWidth / o.outHeight else ih * 3
        val imgBox = FrameLayout(this)
        round(imgBox, 28)
        imgBox.addView(ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            Glide.with(this@BrowseActivity).load(R.drawable.tv_hero_card).dontAnimate().transform(com.bumptech.glide.load.resource.bitmap.RoundedCorners(px(28))).into(this)
        }, FrameLayout.LayoutParams(iw, ih, Gravity.END))
        imgBox.addView(View(this).apply { background = GradientDrawable(Orientation.TOP_BOTTOM, intArrayOf(0x000A0C10, 0xFF0A0C10.toInt())) }, FrameLayout.LayoutParams(-1, px(150), Gravity.BOTTOM))
        card.addView(imgBox, LinearLayout.LayoutParams(-1, ih))
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(px(30), 0, px(30), px(30)); clipChildren = false; clipToPadding = false }
        body.addView(text("Your Robot Obsession\nStarts Here.", 46, Color.WHITE, 800).apply { setLineSpacing(0f, 1.02f) })
        body.addView(text("Handpicked Videos. Playing All Day.", 23, Color.WHITE, 0).apply { setPadding(0, px(16), 0, 0) })
        body.addView(text("Explore robot vacuums, robotic mowers and smart home products from the creators we enjoy and the brands we cover.", 20, 0xFFDDE0E4.toInt()).apply { setPadding(0, px(10), 0, 0); setLineSpacing(0f, 1.2f) })
        body.addView(chaseButton("Start Watching", 0xFFDDE0E5.toInt(), 0xFF08090B.toInt(), glow(), 25, 74, "start") { play("combo", null, mainPool()) }, LinearLayout.LayoutParams(-1, px(74)).apply { topMargin = px(24) })
        body.addView(outlineButton("Browse Feeds", 25, 74, "browse") { column.findViewWithTag<View>("feed" + feeds[1].id)?.let { scroll.smoothScrollTo(0, it.top) } }, LinearLayout.LayoutParams(-1, px(74)).apply { topMargin = px(14) })
        card.addView(body, LinearLayout.LayoutParams(-1, -2).apply { topMargin = -px(40) })
        return FrameLayout(this).apply { addView(card, FrameLayout.LayoutParams(-1, -2)); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(px(E), px(20), px(E), 0) } }
    }

    private fun hero(): View {
        if (phone) return phoneHero()
        val hero = FrameLayout(this).apply { background = fill(0xFF0A0C10.toInt(), 28, 2, 0x17FFFFFF) }  // no clip-to-outline: re-clipping every frame under the moving glow was slow
        // The picture and its fade are the site's own hero, rendered from the page's CSS, so it blends exactly the same way.
        if (!Contrast.on) hero.addView(ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            Glide.with(this@BrowseActivity).load(R.drawable.tv_hero_card).dontAnimate().transform(com.bumptech.glide.load.resource.bitmap.RoundedCorners(px(28))).into(this)
        }, -1, -1)
        val heroText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(px(64), 0, 0, 0); clipChildren = false; clipToPadding = false }
        heroText.addView(text("Your Robot Obsession\nStarts Here.", 62, Color.WHITE, 800).apply { setLineSpacing(0f, 1.02f) })
        heroText.addView(text("Handpicked Videos. Playing All Day.", 26, Color.WHITE, 0).apply { setPadding(0, px(20), 0, 0) })
        heroText.addView(text("Explore robot vacuums, robotic mowers and smart home products from the creators we enjoy and the brands we cover.", 20, 0xFFDDE0E4.toInt()).apply { setPadding(0, px(12), 0, 0); setLineSpacing(0f, 1.25f) })
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false; clipToPadding = false; gravity = Gravity.CENTER_VERTICAL }
        buttons.addView(chaseButton("Start Watching", 0xFFDDE0E5.toInt(), 0xFF08090B.toInt(), glow(), 30, 78, "start") { play("combo", null, mainPool()) }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = -px(14); topMargin = -px(14); bottomMargin = -px(14); rightMargin = -px(14) })
        val browse = outlineButton("Browse Feeds", 28, 78, "browse") { column.findViewWithTag<View>("feed" + feeds[1].id)?.let { scroll.smoothScrollTo(0, it.top) } }
        buttons.addView(browse, LinearLayout.LayoutParams(-2, px(78)).apply { leftMargin = px(18); gravity = Gravity.CENTER_VERTICAL })
        heroText.addView(buttons, LinearLayout.LayoutParams(-2, -2).apply { topMargin = px(30) })
        hero.addView(heroText, FrameLayout.LayoutParams(px(800), -1))
        return FrameLayout(this).apply { addView(hero, FrameLayout.LayoutParams(-1, px(560))); layoutParams = LinearLayout.LayoutParams(-1, px(560)).apply { setMargins(px(E), px(40), px(E), 0) } }
    }

    /** A button with a light that chases around its edge (drawn as its background, so it keeps the button's own size). */
    // The four fonts are loaded once; every label used to ask the resources for its font again, hundreds of times per screen.
    private val fontBlack by lazy { resources.getFont(R.font.roboto_black) }
    private val fontBold by lazy { resources.getFont(R.font.roboto_bold) }
    private val fontSemi by lazy { resources.getFont(R.font.poppins_semibold) }
    private val fontMedium by lazy { resources.getFont(R.font.poppins_medium) }
    private fun face(weight: Int) = when { weight >= 900 -> fontBlack; weight >= 800 -> fontBold; weight >= 600 -> fontSemi; else -> fontMedium }

    /** A filled button with a light chasing around its edge. [label] has no arrow; the button draws its own play triangle. */
    private fun chaseButton(label: String, bg: Int, fg: Int, colors: IntArray, size: Int, h: Int, tag: String, onClick: () -> Unit): View {
        val btn = PillButton(this, label, bg, fg, colors, size * u, face(600), px(h), px(46), px(14).toFloat(), px(3).toFloat(), true).apply { this.tag = tag }
        focusable(btn, 1f); btn.setOnClickListener { onClick() }
        return btn
    }

    private fun outlineButton(label: String, size: Int, h: Int, tag: String, onClick: () -> Unit): View {
        val btn = PillButton(this, label, 0, Color.WHITE, null, size * u, face(600), px(h), px(36), px(12).toFloat(), px(3).toFloat(), false).apply { this.tag = tag }
        focusable(btn, 1f); btn.setOnClickListener { onClick() }
        return btn
    }

    private fun section(title: String, blurb: String, color: Int, feed: Feed?, list: List<Clip>, tag: String, pool: List<Clip> = list, header: Boolean = true): View {
        val sec = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; clipChildren = false; this.tag = tag }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = if (phone) Gravity.CENTER_VERTICAL else Gravity.BOTTOM; setPadding(px(E), px(if (phone) 34 else 54), px(E), px(if (phone) 18 else 34)); clipChildren = false }   // 96 px = the TV's 5% safe area, level with the menu (was 64: the icon was cut off at the edge)
        val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        names.addView(text(title, if (phone) 34 else 44, Color.WHITE, 800))
        names.addView(text(blurb, if (phone) 19 else 25, mute).apply { setPadding(0, px(8), 0, 0) })
        if (feed != null) {
            val art = when (feed.id) { "indoor" -> Ill.INDOOR_F; "outdoor" -> Ill.OUTDOOR_F; "home" -> Ill.SMART_F; else -> Ill.COMBO }
            val finder = feed.id == "indoor" || feed.id == "outdoor" || feed.id == "home"   // the three that match the Product Finder's icons
            head.addView(IllView(this, art, color, if (finder) 24f else 96f, if (finder) 1.3f else 0f, if (feed.id == "outdoor") 7 else Int.MAX_VALUE), LinearLayout.LayoutParams(px(if (phone) 62 else 84), px(if (phone) 62 else 84)).apply { rightMargin = px(if (phone) 16 else 24); gravity = Gravity.CENTER_VERTICAL })
        }
        head.addView(names, LinearLayout.LayoutParams(0, -2, 1f))
        if (feed != null) {
            val glow = intArrayOf(color, color, 0xFFFFFFFF.toInt(), color, color)
            if (phone) head.addView(NeonButton(this, false, glow, px(64), true).apply { this.tag = "play" + feed.id; setOnClickListener { play(feed.play, null, pool) } }, LinearLayout.LayoutParams(px(64), px(64)).apply { leftMargin = px(14) })
            else head.addView(chaseButton("Play This Feed", 0xFF08090B.toInt(), color, glow, 26, 64, "play" + feed.id) { play(feed.play, null, pool) }, LinearLayout.LayoutParams(-2, -2))
        } else if (tag == "list" && list.isNotEmpty()) {
            if (phone) head.addView(NeonButton(this, false, intArrayOf(color, color, 0xFFFFFFFF.toInt(), color, color), px(64), true).apply { this.tag = "playlist"; setOnClickListener { play("combo", null, pool) } }, LinearLayout.LayoutParams(px(64), px(64)).apply { leftMargin = px(14) })
            else head.addView(chaseButton("Play My List", 0x00000000, color, intArrayOf(color, color, 0xFFFFFFFF.toInt(), color, color), 26, 64, "playlist") { play("combo", null, pool) }, LinearLayout.LayoutParams(-2, -2))
        } else if (tag == "res") {
            val clear = outlineButton("Clear Search", 24, 66, "clear") { query = ""; creator = ""; render(tabTag()) }
            head.addView(clear, LinearLayout.LayoutParams(-2, px(66)))
        }
        if (header) sec.addView(head)
        if (header && feed != null) sec.addView(View(this).apply { setBackgroundColor(if (Contrast.on) Contrast.fg else (color and 0x00FFFFFF) or 0x99000000.toInt()) }, LinearLayout.LayoutParams(-1, px(3)).apply { leftMargin = px(E); rightMargin = px(E) })   // thin rule in the feed's color, level with the header
        val box = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(px(8), px(14), px(8), px(14)); clipChildren = false; clipToPadding = false }
        list.forEach { c -> box.addView(card(c, feed?.play ?: (feeds.firstOrNull { it.id == c.feed }?.play ?: "combo"), tag, pool)) }
        // A home row ends with a tile that opens the whole feed, and moving right stops there instead of jumping to another row.
        if (feed != null && gridTab < 0 && tag.startsWith("feed")) {
            val more = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(px(24), px(24), px(24), px(24))
                layoutParams = LinearLayout.LayoutParams(px((if (phone) 256 else 340) + 16), px((if (phone) 256 else 340) * 9 / 16 + 8 + 10 + 130)).apply { rightMargin = px(18) }
                background = android.graphics.drawable.StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_focused), fill(0xFF14171C.toInt(), 16, 4, if (Contrast.on) Color.WHITE else color))
                    addState(intArrayOf(), fill(0xFF0F1216.toInt(), 16, 2, (color and 0x00FFFFFF) or 0x66000000))
                }
                focusable(this, 1f); this.tag = "more" + feed.id
                setOnClickListener { query = ""; creator = ""; showList = false; gridCount = 24; gridTab = feeds.indexOf(feed); tab = gridTab; render(tabTag()) }
            }
            more.addView(IconView(this, "arrow", color), LinearLayout.LayoutParams(px(120), px(120)))
            more.addView(text("See All", 30, Color.WHITE, 800).apply { setPadding(0, px(12), 0, 0); gravity = Gravity.CENTER })
            more.addView(text(feed.label, 22, mute).apply { setPadding(px(12), px(8), px(12), 0); gravity = Gravity.CENTER; maxLines = 1; ellipsize = TextUtils.TruncateAt.END })
            more.id = View.generateViewId(); more.nextFocusRightId = more.id
            box.addView(more)
        }
        box.getChildAt(box.childCount - 1)?.let { last -> if (last.id == View.NO_ID) last.id = View.generateViewId(); last.nextFocusRightId = last.id }
        box.getChildAt(0)?.let { first -> if (first.id == View.NO_ID) first.id = View.generateViewId(); first.nextFocusLeftId = first.id }
        // the row sits on the same left and right edges (the 96 px safe area) as the headings, the rule line and the hero card above it
        sec.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; clipChildren = true; clipToPadding = false; addView(box) },
            LinearLayout.LayoutParams(-1, -2).apply { leftMargin = px(E - 8); rightMargin = px(E - 8) })
        return sec
    }

    // While a video is selected, every other video fades back so the one you're on stands out; leaving the rows brings them all back.
    private val cards = ArrayList<View>()
    private var dimmed = false
    /** Dims a card with a dark scrim drawn over it, not by making the card see-through: a see-through card lets the falling leaves behind it show through. */
    private fun dim(v: View, dark: Boolean, animate: Boolean) {
        if (Contrast.on) return   // nothing fades in high contrast
        val d = (v.foreground as? android.graphics.drawable.ColorDrawable) ?: android.graphics.drawable.ColorDrawable(0xFF08090B.toInt()).also { it.alpha = 0; v.foreground = it }
        val to = if (dark) 158 else 0
        if (!animate) { d.alpha = to; return }
        if (d.alpha != to) android.animation.ValueAnimator.ofInt(d.alpha, to).apply { duration = 140; addUpdateListener { d.alpha = it.animatedValue as Int } }.start()
    }

    private fun spotlight(on: View?) {
        if (on == null) {
            if (dimmed) { dimmed = false; cards.forEach { dim(it, false, false) } }
            return
        }
        if (!dimmed) { dimmed = true; cards.forEach { dim(it, it !== on, false) }; return }
        cards.forEach { v -> dim(v, v !== on, true) }
    }

    private fun card(c: Clip, feed: String, sec: String, pool: List<Clip>): View {
        val w = px(if (phone) 256 else 340)   // a phone fits two tiles across
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(w + px(16), -2).apply { rightMargin = px(18) }
            setPadding(px(8), px(8), px(8), px(10))
            background = android.graphics.drawable.StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), android.graphics.drawable.LayerDrawable(arrayOf(fill(0xFF14171C.toInt(), 16), ChaseDrawable(Glow.current, px(16).toFloat(), px(4).toFloat(), 3600L, true))))
                addState(intArrayOf(), fill(0x00000000, 16, 4, 0x00000000))
            }
            focusable(this, 1.04f)
            tag = sec + c.id
            setOnFocusChangeListener { v, has -> if (has) spotlight(v) else v.post { if (cards.none { it.hasFocus() }) spotlight(null) } }
        }
        cards.add(tile)
        val thumb = FrameLayout(this).apply { setBackgroundColor(0xFF101216.toInt()) }
        val img = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setColorFilter(0xFFDCDCDC.toInt(), android.graphics.PorterDuff.Mode.MULTIPLY) }   // a touch dimmer, easier on an OLED
        thumb.addView(img, -1, -1)
        thumb.addView(text(if (c.maker) "Maker" else "Reviewer", 16, Color.WHITE, 500).apply { setPadding(px(10), px(5), px(10), px(5)); setBackgroundColor(if (Contrast.on) Contrast.fg else 0xD108090B.toInt()); if (Contrast.on) setTextColor(Contrast.bg) },
            FrameLayout.LayoutParams(-2, -2, Gravity.START or Gravity.BOTTOM).apply { setMargins(px(10), 0, 0, px(10)) })
        val badge = BookmarkBadge(this).apply { visibility = if (saved.contains(c.id)) View.VISIBLE else View.GONE }
        thumb.addView(badge, FrameLayout.LayoutParams(px(44), px(44), Gravity.END or Gravity.TOP).apply { setMargins(0, px(10), px(10), 0) })
        if (isNew(c)) thumb.addView(text("New", 17, 0xFF08090B.toInt(), 800).apply { setPadding(px(12), px(5), px(12), px(5)); background = fill(0xFF3FE3E3.toInt(), 8) },
            FrameLayout.LayoutParams(-2, -2, Gravity.START or Gravity.TOP).apply { setMargins(px(10), px(10), 0, 0) })
        tile.addView(thumb, w, w * 9 / 16)
        // The sharpest picture YouTube has (1280 x 720); where it has none, the 640-wide one, and only then the 320-wide one (it is soft on a 4K screen).
        Glide.with(this).load("https://i.ytimg.com/vi/" + c.id + "/maxresdefault.jpg")
            .error(Glide.with(this).load("https://i.ytimg.com/vi/" + c.id + "/sddefault.jpg").error(Glide.with(this).load("https://i.ytimg.com/vi/" + c.id + "/mqdefault.jpg")))
            .override(w, w * 9 / 16).dontAnimate()   // full 8-bit colour: the 16-bit format showed banding in dark gradients
            .transform(com.bumptech.glide.load.resource.bitmap.CenterCrop(), com.bumptech.glide.load.resource.bitmap.RoundedCorners(px(10))).into(img)
        tile.addView(text(c.title, 25, Color.WHITE, 600).apply { maxLines = 2; minLines = 2; ellipsize = TextUtils.TruncateAt.END; setPadding(px(4), px(12), px(4), 0); setLineSpacing(0f, 1.15f) })
        tile.addView(text("By " + c.channel, 21, mute).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; setPadding(px(4), px(6), px(4), 0) })
        tile.setOnClickListener { if (seenIds.add(c.id)) saveFollow(); play(feed, c.id, pool) }
        // Holding OK opens a small menu named for the creator: see all of their videos, or save this one to My List.
        tile.setOnLongClickListener {
            val inList = saved.contains(c.id)
            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert).setTitle(c.channel)
                .setItems(arrayOf("More From " + c.channel, if (inList) "Remove From My List" else "Save to My List", if (following.contains(c.channel)) "Unfollow " + c.channel else "Follow " + c.channel) + arrayOf("Open This Video on YouTube")) { _, which ->
                    if (which == 0) { creator = c.channel; query = ""; showList = false; scroll.scrollTo(0, 0); render("res") }
                    else if (which == 3) openVideo(c)
                    else if (which == 2) {
                        val on = !following.contains(c.channel); setFollow(c.channel, on)
                        Toast.makeText(this, if (on) "Following " + c.channel + ". This is not a YouTube subscription." else "Unfollowed " + c.channel, Toast.LENGTH_SHORT).show()
                    }
                    else {
                        if (!saved.remove(c.id)) saved.add(c.id)
                        getSharedPreferences("mylist", MODE_PRIVATE).edit().putStringSet("ids", HashSet(saved)).apply()
                        badge.visibility = if (saved.contains(c.id)) View.VISIBLE else View.GONE
                        Toast.makeText(this, if (saved.contains(c.id)) "Saved to My List" else "Removed From My List", Toast.LENGTH_SHORT).show()
                    }
                }.show()
            true
        }
        return tile
    }

    /** The site's color bars (white, yellow, cyan, green, magenta, red, blue), dimmed like the page. */
    private fun bars(): GradientDrawable = GradientDrawable(Orientation.LEFT_RIGHT, intArrayOf(0xAED8D8D8.toInt(), 0xAED8D800.toInt(), 0xAE00D8D8.toInt(), 0xAE00D800.toInt(), 0xAED800D8.toInt(), 0xAED80000.toInt(), 0xAE0000D8.toInt()))

    /** The hero's left-to-right fade: solid, then clearing so the photo bleeds into the page. */
    private fun fade(): ShapeDrawable = ShapeDrawable(RectShape()).apply {
        shaderFactory = object : ShapeDrawable.ShaderFactory() {
            override fun resize(w: Int, h: Int) = LinearGradient(0f, 0f, w.toFloat(), 0f,
                intArrayOf(0xFF08090B.toInt(), 0xFF08090B.toInt(), 0xCC08090B.toInt(), 0x0008090B), floatArrayOf(0f, .22f, .44f, .76f), Shader.TileMode.CLAMP)
        }
    }

    // ---- actions -------------------------------------------------------------------------------------------------------------

    /** The Main Feed's videos (indoor and outdoor, mixed), as the settings leave them. */
    private fun mainPool(): List<Clip> {
        val all = (clips ?: emptyList()).filter { visible(it) }
        val indoor = all.filter { it.feed == "indoor" }; val outdoor = all.filter { it.feed == "outdoor" }
        val mixed = ArrayList<Clip>(); for (i in 0 until maxOf(indoor.size, outdoor.size)) { indoor.getOrNull(i)?.let { mixed.add(it) }; outdoor.getOrNull(i)?.let { mixed.add(it) } }
        return if (sortMode == 0) mixed else sorted(indoor + outdoor)
    }

    /** A queue for the player that follows the settings: same channel never twice in a row, the picked video first, 150 at most. */
    private fun queue(pool: List<Clip>, first: String?): String {
        val left = ArrayList(pool.take(400)); val out = ArrayList<Clip>()
        first?.let { id -> left.firstOrNull { it.id == id }?.let { left.remove(it); out.add(it) } }
        while (left.isNotEmpty() && out.size < 150) {
            val last = out.lastOrNull()?.channel
            val next = left.firstOrNull { it.channel != last } ?: left[0]
            left.remove(next); out.add(next)
        }
        return out.joinToString(",") { it.id }
    }

    private fun play(feed: String, video: String? = null, pool: List<Clip>? = null) {
        val q = if (pool != null && pool.size >= 2) queue(pool, video) else ""
        startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_URL,
            "https://www.vacubots.com/tv-watch/?feed=$feed" + (if (MainActivity.isTelevision(this)) "&tv=1" else "") + (if (video != null) "&v=$video" else "") + (if (q.isNotEmpty()) "&q=$q" else "")))
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }

    private var recognizer: android.speech.SpeechRecognizer? = null
    private var pendingInput: EditText? = null
    private var pendingDlg: android.app.Dialog? = null
    private var micStatus: TextView? = null
    private var micSub: TextView? = null
    private var micBtn: View? = null
    private var micFocused = false
    private var micListening = false

    private fun openSearch() {
        val dlg = android.app.Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar)
        val input = EditText(this).apply {
            hint = "Search videos and channels"; setTextColor(SOFT_WHITE); setHintTextColor(if (Contrast.on) Contrast.sec else 0xFF667088.toInt()); setSingleLine(true); imeOptions = EditorInfo.IME_ACTION_SEARCH
            setText(query); setTextSize(TypedValue.COMPLEX_UNIT_PX, 34 * u); typeface = face(500); background = null; setPadding(0, 0, px(30), 0)
            setSelection(text.length)
        }
        // Types ahead for you: as you type, the rest of the first matching channel or title fills in, selected. Search takes it; keep typing or Delete to drop it.
        val ideas = ArrayList<String>().apply {
            addAll((clips ?: emptyList()).map { it.channel }.distinct().sorted())
            addAll((clips ?: emptyList()).filter { visible(it) }.map { it.title }.distinct().take(1500))
        }
        input.addTextChangedListener(object : android.text.TextWatcher {
            private var before = 0; private var busy = false
            override fun beforeTextChanged(t: CharSequence?, st: Int, c: Int, a: Int) { before = t?.length ?: 0 }
            override fun onTextChanged(t: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(e: android.text.Editable?) {
                if (busy || e == null) return
                val typed = e.toString()
                if (typed.length <= before || typed.length < 2 || input.selectionStart != typed.length) return   // only when adding letters at the end
                val hit = ideas.firstOrNull { it.length > typed.length && it.startsWith(typed, ignoreCase = true) } ?: return
                busy = true
                try { e.append(hit.substring(typed.length)); input.setSelection(typed.length, hit.length) } catch (x: Exception) { }
                busy = false
            }
        })
        fun go() { stopListening(); dlg.dismiss(); setQuery(input.text.toString()) }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(px(if (phone) 28 else 76), px(if (phone) 36 else 60), px(if (phone) 28 else 76), px(if (phone) 30 else 56))
            background = android.graphics.drawable.LayerDrawable(arrayOf(ChaseDrawable(glow(), px(28).toFloat(), px(6).toFloat(), 9000L), fill(0xFF11141A.toInt(), 24))).also { it.setLayerInset(1, px(6), px(6), px(6), px(6)) }
        }
        panel.addView(text("Search VacuBots TV", 44, Color.WHITE, 800))
        panel.addView(text("Robots, brands and channels", 26, mute).apply { setPadding(0, px(14), 0, 0) })

        // The microphone comes first and holds the focus when the box opens: OK starts listening at once.
        val mic = FrameLayout(this).apply { isFocusable = true; isClickable = true; isFocusableInTouchMode = false }
        mic.addView(IconView(this, "mic", SOFT_WHITE), FrameLayout.LayoutParams(-1, -1))
        micBtn = mic; micFocused = false; micListening = false
        mic.setOnFocusChangeListener { _, f -> micFocused = f; paintMic() }
        mic.setOnClickListener { listen(input, dlg) }
        val status = text("Tap the microphone and speak", 30, Color.WHITE, 600).also { micStatus = it }
        val sub = text("Say a robot, a brand or a channel", 24, mute).apply { setPadding(0, px(12), 0, 0) }.also { micSub = it }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(status); addView(sub) }
        val micRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; clipChildren = false; clipToPadding = false }
        micRow.addView(mic, LinearLayout.LayoutParams(px(112), px(112)).apply { rightMargin = px(40) })
        micRow.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        panel.addView(micRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = px(56); bottomMargin = px(52) })
        paintMic()

        // The search field: a rounded pill with the site's magnifier; the moving outline shows when it has the focus.
        val fieldRest = fill(0xFF10131B.toInt(), 50, 2, 0x44FFFFFF)
        val fieldOn = android.graphics.drawable.LayerDrawable(arrayOf(ChaseDrawable(glow(), px(50).toFloat(), px(5).toFloat(), 3600L, true), fill(0xFF10131B.toInt(), 46))).also { it.setLayerInset(1, px(5), px(5), px(5), px(5)) }
        val field = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = fieldRest; setPadding(px(44), 0, px(14), 0) }
        field.addView(IconView(this, "search", mute), LinearLayout.LayoutParams(px(60), px(60)).apply { rightMargin = px(20) })
        field.addView(input, LinearLayout.LayoutParams(0, -1, 1f))
        input.setOnFocusChangeListener { _, f -> field.background = if (f) fieldOn else fieldRest }
        panel.addView(field, LinearLayout.LayoutParams(-1, px(100)))
        panel.addView(text("Try: Roborock, JustADad, TerraMow, window cleaning", 23, 0xFF7D869C.toInt()).apply { setPadding(0, px(36), 0, 0) })

        val cancel = outlineButton("Cancel", 28, 84, "cancel") { stopListening(); dlg.dismiss() }
        val search = PillButton(this, "Search", 0xFFDDE0E5.toInt(), 0xFF08090B.toInt(), glow(), 28 * u, face(600), px(84), px(64), px(14).toFloat(), px(3).toFloat(), false).apply { tag = "search" }
        focusable(search, 1f); search.setOnClickListener { go() }
        val btns = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END or Gravity.CENTER_VERTICAL; clipChildren = false; clipToPadding = false }
        btns.addView(cancel, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = px(30) })
        btns.addView(search, LinearLayout.LayoutParams(-2, -2))
        panel.addView(btns, LinearLayout.LayoutParams(-1, -2).apply { topMargin = px(56) })

        input.setOnEditorActionListener { _, _, _ -> go(); true }
        dlg.setOnDismissListener { stopListening(); micBtn = null; micStatus = null; micSub = null }
        lifted(dlg, panel, px(1240))
        dlg.show()
        mic.requestFocus()
    }

    /** The microphone's look: a quiet ring at rest, the theme's moving outline when the remote is on it, and a green glow while it listens. */
    private fun paintMic() {
        val b = micBtn ?: return
        val r = px(56).toFloat()
        b.background = when {
            micListening -> android.graphics.drawable.LayerDrawable(arrayOf(ChaseDrawable(Glow.green, r, px(5).toFloat(), 2200L, true), fill(0x337CFF3A, 56))).also { it.setLayerInset(1, px(5), px(5), px(5), px(5)) }
            micFocused -> android.graphics.drawable.LayerDrawable(arrayOf(ChaseDrawable(glow(), r, px(5).toFloat(), 3600L, true), fill(0x26FFFFFF, 56))).also { it.setLayerInset(1, px(5), px(5), px(5), px(5)) }
            else -> fill(0x14FFFFFF, 56, 3, 0x48FFFFFF)
        }
        micStatus?.text = if (micListening) "Listening\u2026" else "Tap the microphone and speak"
        micStatus?.setTextColor(if (micListening && !Contrast.on) 0xFF9DFF72.toInt() else SOFT_WHITE)
        micSub?.text = if (micListening) "Take your time, I wait for a pause" else "Say a robot, a brand or a channel"
    }

    private fun stopListening() { try { recognizer?.cancel(); recognizer?.destroy() } catch (e: Exception) {}; recognizer = null }

    /** Listens with Android's speech recognizer, waits out pauses (about three seconds of quiet ends it), and types what it hears as it goes. */
    private fun listen(input: EditText, dlg: android.app.Dialog) {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pendingInput = input; pendingDlg = dlg
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 78)
            return
        }
        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(this)) { voice(); return }
        stopListening()
        val sr = android.speech.SpeechRecognizer.createSpeechRecognizer(this).also { recognizer = it }
        sr.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { micListening = true; paintMic() }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { micListening = false; paintMic() }
            override fun onEvent(t: Int, p: Bundle?) {}
            override fun onPartialResults(r: Bundle?) {
                r?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { input.setText(it); input.setSelection(it.length) }
            }
            override fun onResults(r: Bundle?) {
                val heard = r?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                micListening = false; paintMic()
                if (heard.isNotEmpty()) { input.setText(heard); stopListening(); dlg.dismiss(); setQuery(heard) }
            }
            override fun onError(e: Int) { micListening = false; paintMic(); Toast.makeText(this@BrowseActivity, "We Didn't Catch That. Select the Microphone and Try Again.", Toast.LENGTH_SHORT).show() }
        })
        sr.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 6000L))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 78) {
            val i = pendingInput; val d = pendingDlg
            if (grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED && i != null && d != null && d.isShowing) listen(i, d)
            else Toast.makeText(this, "Voice Search Needs the Microphone. You Can Still Type.", Toast.LENGTH_LONG).show()
        }
    }

    private fun visible(c: Clip): Boolean =
        !(hideShorts && (c.short || c.title.lowercase().contains("#short"))) && !(hideBrands && c.maker) && !hidden.contains(c.channel)

    private val sortNames = arrayOf("Newest First", "Oldest First", "Channel A to Z", "Shuffled")

    /** Newest first is the order the list already comes in; the others reorder each row. */
    private fun sorted(list: List<Clip>): List<Clip> = when (sortMode) {
        1 -> list.sortedBy { it.stamp }
        2 -> list.sortedWith(compareBy({ it.channel.lowercase() }, { -it.stamp }))
        3 -> list.shuffled(java.util.Random(shuffleSeed))
        else -> list
    }

    private fun saveSettings() {
        getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("hideShorts", hideShorts).putBoolean("hideBrands", hideBrands)
            .putInt("sort", sortMode).putStringSet("hidden", HashSet(hidden)).apply()
    }

    private var theme = "none"
    private lateinit var themeLayer: FrameLayout
    /** Puts the chosen theme behind the interface. "No Theme" is the plain dark screen. */
    private fun applyTheme() {
        themeLayer.removeAllViews()
        Contrast.mode = modeOf(theme)
        (themeLayer.parent as? View)?.setBackgroundColor(if (Contrast.on) Contrast.bg else 0xFF08090B.toInt())
        if (Contrast.on) { applySound(); return }   // no wallpaper, no creator artwork, no motion
        val t = Wallpapers.byId(theme) ?: run { if (eggKey(query) == "thievingginger") themeLayer.addView(gingerPage(), -1, -1); applySound(); return }
        Wallpapers.build(this, t).forEach { themeLayer.addView(it, -1, -1) }
        if (eggKey(query) == "thievingginger") themeLayer.addView(gingerPage(), -1, -1)
        val shown = theme
        Videos.sync(this) { runOnUiThread { if (theme == shown && !isFinishing) applyTheme() } }
        applySound()
    }

    private var soundLevel = 0

    /** Starts (or stops) the quiet sound that goes with the current theme. */
    private fun applySound() {
        val kind = Wallpapers.byId(theme)?.let { Ambience.forTheme[it.id] } ?: -1
        if (soundLevel > 0) Ambience.sync(this) { runOnUiThread { Ambience.set(kind, soundLevel) } }
        Ambience.set(kind, soundLevel)
    }

    override fun onPause() { Ambience.stop(); super.onPause() }
    // A dialog (Settings, a picker) takes the window focus: the full-screen wallpaper layers and the video behind it stand still, which frees most of the graphics work for the dialog.
    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); Effects.pauseWallpapers = !hasFocus }
    /** While a video plays (the player is another screen on top of this one) the wallpaper video lets go of the TV's video decoder: a streamer
     *  has only one or two, and a 4K loop left running behind the player can starve it. The view stays, so nothing flashes on return. */
    private fun wallpaperVideos(f: (Videos.LoopVideo) -> Unit) { if (::themeLayer.isInitialized) for (i in 0 until themeLayer.childCount) (themeLayer.getChildAt(i) as? Videos.LoopVideo)?.let(f) }
    override fun onStop() { super.onStop(); wallpaperVideos { it.suspendPlayback() } }
    override fun onResume() { super.onResume(); wallpaperVideos { it.resumePlayback() }; applySound() }

    private fun themeName() = Wallpapers.byId(theme)?.name ?: when (theme) { "hcblack" -> "High Contrast Black"; "hcwhite" -> "High Contrast White"; else -> "No Theme" }

    /** A grid of small previews, one per theme; the remote moves across them and OK picks one. */
    private fun themePicker() {
        val dlg = android.app.Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(px(48), px(36), px(48), px(30))
            background = android.graphics.drawable.LayerDrawable(arrayOf(ChaseDrawable(glow(), px(28).toFloat(), px(6).toFloat(), 9000L), fill(0xFF11141A.toInt(), 24))).also { it.setLayerInset(1, px(6), px(6), px(6), px(6)) }
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; clipChildren = false }
        top.addView(text("Choose a Theme", 42, Color.WHITE, 800), LinearLayout.LayoutParams(0, -2, 1f))
        val back = chaseButton("Back", 0x00000000, SOFT_WHITE, glow(), 28, 74, "back") { dlg.dismiss(); openSettings(6) }
        fun pick(id: String) { theme = id; getSharedPreferences("settings", MODE_PRIVATE).edit().putString("theme", id).apply(); applyTheme(); render("settings"); dlg.dismiss(); openSettings(6) }
        top.addView(back, LinearLayout.LayoutParams(-2, -2))
        panel.addView(top, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = px(10) })
        val entries = listOf<Pair<String, Pair<String, Int?>>>("none" to ("No Theme" to null), "hcblack" to ("High Contrast Black" to null), "hcwhite" to ("High Contrast White" to null)) + Wallpapers.all.map { it.id to (it.name to it.img) }
        var focus: View? = null
        entries.chunked(6).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, px(6), 0, px(6)) }
            for (i in group.indices) {
                val e = group[i]
                val (id, info) = e
                val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(px(8), px(8), px(8), px(8)); isFocusable = true; isClickable = true }
                val tid = id
                val thumb = android.widget.ImageView(this).apply { scaleType = android.widget.ImageView.ScaleType.CENTER_CROP; setBackgroundColor(when (tid) { "hcblack" -> 0xFF000000.toInt(); "hcwhite" -> 0xFFFFFFFF.toInt(); else -> 0xFF08090B.toInt() }) }
                if (info?.second != null) {
                    val o = BitmapFactory.Options().apply { inSampleSize = 4 }
                    thumb.setImageBitmap(BitmapFactory.decodeResource(resources, info.second!!, o))
                }
                card.addView(thumb, LinearLayout.LayoutParams(-1, px(112)))
                card.addView(text(info!!.first + if (id == theme) "  \u2713" else "", 18, Color.WHITE, 600).apply { setPadding(0, px(8), 0, 0); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END })
                val rest = fill(0xFF171B22.toInt(), 16, 2, if (id == theme) 0xFF8FC4F0.toInt() else 0xFF232933.toInt())
                card.background = rest
                card.setOnFocusChangeListener { v, f -> v.background = if (f) fill(0x2EFFFFFF, 16, 3, Color.WHITE) else rest }
                card.setOnClickListener { pick(id) }
                row.addView(card, LinearLayout.LayoutParams(px(228), -2).apply { leftMargin = px(6); rightMargin = px(6) })
                if (id == theme) focus = card
            }
            panel.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        lifted(dlg, panel, px(1560))
        dlg.setOnCancelListener { openSettings(6) }
        dlg.show()
        (focus ?: back).requestFocus()
    }

    private fun dark() = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)

    /** One card of the Settings screen: a neon icon, a short name, the current choice in its own color, and a quieter explainer line; a highlight when the remote lands on it. */
    private fun settingsCard(title: String, value: String, valueColor: Int, sub: String, art: List<Pair<String, Boolean>>, neon: Long, onClick: () -> Unit): View {
        val card = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(px(22), px(18), px(22), px(18)); tag = title }
        card.addView(IllView(this, art, neon.toInt()), LinearLayout.LayoutParams(px(64), px(64)).apply { rightMargin = px(20) })
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(text(title, 27, Color.WHITE, 700))
        if (value.isNotEmpty()) col.addView(text(value, 25, valueColor, 800).apply { setPadding(0, px(3), 0, 0) })
        if (sub.isNotEmpty()) col.addView(text(sub, 19, mute).apply { setPadding(0, px(6), 0, 0); setLineSpacing(0f, 1.18f) })
        card.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        val rest = fill(0xFF171B22.toInt(), 18, 2, 0xFF232933.toInt())
        card.background = rest
        card.isFocusable = true; card.isClickable = true
        card.setOnFocusChangeListener { v, f -> v.background = if (f) fill(0x2EFFFFFF, 18, 3, Color.WHITE) else rest }
        card.setOnClickListener { onClick() }
        return card
    }

    /** The Settings screen, laid out like the welcome screen: one wide panel of cards, so nothing scrolls off the screen. Each card changes (or opens) something and the screen comes back with the same card selected. */
    /** One line in the Settings panel. */
    private class SRow(val tagName: String, val art: List<Pair<String, Boolean>>, val neon: Int, val label: String, val beta: Boolean,
                       val value: String, val valueColor: Int, val chevron: Boolean, val hint: String, val onClick: () -> Unit)

    /** Settings: categories down the left, the rows of the highlighted category on the right, one line of help underneath. Back closes it.
     *  focusRow numbers the rows in order: 0-5 Content (Following, Sort Order, Creators, Brands, Shorts, Brand Videos), 6-7 Appearance
     *  (Theme, Theme Sounds), 8-9 About & Help (App Version, Privacy and Video Information). Other screens use 6, 7 and 9 to come back. */
    private fun openSettings(focusRow: Int = 0) {
        val creators = (clips ?: emptyList()).filter { !it.maker }.map { it.channel }.toSet()
        val brands = (clips ?: emptyList()).filter { it.maker }.map { it.channel }.toSet()
        fun shown(all: Set<String>) = all.count { !hidden.contains(it) }
        val dlg = android.app.Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(px(if (phone) 26 else 64), px(if (phone) 30 else 44), px(if (phone) 26 else 64), px(if (phone) 24 else 36)); minimumHeight = if (phone) 0 else px(860)
            background = android.graphics.drawable.LayerDrawable(arrayOf(ChaseDrawable(glow(), px(28).toFloat(), px(6).toFloat(), 9000L), fill(0xFF11141A.toInt(), 24))).also { it.setLayerInset(1, px(6), px(6), px(6), px(6)) }
        }
        // On a tablet (a phone never gets these native screens) there is no remote Back key, so the heading line carries the same round X as the player's: tap it to close.
        val titleRow = FrameLayout(this)
        titleRow.addView(text("Settings", 46, Color.WHITE, 800), FrameLayout.LayoutParams(-2, -2, android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START))
        if (!MainActivity.isTelevision(this)) {
            titleRow.addView(NeonButton(this, true, glow(), px(60)).apply { setOnClickListener { dlg.cancel() } },
                FrameLayout.LayoutParams(px(60), px(60), android.view.Gravity.TOP or android.view.Gravity.END))
        }
        panel.addView(titleRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = px(14) })
        val on = 0xFF39FF14.toInt(); val off = 0xFFFF4D4D.toInt()
        val soundColors = intArrayOf(off, 0xFF00F0FF.toInt(), 0xFFFFE600.toInt(), 0xFF39FF14.toInt())
        val white = Color.WHITE
        val keep = if (phone) listOf(0, 2) else listOf(0, 1, 2)   // a phone has no themes, so no Appearance
        val allPages: List<List<SRow>> = listOf(
            listOf(
                SRow("following", Ill.S_PERSON, 0xFFB026FF.toInt(), "Following", false, if (following.isEmpty()) "None Yet" else following.size.toString() + " Followed", white, true,
                    if (following.isEmpty()) "$holdOn any video and choose Follow." else "New videos from creators you follow show on Home. Select to manage.") { dlg.dismiss(); followPicker() },
                SRow("sort", Ill.S_SORT, 0xFF39FF14.toInt(), "Sort Order", false, sortNames[sortMode], white, false,
                    "Each press moves to the next choice: " + sortNames.joinToString(", ") + ".") { sortMode = (sortMode + 1) % sortNames.size; saveSettings(); dlg.dismiss(); openSettings(1) },
                SRow("creators", Ill.S_PERSON, 0xFF00F0FF.toInt(), "Creators", false, if (creators.isEmpty()) "Loading" else shown(creators).toString() + " of " + creators.size + " Shown", white, true,
                    "Select to choose which creators to show.") { dlg.dismiss(); channelPicker(false) },
                SRow("brands", Ill.S_TAG, 0xFFFF9500.toInt(), "Brands", false, if (brands.isEmpty()) "Loading" else shown(brands).toString() + " of " + brands.size + " Shown", white, true,
                    "Select to choose which brands to show.") { dlg.dismiss(); channelPicker(true) },
                SRow("shorts", Ill.S_SHORTS, 0xFFFF2E9A.toInt(), "Shorts", false, if (hideShorts) "Off" else "On", if (hideShorts) off else on, false,
                    "Shorts are very short vertical videos. Select to turn them on or off.") { hideShorts = !hideShorts; saveSettings(); dlg.dismiss(); openSettings(4) },
                SRow("brandvideos", Ill.S_TAG, 0xFFFFE600.toInt(), "Brand Videos", false, if (hideBrands) "Off" else "On", if (hideBrands) off else on, false,
                    "Videos from the companies that make the products. Select to turn them on or off.") { hideBrands = !hideBrands; saveSettings(); dlg.dismiss(); openSettings(5) }),
            listOf(
                SRow("theme", Ill.S_IMAGE, 0xFFFF7A3B.toInt(), "Theme", false, themeName(), white, true,
                    "Animated wallpapers behind the screen, plus two high contrast themes. Select to pick one.") { dlg.dismiss(); themePicker() },
                SRow("sounds", Ill.S_SOUND, 0xFF2E7BFF.toInt(), "Theme Sounds", true, Ambience.levelNames[soundLevel], soundColors[soundLevel], false,
                    "Quiet sounds for some themes. Press to change the volume.") { soundLevel = (soundLevel + 1) % Ambience.levelNames.size; getSharedPreferences("settings", MODE_PRIVATE).edit().putInt("themeSound", soundLevel).apply(); applySound(); dlg.dismiss(); openSettings(7) }),
            listOf(
                SRow("version", Ill.S_UPDATE, 0xFF00FFC8.toInt(), "App Version", false, version, white, false,
                    "Select to check for a new version.") { dlg.dismiss(); Updater.check(this, true) },
                SRow("terms", Ill.S_DOC, 0xFFC9A8FF.toInt(), "Privacy and Video Information", false, "", white, true,
                    "Read how VacuBots TV uses YouTube and your preferences.") { dlg.dismiss(); legalGate(true) }))
        val pages = keep.map { allPages[it] }
        val catNames = listOf("Content", "Appearance", "About & Help").filterIndexed { i, _ -> i in keep }
        val catArt = listOf(Ill.S_CONTENT, Ill.S_LOOK, Ill.S_INFO).filterIndexed { i, _ -> i in keep }
        val catColor = intArrayOf(0xFF00F0FF.toInt(), 0xFFFF4FD8.toInt(), 0xFFFFE600.toInt()).filterIndexed { i, _ -> i in keep }.toIntArray()
        val catHint = listOf("Choose what shows and how it is sorted.", "Change how VacuBots TV looks and sounds.", "Version, updates and privacy information.").filterIndexed { i, _ -> i in keep }
        fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)
        val hint = text("", 24, mute).apply { setLineSpacing(0f, 1.2f); minLines = 1 }
        val heading = text("", 40, Color.WHITE, 800).apply { gravity = Gravity.CENTER_VERTICAL }
        val rowsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val catBox = LinearLayout(this).apply { orientation = if (phone) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL }
        val lineColor = if (Contrast.on) Contrast.fg else 0x22FFFFFF
        var cur = -1
        var rowViews = listOf<View>()
        val catViews = ArrayList<View>()

        fun refreshCats() {
            catViews.forEachIndexed { i, v ->
                v.background = android.graphics.drawable.StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_focused), fill(0x2EFFFFFF, 18, 4, Color.WHITE))
                    addState(intArrayOf(), when {
                        i != cur -> fill(0x00000000, 18)
                        Contrast.on -> android.graphics.drawable.GradientDrawable().apply { cornerRadius = px(12).toFloat(); setColor(0x00000000); setStroke(px(4), Contrast.focus) }   // high contrast: a box on the chosen category
                        else -> fill(0xFF2A3350.toInt(), 18)
                    })
                }
                v.alpha = if (Contrast.on || i == cur) 1f else 0.62f   // the other categories sit back; high contrast keeps full contrast
            }
        }
        fun rowView(r: SRow): View {
            val v = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(px(if (phone) 6 else 34), 0, px(if (phone) 4 else 30), 0); tag = r.tagName; id = View.generateViewId() }
            v.addView(IllView(this, r.art, r.neon, 24f, 1.9f), lp(px(40), px(40)).apply { rightMargin = px(if (phone) 18 else 28) })
            v.addView(text(r.label, if (phone) 27 else 30, Color.WHITE, 600))
            if (r.beta) v.addView(text("BETA", 16, 0xFFFFB020.toInt(), 800).apply { letterSpacing = 0.12f; setPadding(px(12), px(5), px(12), px(5)); background = fill(0x00000000, 40, 2, 0xFFFFB020.toInt()) }, lp(-2, -2).apply { leftMargin = px(16) })
            v.addView(View(this), lp(0, 1, 1f))
            if (r.value.isNotEmpty()) v.addView(text(r.value, if (phone) 24 else 28, r.valueColor, 600))
            v.addView(if (r.chevron) IllView(this, Ill.S_CHEV, SOFT_WHITE, 24f, 3f) else View(this), lp(px(26), px(26)).apply { leftMargin = px(20) })   // every row keeps the arrow's slot so the values line up
            v.background = android.graphics.drawable.StateListDrawable().apply { addState(intArrayOf(android.R.attr.state_focused), fill(0x2EFFFFFF, 18, 4, Color.WHITE)); addState(intArrayOf(), fill(0x00000000, 18)) }
            v.isFocusable = true; v.isClickable = true; v.isFocusableInTouchMode = false
            v.setOnFocusChangeListener { _, f -> if (f) hint.text = r.hint }
            v.setOnClickListener { r.onClick() }
            return v
        }
        fun showCat(i: Int) {
            cur = i
            heading.text = catNames[i]
            rowsBox.removeAllViews()
            rowViews = pages[i].map { r ->
                val v = rowView(r)
                rowsBox.addView(v, lp(-1, px(84)))
                rowsBox.addView(View(this).apply { setBackgroundColor(lineColor) }, lp(-1, px(2)).apply { bottomMargin = px(4) })   // row + rule + gap = 90, the categories' pitch
                v
            }
            rowViews.forEach { it.nextFocusLeftId = catViews[i].id }
            catViews.forEach { it.nextFocusRightId = rowViews.first().id }
            hint.text = catHint[i]
            refreshCats()
        }
        catNames.forEachIndexed { i, name ->
            val v = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(px(if (phone) 14 else 34), 0, px(if (phone) 12 else 30), 0); tag = "cat$i"; id = View.generateViewId() }
            v.addView(IllView(this, catArt[i], catColor[i], 24f, 1.9f), lp(px(if (phone) 34 else 40), px(if (phone) 34 else 40)).apply { rightMargin = px(if (phone) 12 else 26) })
            v.addView(text(name, if (phone) 25 else 30, Color.WHITE, 600))
            v.isFocusable = true; v.isClickable = true; v.isFocusableInTouchMode = false
            v.setOnFocusChangeListener { _, f -> if (f) { if (cur != i) showCat(i) else hint.text = catHint[i] } }
            v.setOnClickListener { showCat(i); rowViews.firstOrNull()?.requestFocus() }   // a tap has no focus to follow, so it opens the category itself
            catViews.add(v)
            if (phone) catBox.addView(v, lp(0, px(72), 1f).apply { rightMargin = px(8) }) else catBox.addView(v, lp(-1, px(84)).apply { bottomMargin = px(6) })
        }
        if (phone) {
            panel.addView(catBox, lp(-1, -2).apply { topMargin = px(8) })
            panel.addView(heading, lp(-1, px(76)))
            panel.addView(rowsBox, lp(-1, -2))
        } else {
            val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            left.addView(View(this), lp(1, px(80)))   // level with the heading, so a category and the row beside it share a line
            left.addView(catBox, lp(-1, -2))
            val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            right.addView(heading, lp(-1, px(80)))
            right.addView(rowsBox, lp(-1, -2))
            val body = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; minimumHeight = px(6 * 90 + 80) }
            body.addView(left, lp(px(390), -2))
            body.addView(View(this).apply { setBackgroundColor(lineColor) }, lp(px(2), -1).apply { topMargin = px(6); bottomMargin = px(6) })
            body.addView(right, lp(0, -2, 1f).apply { leftMargin = px(46) })
            panel.addView(body, lp(-1, -2))
        }
        panel.addView(View(this).apply { setBackgroundColor(lineColor) }, lp(-1, px(2)).apply { topMargin = px(22); bottomMargin = px(20) })
        panel.addView(hint, lp(-1, -2))
        val catRaw = when { focusRow <= 5 -> 0; focusRow <= 7 -> 1; else -> 2 }
        val cat = keep.indexOf(catRaw).coerceAtLeast(0)
        val rowIn = if (catRaw == 0) focusRow.coerceIn(0, 5) else if (catRaw == 1) (focusRow - 6).coerceIn(0, 1) else (focusRow - 8).coerceIn(0, 1)
        showCat(cat)
        lifted(dlg, panel, px(1560))
        dlg.setOnCancelListener { render("settings") }
        dlg.show()
        rowViews[rowIn].requestFocus()
    }

    /** A short plain-language notice for the TV, where the full policy page can't be opened. */
    /** Opens this video in the YouTube app (the TV's own YouTube app first), where it can be liked, commented on and its channel subscribed to. */
    private fun openVideo(c: Clip) {
        val uri = Uri.parse("https://www.youtube.com/watch?v=" + c.id)
        for (pkg in listOf("com.google.android.youtube.tv", "com.google.android.youtube", null)) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri).apply { if (pkg != null) setPackage(pkg); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                return
            } catch (e: Exception) { /* try the next one */ }
        }
        Toast.makeText(this, "Couldn\u2019t Open YouTube. Search for this video in the YouTube app.", Toast.LENGTH_LONG).show()
    }

    /** The creators you follow, each with a tick: unticked ones are unfollowed. */
    private fun followPicker() {
        val names = following.sortedBy { it.lowercase() }
        if (names.isEmpty()) { Toast.makeText(this, "You Aren\u2019t Following Anyone Yet. " + (if (MainActivity.isTelevision(this)) "Hold OK on a Video" else "Press and Hold a Video") + " to Follow Its Creator.", Toast.LENGTH_LONG).show(); openSettings(); return }
        val on = BooleanArray(names.size) { true }
        dark().setTitle("Creators You Follow")
            .setMultiChoiceItems(Array(names.size) { names[it] }, on) { _, i, v -> on[i] = v }
            .setPositiveButton("Save Changes") { _, _ -> names.forEachIndexed { i, n -> if (!on[i]) setFollow(n, false) }; openSettings() }
            .setNegativeButton("Cancel") { _, _ -> openSettings() }.show()
    }

    /** Every creator (or brand) in the feeds with a tick: ticked ones show up, unticked ones are left out of the rows. */
    private fun channelPicker(makers: Boolean) {
        val counts = HashMap<String, Int>()
        (clips ?: emptyList()).filter { it.maker == makers }.forEach { counts[it.channel] = (counts[it.channel] ?: 0) + 1 }
        val names = counts.keys.sortedBy { it.lowercase() }
        if (names.isEmpty()) { Toast.makeText(this, "The List Is Still Loading. Try Again in a Moment.", Toast.LENGTH_SHORT).show(); openSettings(if (makers) 3 else 2); return }
        val on = BooleanArray(names.size) { !hidden.contains(names[it]) }
        dark().setTitle(if (makers) "Choose Which Brands to Show" else "Choose Which Creators to Show")
            .setMultiChoiceItems(Array(names.size) { names[it] + "  (" + counts[names[it]] + ")" }, on) { _, i, v -> on[i] = v }
            .setPositiveButton("Save Changes") { _, _ -> names.forEachIndexed { i, n -> if (on[i]) hidden.remove(n) else hidden.add(n) }; saveSettings(); openSettings(if (makers) 3 else 2) }
            .setNeutralButton("Show All") { _, _ -> names.forEach { hidden.remove(it) }; saveSettings(); openSettings(if (makers) 3 else 2) }
            .setNegativeButton("Cancel") { _, _ -> openSettings(if (makers) 3 else 2) }.show()
    }

    /** "just a dad" finds JustADad: every word must be in the title or channel, or the whole phrase run together must be in the channel's name. */
    private fun matches(c: Clip, q: String): Boolean {
        val hay = (c.title + " " + c.channel).lowercase()
        val words = q.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        if (words.all { hay.contains(it) }) return true
        val squash = { t: String -> t.lowercase().filter { it.isLetterOrDigit() } }
        return squash(c.channel).contains(squash(q)) || squash(c.title).contains(squash(q))
    }

    private fun voice() {
        try {
            startActivityForResult(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a robot, a brand or a channel"), 77)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Voice Search Is Not Available on This Device. Please Type Instead.", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 77 && resultCode == RESULT_OK) setQuery(data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: "")
    }

    /** A search that is only a name (capitals, spaces and punctuation ignored) opens a hidden page: "ThievingGinger" or "VacuBots Holiday". */
    private fun eggKey(q: String): String? {
        val k = q.lowercase().filter { it.isLetterOrDigit() }
        if (k.length < 6) return null
        // Voice search hears "thieving ginger" as "thief in ginger", "thieving ginja" and the like, so a close spelling is enough.
        fun close(a: String, b: String): Boolean {
            if (kotlin.math.abs(a.length - b.length) > 4) return false
            var prev = IntArray(b.length + 1) { it }
            for (i in 1..a.length) { val cur = IntArray(b.length + 1); cur[0] = i
                for (j in 1..b.length) cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = cur }
            return prev[b.length] <= 4
        }
        return when { close(k, "thievingginger") || (k.contains("ginger") && k.length <= 20) -> "thievingginger"
            (k.length >= 8 && (close(k, "vacubotsholiday") || (k.contains("holiday") && k.contains("bots")))) -> "vacubotsholiday"; else -> null }
    }

    /** id, the video's own name, and the product it is about (brand casing as the brand writes it). */
    private val gingerVideos = listOf(
        Triple("5FiHxa7AqeA", "Why Are You Fighting Me?", "MOVA LiDAX Ultra 2000"),
        Triple("sJoXihll8L0", "Heavy Pet Hair Test", "MOVA Z60"),
        Triple("rrk9kXDg1ec", "Dried Mud Test", "MOVA Z60"),
        Triple("lZhTgHOAQVk", "Water Stain Test", "MOVA Z60"))

    /** ThievingGinger's creator page: his artwork fills the screen (logo included, as drawn) and his videos are tiles in the open blue. */
    private fun gingerPage(): View {
        val w = resources.displayMetrics.widthPixels; val h = resources.displayMetrics.heightPixels
        val pool = gingerVideos.map { (id, t, prod) -> Clip(id, "$prod $t", "ThievingGinger", false, "indoor", System.currentTimeMillis()) }
        val page = FrameLayout(this).apply { tag = "gg" }
        page.addView(ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setImageResource(R.drawable.tv_gg_bg) }, FrameLayout.LayoutParams(-1, -1))
        // The open blue to the right of him: two by two, real 16:9 pictures with the title underneath, inside the TV-safe area, clear of the menu above and his logo below.
        // The stadium floodlights glow: a soft white-blue halo over the lamps that breathes slowly, and a wider haze that breathes against it.
        fun halo(sizePx: Int, cx: Float, cy: Float, core: Int, from: Float, to: Float, ms: Long, grow: Float) {
            val v = View(this).apply {
                background = GradientDrawable().apply { setShape(GradientDrawable.OVAL); setGradientType(GradientDrawable.RADIAL_GRADIENT); setGradientRadius(sizePx / 2f); setColors(intArrayOf(core, (core and 0x00FFFFFF))) }
                alpha = from
            }
            page.addView(v, FrameLayout.LayoutParams(sizePx, sizePx).apply { leftMargin = (w * cx).toInt() - sizePx / 2; topMargin = (h * cy).toInt() - sizePx / 2 })
            val anim = android.animation.ObjectAnimator.ofPropertyValuesHolder(v,
                android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, from, to),
                android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, grow), android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, grow)).apply {
                duration = ms; repeatCount = android.animation.ValueAnimator.INFINITE; repeatMode = android.animation.ValueAnimator.REVERSE
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            }
            v.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(x: View) { if (!anim.isStarted) anim.start() }
                override fun onViewDetachedFromWindow(x: View) { anim.cancel() }
            })
        }
        halo((w * 0.30f).toInt(), 0.232f, 0.285f, 0x4DBFE3FF, 0.35f, 0.85f, 3200L, 1.18f)   // wide haze
        halo((w * 0.115f).toInt(), 0.232f, 0.285f, 0xCCFFFFFF.toInt(), 0.45f, 1f, 2300L, 1.12f)   // the lamps themselves
        // Laid out from the approved mockup: two cards a row, a small inset around a wide picture, the title big and the product under it.
        val tw = (w * 0.266f).toInt(); val inset = (w * 0.012f).toInt(); val pw = tw - 2 * inset; val pic = (h * 0.1817f).toInt()
        val th = (h * 0.2907f).toInt();   // shorter tiles: room between the grid and his logo, which is part of the artwork and stays in the TV-safe area
         val gap = (w * 0.0132f).toInt(); val vgap = (h * 0.0234f).toInt()
        val gx = (w * 0.329f).toInt(); val gy = (h * 0.1753f).toInt()
        val stamp = System.currentTimeMillis() / 3_600_000L   // a new picture URL every hour, so a thumbnail changed on YouTube shows up
        var first: View? = null
        gingerVideos.forEachIndexed { i, (id, title, product) ->
            val card = FrameLayout(this).apply { background = fill(0xE60A1430.toInt(), 18, 2, 0xFF2A3A6E.toInt()); clipToOutline = true; outlineProvider = android.view.ViewOutlineProvider.BACKGROUND }
            card.addView(ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setBackgroundColor(0xFF0A1430.toInt()); clipToOutline = true; outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                background = fill(0xFF0A1430.toInt(), 10) }.also {
                Glide.with(this).load("https://i.ytimg.com/vi/$id/maxresdefault.jpg?t=$stamp")
                    .error(Glide.with(this).load("https://i.ytimg.com/vi/$id/hqdefault.jpg?t=$stamp"))   // cached: the address changes every hour, so a changed thumbnail still shows up
                    .transform(com.bumptech.glide.load.resource.bitmap.CenterCrop(), com.bumptech.glide.load.resource.bitmap.RoundedCorners(px(10))).into(it) },
                FrameLayout.LayoutParams(pw, pic).apply { setMargins(inset, inset, 0, 0) })
            card.addView(LinearLayout(this).apply {      // the app's own font, same as the menu and the Settings cards
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(inset + px(4), px(10), inset, 0)   // a smidge lower: a Short's picture is taller, so its title needs more air
                addView(TextView(this@BrowseActivity).apply {
                    text = title; setTextColor(Color.WHITE); typeface = face(700); setTextSize(TypedValue.COMPLEX_UNIT_PX, 30 * u)
                    maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; includeFontPadding = false; setLineSpacing(0f, 0.89f) })
                addView(TextView(this@BrowseActivity).apply {
                    text = product; setTextColor(0xFFE3E8F5.toInt()); typeface = face(500); setTextSize(TypedValue.COMPLEX_UNIT_PX, 22 * u)   // MOVA, LiDAX keep their brand casing
                    maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; includeFontPadding = false; setLineSpacing(0f, 0.89f); setPadding(0, px(3), 0, 0) })   // the product sits right under the title
            }, FrameLayout.LayoutParams(-1, th - inset - pic, Gravity.BOTTOM))
            // The tile: with the remote on it, the same moving colour outline as the Settings panel.
            val tile = FrameLayout(this).apply {
                setOnFocusChangeListener { v, f ->
                    v.animate().scaleX(if (f) 1.04f else 1f).scaleY(if (f) 1.04f else 1f).setDuration(120).start()
                    v.background = if (f) ChaseDrawable(Glow.green, px(18).toFloat(), px(5).toFloat(), 3500L, true) else null
                    v.setPadding(if (f) px(5) else 0, if (f) px(5) else 0, if (f) px(5) else 0, if (f) px(5) else 0)
                    if (f) v.bringToFront()
                }
                setOnClickListener { play("combo", id, pool) }
            }
            focusable(tile, 1f)
            tile.addView(card, FrameLayout.LayoutParams(-1, -1))
            page.addView(tile, FrameLayout.LayoutParams(tw, th).apply { leftMargin = gx + (i % 2) * (tw + gap); topMargin = gy + (i / 2) * (th + vgap) })
            if (first == null) first = tile
        }
        first?.let { f -> page.post { f.requestFocus() } }
        return page
    }

    private fun setQuery(q: String) {
        val t = q.trim()
        // A search that is just a reviewer's name ("just a dad") opens that creator's own page. Brands still search everything, reviews by others included.
        val ch = if (t.isNotEmpty() && eggKey(t) == null) creatorFor(t) else null
        if (ch != null) { query = ""; creator = ch; showList = false; scroll.scrollTo(0, 0); render("res"); return }
        query = t; creator = ""; showList = false; scroll.scrollTo(0, 0); render(if (query.isEmpty()) tabTag() else "res")
    }

    private fun squash(t: String) = t.lowercase().filter { it.isLetterOrDigit() }

    /** True when [a] and [b] are within [max] single-letter edits of each other (voice hears "just a dead" for "just a dad"). */
    private fun near(a: String, b: String, max: Int): Boolean {
        if (kotlin.math.abs(a.length - b.length) > max) return false
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1); cur[0] = i
            for (j in 1..b.length) cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            prev = cur
        }
        return prev[b.length] <= max
    }

    /** The reviewer channel a search names, spaces and capitals ignored, or null. Maker channels are left out on purpose. */
    private fun creatorFor(q: String): String? {
        val k = squash(q); if (k.length < 5) return null
        val chans = (clips ?: emptyList()).filter { !it.maker && it.channel.isNotEmpty() }.map { it.channel }.distinct()
        chans.firstOrNull { squash(it) == k }?.let { return it }
        return chans.firstOrNull { val c = squash(it); c.length >= 6 && near(c, k, 2) }
    }


    /** Back closes a search or My List first; on the start screen it leaves the app. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && goBack()) return true
        return super.onKeyDown(keyCode, event)
    }

    /** One step back: a search, a creator page or My List closes first, then a feed returns to Home. False on Home itself. */
    private fun goBack(): Boolean {
        if (gridTab >= 0 && query.isEmpty() && creator.isEmpty() && !showList) { gridTab = -1; tab = 0; render(tabTag()); return true }
        if (query.isNotEmpty() || creator.isNotEmpty() || showList) { query = ""; creator = ""; showList = false; render(tabTag()); return true }
        return false
    }

    /** True when there is somewhere to go back to, which is when the touch back arrow shows. */
    private fun canGoBack() = gridTab >= 0 || query.isNotEmpty() || creator.isNotEmpty() || showList

    // A tablet's swipe-back gesture and Back button do not send a key: they call this. A TV keeps its own Back key handling untouched.
    private fun listenForSwipeBack() {
        if (MainActivity.isTelevision(this) || android.os.Build.VERSION.SDK_INT < 33) return
        onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, android.window.OnBackInvokedCallback { if (!goBack()) finish() })
    }

    // The creator page sits behind the menu layer, whose scroll view would swallow every touch below the menu: hand those touches to the page.
    private var pageTouch = false
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        val page = if (this::themeLayer.isInitialized) themeLayer.findViewWithTag<View>("gg") else null
        if (page != null) {
            if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) pageTouch = ev.y > header.bottom
            if (pageTouch) { themeLayer.dispatchTouchEvent(ev); if (ev.actionMasked == android.view.MotionEvent.ACTION_UP || ev.actionMasked == android.view.MotionEvent.ACTION_CANCEL) pageTouch = false; return true }
        } else pageTouch = false
        return super.dispatchTouchEvent(ev)
    }

    // ---- data ----------------------------------------------------------------------------------------------------------------

    /** Newest first, at most two from one channel, 14 videos a row. */
    private fun pick(list: List<Clip>): List<Clip> {
        val seen = HashMap<String, Int>(); val out = ArrayList<Clip>()
        for (c in list) {
            val n = (seen[c.channel] ?: 0) + 1; seen[c.channel] = n
            if (n <= 2) out.add(c)
            if (out.size >= 14) break
        }
        return out
    }

    // The page names the current, versioned address of the video list in its data-videos attribute.
    private fun load(): List<Clip> {
        val html = get("https://www.vacubots.com/tv/?tv=1")
        val m = Regex("data-videos=\"([^\"]+)\"").find(html) ?: return emptyList()
        var url = m.groupValues[1].replace("&amp;", "&")
        if (url.startsWith("/")) url = "https://www.vacubots.com$url"
        val json = JSONObject(get(url))
        val out = ArrayList<Clip>()
        val moved = HashMap<String, String>()
        for (id in json.keys()) {
            val v = json.optJSONObject(id) ?: continue
            val o = v.optString("o"); if (o.isNotEmpty()) moved[o] = v.optString("c")   // a channel that was renamed: its old name -> its new name
            val t = v.optString("t"); val f = v.optString("f", "indoor")
            if (t.isEmpty() || f == "off") continue
            out.add(Clip(id, t, v.optString("c"), v.optString("k") == "m", f, stamp(v.optString("d")), v.optInt("s") == 1, v.optString("u")))
        }
        out.sortByDescending { it.stamp }
        renamed = moved
        return out
    }

    /** Channels get proper display names now (Philips Hue, not philips-hue). Follows and hidden brands saved under an old name move to the new one. */
    @Volatile private var renamed: Map<String, String> = emptyMap()
    private fun migrateNames() {
        val map = renamed; if (map.isEmpty()) return
        var f = false; var h = false
        following.toList().forEach { n -> map[n]?.let { following.remove(n); following.add(it); f = true } }
        hidden.toList().forEach { n -> map[n]?.let { hidden.remove(n); hidden.add(it); h = true } }
        if (f) saveFollow(); if (h) saveSettings()
    }

    private fun stamp(d: String): Long {
        val m = Regex("^([A-Za-z]+)\\.?\\s+(\\d+),\\s+(\\d{4})$").find(d) ?: return 0
        val months = mapOf("Jan" to 0, "Feb" to 1, "March" to 2, "April" to 3, "May" to 4, "June" to 5, "July" to 6, "Aug" to 7, "Sept" to 8, "Oct" to 9, "Nov" to 10, "Dec" to 11)
        val mo = months[m.groupValues[1]] ?: return 0
        return java.util.GregorianCalendar(m.groupValues[3].toInt(), mo, m.groupValues[2].toInt()).timeInMillis
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 30000
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) VacuBotsTV")
        try { return c.inputStream.readBytes().toString(Charsets.UTF_8) } finally { c.disconnect() }
    }

    override fun onDestroy() { ui.removeCallbacksAndMessages(null); super.onDestroy() }
}
