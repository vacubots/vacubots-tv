package com.vacubots.tv

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Quiet background sound for the wallpaper themes, played from real recordings downloaded from https://vacubots.github.io/vacubots-tv/sounds/
 * (manifest.json lists them; see pages/sounds/README.md), so sounds can be swapped without an app release. A theme with no
 * recording is silent. One thread loops the recording through a streaming AudioTrack; starting a sound fades it in, stopping fades it out.
 */
object Ambience {
    private val names = arrayOf("meadow", "snowfield")
    const val MEADOW = 0; const val SNOWFIELD = 1

    /** Which recording goes with which theme (themes not listed are silent). Add a theme here when its recording is added. */
    val forTheme = mapOf("meadow" to MEADOW, "pinksnow" to SNOWFIELD)

    val levelNames = listOf("Off", "Low", "Medium", "High")
    private val levelGain = floatArrayOf(0f, 0.10f, 0.22f, 0.45f)

    private const val RATE = 22050
    private const val BASE = "https://vacubots.github.io/vacubots-tv/sounds/"
    /** Decoded recordings by kind (mono, 22050 Hz, already blended so the loop has no seam). */
    private val clips = java.util.concurrent.ConcurrentHashMap<Int, ShortArray>()
    @Volatile private var syncing = false

    /** Loads the recordings kept on the TV, then looks for new or changed ones on the site. Safe to call often; does its work in the background. */
    fun sync(ctx: android.content.Context, onReady: () -> Unit = {}) {
        if (syncing) return
        syncing = true
        val dir = File(ctx.filesDir, "sounds").apply { mkdirs() }
        Thread {
            try {
                for (k in names.indices) loadFile(File(dir, names[k] + ".ogg"))?.let { clips[k] = it }
                onReady()
                val man = JSONObject(String(fetch(BASE + "manifest.json?t=" + System.currentTimeMillis()), Charsets.UTF_8))
                val have = File(dir, "versions.json").let { f -> try { JSONObject(f.readText()) } catch (e: Exception) { JSONObject() } }
                for (k in names.indices) {
                    val e = man.optJSONObject(names[k]) ?: continue
                    val v = e.optInt("v", 1)
                    if (have.optInt(names[k], 0) == v && clips.containsKey(k)) continue
                    try {
                        val bytes = fetch(BASE + e.getString("file") + "?v=" + v)
                        val tmp = File(dir, names[k] + ".new").apply { writeBytes(bytes) }
                        val pcm = loadFile(tmp)
                        if (pcm == null) { tmp.delete(); continue }
                        tmp.renameTo(File(dir, names[k] + ".ogg"))
                        clips[k] = pcm; have.put(names[k], v); onReady()
                        File(dir, "versions.json").writeText(have.toString())
                    } catch (e2: Exception) { }
                }
            } catch (e: Exception) { }
            syncing = false
        }.start()
    }

    private fun fetch(url: String): ByteArray {
        val c = URL(url).openConnection() as HttpURLConnection
        c.useCaches = false; c.connectTimeout = 15000; c.readTimeout = 60000
        return c.inputStream.use { it.readBytes() }
    }

    /** Decodes a file to mono 16-bit PCM at [RATE]; null when it is missing, unreadable or not in that format. */
    private fun loadFile(f: File): ShortArray? {
        if (!f.exists()) return null
        val ex = android.media.MediaExtractor()
        var codec: android.media.MediaCodec? = null
        try {
            ex.setDataSource(f.absolutePath)
            val fmt = ex.getTrackFormat(0)
            if (fmt.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE) != RATE || fmt.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT) != 1) return null
            ex.selectTrack(0)
            codec = android.media.MediaCodec.createDecoderByType(fmt.getString(android.media.MediaFormat.KEY_MIME)!!)
            codec.configure(fmt, null, null, 0); codec.start()
            val out = java.io.ByteArrayOutputStream(); val info = android.media.MediaCodec.BufferInfo()
            var inDone = false; var outDone = false
            while (!outDone) {
                if (!inDone) {
                    val i = codec.dequeueInputBuffer(10000)
                    if (i >= 0) {
                        val n = ex.readSampleData(codec.getInputBuffer(i)!!, 0)
                        if (n < 0) { codec.queueInputBuffer(i, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM); inDone = true }
                        else { codec.queueInputBuffer(i, 0, n, ex.sampleTime, 0); ex.advance() }
                    }
                }
                val o = codec.dequeueOutputBuffer(info, 10000)
                if (o >= 0) {
                    val b = codec.getOutputBuffer(o)!!; val chunk = ByteArray(info.size); b.get(chunk); out.write(chunk)
                    codec.releaseOutputBuffer(o, false)
                    if (info.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outDone = true
                }
            }
            val bb = java.nio.ByteBuffer.wrap(out.toByteArray()).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val pcm = ShortArray(bb.remaining()); bb.get(pcm)
            if (pcm.size < RATE * 5) return null
            // Blend the last two seconds into the first two so the loop has no seam.
            val xf = minOf(RATE * 2, pcm.size / 4); val n = pcm.size - xf
            val loop = ShortArray(n)
            for (i in 0 until n) loop[i] = pcm[i]
            for (i in 0 until xf) {
                val a = i / xf.toFloat()
                loop[i] = (pcm[i] * a + pcm[n + i] * (1f - a)).toInt().toShort()
            }
            return loop
        } catch (e: Exception) { return null }
        finally { try { codec?.stop(); codec?.release() } catch (e: Exception) { }; try { ex.release() } catch (e: Exception) { } }
    }

    @Volatile private var thread: Thread? = null
    @Volatile private var wantKind = -1
    @Volatile private var wantGain = 0f

    /** Plays the sound for [kind] at [level] (0 is off), or fades to silence when kind is -1. */
    @Synchronized fun set(kind: Int, level: Int) {
        wantKind = if (level <= 0 || !clips.containsKey(kind)) -1 else kind
        wantGain = levelGain[level.coerceIn(0, 3)]
        if (wantKind >= 0 && thread == null) {
            val t = Thread { run() }.apply { isDaemon = true; name = "ambience" }
            thread = t; t.start()
        }
    }

    fun stop() = set(-1, 0)

    private fun run() {
        val rate = RATE
        val buf = ShortArray(1024)
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(min, buf.size * 4)).build()
        } catch (e: Exception) { thread = null; return }
        track.play()
        var kind = -1; var gain = 0f; var idle = 0; var pos = 0
        while (true) {
            val k = wantKind
            if (k != kind) {                       // fade out the old sound, then switch to the new one
                gain -= 0.02f
                if (gain <= 0f) { gain = 0f; kind = k; pos = 0 }
            } else if (gain < wantGain) gain = minOf(wantGain, gain + 0.01f)
            else if (gain > wantGain) gain = maxOf(wantGain, gain - 0.01f)
            if (kind < 0 && gain == 0f) { if (++idle > 40) break }   // silent for a while: end the thread
            else idle = 0
            val clip = if (kind >= 0) clips[kind] else null
            if (clip != null) { for (i in buf.indices) { buf[i] = (clip[pos] * gain).toInt().toShort(); if (++pos >= clip.size) pos = 0 } }
            else java.util.Arrays.fill(buf, 0.toShort())
            track.write(buf, 0, buf.size)
        }
        try { track.stop(); track.release() } catch (e: Exception) { }
        thread = null
    }
}
