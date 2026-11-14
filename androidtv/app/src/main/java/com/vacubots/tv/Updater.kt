package com.vacubots.tv

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Keeps the app up to date by itself: a little while after launch it reads https://vacubots.github.io/vacubots-tv/apk/version.json,
 * and when that names a newer build it downloads the APK and hands it to Android's installer, which shows its normal
 * "Update this app?" prompt. Each build is offered once; if it is declined it is not asked again until a newer one exists.
 * It runs on its own thread and swallows every error, so it can never get in the way of the player.
 */
object Updater {
    private const val BASE = "https://vacubots.github.io/vacubots-tv/apk/"

    /** The little panel shown for a check you asked for: a dark card with the rainbow border, a title, a line of text and a progress bar. */
    private class Panel(private val a: Activity) {
        private val dlg = android.app.Dialog(a, android.R.style.Theme_Material_Dialog_NoActionBar)
        private val title = android.widget.TextView(a)
        private val body = android.widget.TextView(a)
        private val bar = android.widget.ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal)
        private val ui = android.os.Handler(android.os.Looper.getMainLooper())
        private fun dp(v: Int) = (v * a.resources.displayMetrics.widthPixels / 1920f).toInt()
        init {
            val rainbow = intArrayOf(0xFFF2F2F2.toInt(), 0xFFF2E84A.toInt(), 0xFF3FE3E3.toInt(), 0xFF3FD84F.toInt(), 0xFFE24FE2.toInt(), 0xFFF24A45.toInt(), 0xFF5B6CFF.toInt(), 0xFFF2F2F2.toInt())
            val inner = android.graphics.drawable.GradientDrawable().apply { setColor(0xFF11141A.toInt()); cornerRadius = dp(22).toFloat() }
            val col = android.widget.LinearLayout(a).apply {
                orientation = android.widget.LinearLayout.VERTICAL; setPadding(dp(56), dp(40), dp(56), dp(40))
                background = android.graphics.drawable.LayerDrawable(arrayOf(ChaseDrawable(Glow.current, dp(26).toFloat(), dp(5).toFloat(), 9000L), inner)).also { it.setLayerInset(1, dp(5), dp(5), dp(5), dp(5)) }
            }
            title.apply { setTextColor(0xFFFFFFFF.toInt()); setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(36).toFloat()); typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD) }
            body.apply { setTextColor(0xFFC4C9D0.toInt()); setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, dp(26).toFloat()); setPadding(0, dp(10), 0, dp(18)) }
            bar.apply { max = 100; progressDrawable.setColorFilter(0xFF3FE3E3.toInt(), android.graphics.PorterDuff.Mode.SRC_IN); indeterminateDrawable.setColorFilter(0xFF3FE3E3.toInt(), android.graphics.PorterDuff.Mode.SRC_IN) }
            col.addView(title); col.addView(body); col.addView(bar, android.widget.LinearLayout.LayoutParams(-1, dp(10)))
            dlg.setContentView(col)
            dlg.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0x99000000.toInt()))
            dlg.window?.setLayout(dp(980), -2)
        }
        /** progress < 0 spins; 0..100 fills the bar; done shows no bar and closes by itself a moment later. */
        fun show(head: String, text: String, progress: Int = -1, done: Boolean = false) = a.runOnUiThread {
            if (a.isFinishing) return@runOnUiThread
            title.text = head; body.text = text
            bar.visibility = if (done) android.view.View.GONE else android.view.View.VISIBLE
            bar.isIndeterminate = progress < 0; if (progress >= 0) bar.progress = progress
            if (!dlg.isShowing) dlg.show()
            ui.removeCallbacksAndMessages(null)
            if (done) ui.postDelayed({ try { dlg.dismiss() } catch (e: Exception) {} }, 2600)
        }
        fun close() = a.runOnUiThread { ui.removeCallbacksAndMessages(null); try { dlg.dismiss() } catch (e: Exception) {} }
    }

    fun check(activity: Activity, manual: Boolean = false) {
        val panel = if (manual) Panel(activity) else null
        fun say(head: String, msg: String, progress: Int = -1, done: Boolean = false) { panel?.show(head, msg, progress, done) }
        say("Checking for Updates", "Looking for a newer version of VacuBots TV…")
        Thread {
            try {
                val stamp = System.currentTimeMillis()
                @Suppress("DEPRECATION")
                val mine = activity.packageManager.getPackageInfo(activity.packageName, 0).versionCode
                // The site keeps serving the first copy it saw of version.json, so look for the next build's own file first (v<next>.json
                // is new every release), then fall back to version.json.
                // Look a few builds ahead too (newest first), so a release that skipped numbers is still found.
                var found: JSONObject? = null
                for (n in 8 downTo 1) {
                    found = try { JSONObject(get(BASE + "v${mine + n}.json?t=$stamp").toString(Charsets.UTF_8)) } catch (e: Exception) { null }
                    if (found != null) break
                }
                val info = found ?: JSONObject(get(BASE + "version.json?t=$stamp").toString(Charsets.UTF_8))
                val code = info.getInt("code")
                val prefs = activity.getSharedPreferences("update", Activity.MODE_PRIVATE)
                if (code <= mine) { say("You\u2019re Up to Date", "You have the latest version of VacuBots TV.", done = true); return@Thread }
                if (!manual && prefs.getInt("offered", 0) >= code) return@Thread

                if (!activity.packageManager.canRequestPackageInstalls()) {
                    // Android asks once, on its own screen: allow VacuBots TV to install apps. It is offered again next launch.
                    activity.runOnUiThread {
                        activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.packageName)))
                    }
                    panel?.close()
                    return@Thread
                }
                val label = "VacuBots TV " + info.optString("name") + " (" + code + ")"
                say("Downloading Update", label, 0)
                val bytes = get(BASE + info.optString("file", "VacuBotsTV.apk") + "?t=$stamp") { pct -> say("Downloading Update", "$label  \u2022  $pct%", pct) }
                if (bytes.size < 500_000) { say("Update Not Ready", "The file didn\u2019t download properly. Try again in a minute.", done = true); return@Thread }   // not an APK (an error page, say)
                val dir = File(activity.cacheDir, "update").apply { mkdirs() }
                val apk = File(dir, "VacuBotsTV.apk")
                apk.writeBytes(bytes)
                val uri = FileProvider.getUriForFile(activity, activity.packageName + ".update", apk)
                prefs.edit().putInt("offered", code).apply()
                say("Ready to Install", "Android will ask you to confirm the update.", 100)
                activity.runOnUiThread {
                    panel?.close()
                    activity.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            } catch (e: Exception) {
                say("Couldn\u2019t Check for Updates", "Try again in a minute.", done = true)
            }
        }.start()
    }

    private fun get(url: String, progress: ((Int) -> Unit)? = null): ByteArray {
        val c = URL(url).openConnection() as HttpURLConnection
        c.useCaches = false; c.connectTimeout = 15000; c.readTimeout = 60000
        try {
            if (progress == null) return c.inputStream.readBytes()
            val total = c.contentLength.toLong(); val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(64 * 1024); var got = 0L; var last = -1
            c.inputStream.use { s ->
                while (true) {
                    val n = s.read(buf); if (n < 0) break
                    out.write(buf, 0, n); got += n
                    if (total > 0) { val pct = (got * 100 / total).toInt(); if (pct != last) { last = pct; progress(pct) } }
                }
            }
            return out.toByteArray()
        } finally { c.disconnect() }
    }
}
