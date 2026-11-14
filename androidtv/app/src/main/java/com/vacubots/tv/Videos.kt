package com.vacubots.tv

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.View
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Looping video wallpapers. A theme with a video downloaded from https://vacubots.github.io/vacubots-tv/videos/ (manifest.json lists them; see
 * pages/videos/README.md) plays it silently behind the interface. With no video (not downloaded yet, or the TV is offline)
 * the theme keeps its still picture and moving effects, so nothing is ever missing.
 */
object Videos {
    private const val BASE = "https://vacubots.github.io/vacubots-tv/videos/"
    @Volatile private var syncing = false

    private fun dir(ctx: Context) = File(ctx.filesDir, "videos").apply { mkdirs() }

    /** The downloaded video for a theme, or null. */
    fun file(ctx: Context, id: String): File? = File(dir(ctx), "$id.mp4").takeIf { it.exists() && it.length() > 100_000 }

    /** Looks for new or changed videos on the site and downloads them in the background; [onNew] runs (on any thread) after one has arrived. */
    fun sync(ctx: Context, onNew: () -> Unit) {
        if (syncing) return
        syncing = true
        val d = dir(ctx)
        Thread {
            try {
                val man = JSONObject(String(get(BASE + "manifest.json?t=" + System.currentTimeMillis()), Charsets.UTF_8))
                val vf = File(d, "versions.json")
                val have = try { JSONObject(vf.readText()) } catch (e: Exception) { JSONObject() }
                var got = false
                for (id in man.keys()) {
                    val e = man.optJSONObject(id) ?: continue
                    val small = ctx.resources.displayMetrics.widthPixels < 2560 && e.has("file1080")
                    val v = if (small) e.optInt("v1080", e.optInt("v", 1)) else e.optInt("v", 1)   // the 1080p copy has its own version
                    if (have.optInt(id, 0) == v && File(d, "$id.mp4").exists()) continue
                    try {
                        // A TV that gives apps a 1080p screen would decode a 4K video only to shrink it, so it gets the 1080p copy (a third of the size, and
                        // within every TV chip's hardware decoder); a screen that is really 4K wide gets the 4K one.
                        val name = if (small) e.getString("file1080") else e.getString("file")
                        val tmp = File(d, "$id.part").apply { writeBytes(get(BASE + name + "?v=" + v)) }
                        if (tmp.length() < 100_000) { tmp.delete(); continue }
                        File(d, "$id.mp4").delete(); tmp.renameTo(File(d, "$id.mp4"))
                        have.put(id, v); vf.writeText(have.toString()); got = true
                    } catch (e2: Exception) { }
                }
                if (got) onNew()
            } catch (e: Exception) { }
            syncing = false
        }.start()
    }

    private fun get(url: String): ByteArray {
        val c = URL(url).openConnection() as HttpURLConnection
        c.useCaches = false; c.connectTimeout = 15000; c.readTimeout = 120000
        return c.inputStream.use { it.readBytes() }
    }

    /** A silent, looping, full-screen video. If it cannot play it stays transparent, so the still picture under it shows. */
    class LoopVideo(ctx: Context, private val file: File) : TextureView(ctx), TextureView.SurfaceTextureListener, Tickable {
        private var player: MediaPlayer? = null
        private var held = false   // paused because a dialog has the focus
        init { surfaceTextureListener = this; isOpaque = false }

        // While a dialog has the focus the picture behind it stands still (the decoder rests); it carries on when the dialog closes.
        override fun onAttachedToWindow() { super.onAttachedToWindow(); Effects.add(this) }
        override fun onDetachedFromWindow() { Effects.remove(this); super.onDetachedFromWindow() }
        override fun tick() {
            val p = Effects.pauseWallpapers
            if (p == held) return
            held = p
            try { player?.let { if (p) it.pause() else if (windowVisibility == View.VISIBLE) it.start() } } catch (e: Exception) { }
        }

        private var texture: SurfaceTexture? = null

        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) { texture = st; startPlayer() }

        private fun startPlayer() {
            val st = texture ?: return
            if (player != null) return
            try {
                val p = MediaPlayer()
                p.setDataSource(file.absolutePath)
                p.setSurface(Surface(st))
                p.isLooping = true
                p.setVolume(0f, 0f)
                p.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                p.setOnErrorListener { _, _, _ -> release(); true }
                p.setOnPreparedListener { if (windowVisibility == View.VISIBLE && !held) it.start() }
                p.prepareAsync()
                player = p
            } catch (e: Exception) { release() }
        }

        /** Lets go of the video decoder (another screen is playing a video) without removing the view, so nothing flashes on return. */
        fun suspendPlayback() = release()
        fun resumePlayback() = startPlayer()

        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { release(); texture = null; return true }

        override fun onWindowVisibilityChanged(visibility: Int) {
            super.onWindowVisibilityChanged(visibility)
            try { player?.let { if (visibility == View.VISIBLE) it.start() else it.pause() } } catch (e: Exception) { }
        }

        private fun release() { try { player?.release() } catch (e: Exception) { }; player = null }
    }
}
