package com.vacubots.tv

import android.content.Context
import android.view.Choreographer
import android.view.View
import java.lang.ref.WeakReference

/** Something that redraws on the shared effects clock. */
interface Tickable { fun tick() }

/**
 * One clock for every moving effect (the wallpaper layers, the animated outlines and buttons). Before, each of them asked for its own next
 * frame, some at the screen's rate and some at 25 or 30 a second, so a theme with four layers made the screen draw four separate times
 * per frame. Now they all redraw together, at 30 a second, which is smooth for slow ambient motion and about half the work for a TV's
 * graphics chip. Focus changes and scrolling are not on this clock; they still run at the screen's full rate.
 *
 * The clock only runs while something is listening. Listeners are held weakly, so a screen that is thrown away never keeps one alive.
 */
object Effects {
    /** True while a dialog or another window has the focus: the full-screen wallpaper layers stand still underneath it, which saves most of the work. */
    @Volatile var pauseWallpapers = false

    private val subs = ArrayList<WeakReference<Tickable>>()
    private var scheduled = false
    private var last = 0L
    private const val STEP_NS = 25_000_000L   // between one 60 Hz frame (16.7 ms) and two (33.3 ms): draws on every second frame, 30 a second

    private val frame = Choreographer.FrameCallback { now ->
        scheduled = false
        if (subs.isEmpty()) return@FrameCallback
        if (now - last >= STEP_NS) {
            last = now
            var i = 0
            while (i < subs.size) {
                val t = subs[i].get()
                if (t == null) subs.removeAt(i) else { t.tick(); i++ }
            }
        }
        schedule()
    }

    private fun schedule() {
        if (scheduled || subs.isEmpty()) return
        scheduled = true
        Choreographer.getInstance().postFrameCallback(frame)
    }

    fun add(t: Tickable) {
        if (subs.none { it.get() === t }) subs.add(WeakReference(t))
        schedule()
    }

    fun remove(t: Tickable) { subs.removeAll { val x = it.get(); x == null || x === t } }
}

/** A full-screen moving layer (snow, stars, haze ...): redraws on the shared clock while it is on screen, and stands still while a dialog has the focus. */
open class EffectView(ctx: Context) : View(ctx), Tickable {
    override fun onAttachedToWindow() { super.onAttachedToWindow(); Effects.add(this) }
    override fun onDetachedFromWindow() { Effects.remove(this); super.onDetachedFromWindow() }
    override fun tick() { if (!Effects.pauseWallpapers) invalidate() }
}
