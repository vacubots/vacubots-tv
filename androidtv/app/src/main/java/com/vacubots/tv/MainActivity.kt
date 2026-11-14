package com.vacubots.tv

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import android.app.Activity

/**
 * VacuBots TV for Google TV: a full-screen WebView on https://www.vacubots.com/tv/?tv=1.
 * The page does the work (YouTube's own embedded player, so creators keep their views); this app keeps the
 * screen on, goes full screen, tells the page it is on a TV, and turns remote keys into the page's keys.
 */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private lateinit var note: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var loaded = false
    // A TV (remote, no touch) gets the page in TV mode; a phone or tablet gets the normal page with its touch buttons.
    private val isTv by lazy { isTelevision(this) }
    // The native start screen can ask for a particular page (a feed to play); otherwise the start page.
    private val startUrl get() = intent?.getStringExtra(EXTRA_URL) ?: if (isTv) START_URL else START_URL_TOUCH

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
        requestedOrientation = if (isTv) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()) }
        web = WebView(this).apply {
            setBackgroundColor(0xFF000000.toInt())
            isFocusable = true
            isFocusableInTouchMode = true
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false   // nobody can tap Start on a TV
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                // Look like desktop Chrome: a phone-style browser makes YouTube's embed show the Shorts screen (logo, red bar)
                // and loop Shorts. "VacuBotsTV" in the user agent is how the page knows to use its TV mode.
                // The Chrome number is the real engine's (it used to say 124 while the engine's own headers said 156, and YouTube's
                // player sometimes stalled on the mismatch: "error 2", then stuck unstarted).
                val engine = Regex("Chrome/[\\d.]+").find(WebSettings.getDefaultUserAgent(this@MainActivity))?.value ?: "Chrome/124.0.0.0"
                userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) $engine Safari/537.36" + (if (isTv) " VacuBotsTV/" else " VacuBotsApp/") + appVersion()
                // A phone or tablet WebView also tells YouTube "mobile" in its client hints (Sec-CH-UA-Mobile), whatever the user agent says, and YouTube's
                // embed then shows its phone Shorts screen (the red progress bar) and fails on Shorts. Say desktop Linux instead, as the TV's WebView does.
                if (!isTv) try {
                    if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.USER_AGENT_METADATA))
                        androidx.webkit.WebSettingsCompat.setUserAgentMetadata(this, androidx.webkit.UserAgentMetadata.Builder().setMobile(false).setPlatform("Linux").setModel("").build())
                } catch (e: Throwable) { /* an older WebView keeps its own hints */ }
            }
            // YouTube's player can also tell a touch screen from its own scripts and then serves its phone Shorts player, which fails inside our page (error 2).
            // The TV has no touch screen, so it never does. On a tablet or phone, tell the YouTube frames (and only those) that there is no touch.
            if (!isTv) try {
                if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.DOCUMENT_START_SCRIPT))
                    androidx.webkit.WebViewCompat.addDocumentStartJavaScript(this, NO_TOUCH_JS, setOf("https://www.youtube.com", "https://www.youtube-nocookie.com"))
            } catch (e: Throwable) { /* an older WebView: nothing changes */ }
            // Android draws its own small gray play icon while a video loads: give it a plain black picture instead.
            // (1.2 also replaced the loading spinner view; that is left alone now, in case it is what upset the player.)
            webChromeClient = object : WebChromeClient() {
                override fun getDefaultVideoPoster(): Bitmap? =
                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
            }
            // The page's own stand-by card shows well before the page counts as finished; the page tells the app when it is up,
            // and the app's note fades away (the player's own callbacks are left exactly as in 1.4).
            addJavascriptInterface(object { @android.webkit.JavascriptInterface fun ready() { handler.post { fadeNote() } } }, "VBApp")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) { loaded = true; fadeNote() }
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) retryLater()
                }
                // Keep everything on vacubots.com / YouTube inside the app; nothing else can be opened on a TV.
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val host = request?.url?.host ?: return true
                    // The exact site or a subdomain of it ("evilvacubots.com" must not pass), and only over https.
                    val ok = listOf("vacubots.com", "youtube.com", "ytimg.com", "google.com")
                        .any { host == it || host.endsWith(".$it") }
                    return !(ok && request.url.scheme == "https")
                }
            }
        }
        note = TextView(this).apply {
            setTextColor(0xFFAAAEB6.toInt()); textSize = 28f
            // From the start screen the page's own stand-by card follows right away, so no second "tuning in" message here.
            text = if (intent?.hasExtra(EXTRA_URL) == true) "" else "VacuBots TV is tuning in…"
            gravity = android.view.Gravity.CENTER
        }
        root.addView(web, FrameLayout.LayoutParams(-1, -1))
        root.addView(note, FrameLayout.LayoutParams(-1, -1))
        if (!isTv && intent?.hasExtra(EXTRA_URL) == true) {
            val size = (resources.displayMetrics.density * 48).toInt()
            closeBtn = NeonButton(this, true, Glow.current, size).apply {
                alpha = 0f; visibility = View.GONE
                setOnClickListener { finish() }
            }
            val m = (resources.displayMetrics.density * 16).toInt()
            root.addView(closeBtn, FrameLayout.LayoutParams(size, size, android.view.Gravity.TOP or android.view.Gravity.END).apply { topMargin = m; rightMargin = m })
            web.setOnTouchListener { _, ev -> if (ev.actionMasked == android.view.MotionEvent.ACTION_UP) showClose(); false }   // false: the page still gets the tap
        }
        listenForSwipeBack()
        setContentView(root)
        goFullScreen()
        web.requestFocus()
        web.loadUrl(startUrl)
        // A while after launch, so the player is never held up: look for a newer build of this app.
        if (!isTv) handler.postDelayed({ Updater.check(this) }, 20000)   // on a TV the native start screen checks
    }

    private fun fadeNote() {
        if (note.visibility != View.VISIBLE || note.alpha < 1f) return
        note.animate().alpha(0f).setDuration(450).setInterpolator(AccelerateInterpolator()).withEndAction { note.visibility = View.GONE }.start()
    }

    private fun retryLater() {
        note.animate().cancel()
        note.alpha = 1f
        note.visibility = View.VISIBLE
        note.text = "No connection. Trying again…"
        handler.postDelayed({ if (!loaded) web.loadUrl(startUrl) }, 8000)
    }

    private fun goFullScreen() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goFullScreen()
    }

    override fun finish() { super.finish(); @Suppress("DEPRECATION") overridePendingTransition(0, 0) }

    override fun onResume() { super.onResume(); web.onResume(); web.resumeTimers() }
    override fun onPause() { web.onPause(); super.onPause() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); web.destroy(); super.onDestroy() }

    /** Remote keys become the page's keys (the page listens for these names). Back opens the feed menu, then goes to the start screen, then leaves the app. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val key = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> "ArrowLeft"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "ArrowRight"
            KeyEvent.KEYCODE_DPAD_UP -> "ArrowUp"
            KeyEvent.KEYCODE_DPAD_DOWN -> "ArrowDown"
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> "Enter"
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "MediaPlayPause"
            KeyEvent.KEYCODE_MEDIA_PLAY -> "MediaPlay"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "MediaPause"
            KeyEvent.KEYCODE_MEDIA_NEXT -> "MediaTrackNext"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "MediaTrackPrevious"
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "MediaFastForward"
            KeyEvent.KEYCODE_MEDIA_REWIND -> "MediaRewind"
            KeyEvent.KEYCODE_CHANNEL_UP -> "ChannelUp"
            KeyEvent.KEYCODE_CHANNEL_DOWN -> "ChannelDown"
            else -> null
        }
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            // In the player, Back opens the page's feed menu, and a second Back goes to the start screen (the feeds). On the start
            // screen, Back leaves the app.
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) goBack()
            return true
        }
        if (key != null) {
            // A held key is sent again only for the seek keys, so holding Left or Right keeps rewinding or fast-forwarding; every other key counts once per press.
            val seekKey = key == "ArrowLeft" || key == "ArrowRight" || key == "MediaFastForward" || key == "MediaRewind"
            if (event.action == KeyEvent.ACTION_DOWN && (event.repeatCount == 0 || seekKey)) {
                web.evaluateJavascript("document.dispatchEvent(new KeyboardEvent('keydown',{key:'$key',bubbles:true,cancelable:true}))", null)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /** One step back: the page's feed menu first, then the start screen, then out of the app. */
    private fun goBack() {
        web.evaluateJavascript("(function(){return !!(window.__vbBack && window.__vbBack());})()") { r ->
            if (r != "true") {
                if (isTv || intent?.hasExtra(EXTRA_URL) == true) finish() else if ((web.url ?: "").contains("/tv-watch")) web.loadUrl(START_URL_TOUCH) else finish()
            }
        }
    }

    // A tablet's swipe-back gesture and Back button call this instead of sending a key. A TV keeps its own Back key handling untouched.
    private fun listenForSwipeBack() {
        if (isTv || android.os.Build.VERSION.SDK_INT < 33) return
        onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, android.window.OnBackInvokedCallback { goBack() })
    }

    // Opened from the tablet's native screens, the player is full screen with nothing to tap: one tap on the video shows a neon close
    // button in the top corner for 3 seconds. It sits over the page's own corner and never takes the video's touches.
    private var closeBtn: NeonButton? = null
    private val hideClose = Runnable { closeBtn?.animate()?.alpha(0f)?.setDuration(250)?.withEndAction { closeBtn?.visibility = View.GONE }?.start() }
    private fun showClose() {
        val b = closeBtn ?: return
        handler.removeCallbacks(hideClose)
        b.animate().cancel(); b.visibility = View.VISIBLE; b.animate().alpha(1f).setDuration(150).start()
        handler.postDelayed(hideClose, 3000)
    }

    // The app's real version (build.gradle.kts versionName), told to the page in the user agent so it can show which app this is.
    private fun appVersion(): String = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "?" } catch (e: Exception) { "?" }

    companion object {
        const val START_URL = "https://www.vacubots.com/tv/?tv=1"
        const val START_URL_TOUCH = "https://www.vacubots.com/tv/"
        const val EXTRA_URL = "url"

        /** Run first in YouTube's own frames: no touch events, no touch points, no "coarse pointer" or "no hover" media queries. */
        private const val NO_TOUCH_JS = """(function(){try{
  try{Object.defineProperty(Navigator.prototype,'maxTouchPoints',{get:function(){return 0},configurable:true});}catch(e){}
  ['ontouchstart','ontouchend','ontouchmove','ontouchcancel','TouchEvent','Touch','TouchList'].forEach(function(k){var o=window;while(o){try{delete o[k];}catch(e){}o=Object.getPrototypeOf(o);}});
  var mm=window.matchMedia;
  window.matchMedia=function(q){
    var r=mm.call(window,q);
    if(/(any-)?pointer:\s*coarse|(any-)?hover:\s*none/.test(String(q))){
      return {matches:false,media:String(q),onchange:null,addListener:function(){},removeListener:function(){},addEventListener:function(){},removeEventListener:function(){},dispatchEvent:function(){return false}};
    }
    return r;
  };
}catch(e){}})();"""

        /** A tablet (at least 600 dp wide, not a TV) gets the same native screens, driven by touch. */
        fun isTablet(c: android.content.Context): Boolean = !isTelevision(c) && c.resources.configuration.smallestScreenWidthDp >= 600

        /** A TV (remote, no touch) gets the native start screen; a phone or tablet gets the normal page with its touch buttons. */
        fun isTelevision(c: android.content.Context): Boolean =
            (c.getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager).currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION ||
                c.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
    }
}
