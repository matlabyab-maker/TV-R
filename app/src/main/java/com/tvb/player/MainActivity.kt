package com.tvb.player

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var urlInput: EditText
    private lateinit var status: TextView
    private lateinit var webView: WebView
    private lateinit var playlistBox: LinearLayout
    private lateinit var content: LinearLayout
    private var activeMode = "direct"

    private val openPlaylist = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) loadPlaylist(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(0xFF101820.toInt())
        }
        val title = TextView(this).apply {
            text = "TV R  •  گیرنده جامع تلویزیون اینترنتی"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 4, 0, 12)
        }
        val modes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("پخش مستقیم" to "direct", "تلویزیون وب" to "web", "فهرست شبکه‌ها" to "list").forEach { (label, mode) ->
            modes.addView(Button(this).apply {
                text = label
                setOnClickListener { showMode(mode) }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        urlInput = EditText(this).apply {
            hint = "آدرس جریان، صفحه تلویزیون یا لینک IPTV"
            setSingleLine(true)
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFFB0BEC5.toInt())
            setSelectAllOnFocus(false)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this).apply {
            text = "تشخیص و اجرا"
            setOnClickListener { resolveAndOpen() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(Button(this).apply {
            text = "بازکردن فایل M3U"
            setOnClickListener { openPlaylist.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/plain", "*/*")) }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        playerView = PlayerView(this).apply {
            useController = true
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        webView = createWebView()
        playlistBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        status = TextView(this).apply {
            text = "آماده دریافت شبکه"
            textSize = 14f
            setTextColor(0xFFFFC400.toInt())
            setPadding(4, 8, 4, 8)
        }
        page.addView(title)
        page.addView(modes)
        page.addView(urlInput, LinearLayout.LayoutParams(-1, -2))
        page.addView(actions)
        page.addView(content)
        page.addView(status)
        setContentView(page)
        showMode("direct")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView = WebView(this).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.loadsImagesAutomatically = true
        settings.allowContentAccess = true
        settings.javaScriptCanOpenWindowsAutomatically = false
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                status.text = "صفحه وب بارگذاری شد؛ اگر پخش‌کننده سایت سازگار باشد، پخش را از داخل صفحه آغاز کنید."
            }
        }
        webChromeClient = WebChromeClient()
    }

    private fun showMode(mode: String) {
        activeMode = mode
        content.removeAllViews()
        when (mode) {
            "direct" -> {
                content.addView(playerView, LinearLayout.LayoutParams(-1, -1))
                status.text = "پخش مستقیم: HLS، DASH، RTSP، SmoothStreaming و فایل‌های مستقیم سازگار"
            }
            "web" -> {
                content.addView(webView, LinearLayout.LayoutParams(-1, -1))
                status.text = "تلویزیون وب: صفحه را وارد کنید؛ پخش به سازگاری و مجوزهای خود سایت وابسته است."
            }
            else -> {
                val scroll = ScrollView(this)
                scroll.addView(playlistBox)
                content.addView(scroll, LinearLayout.LayoutParams(-1, -1))
                status.text = "فهرست شبکه‌ها: فایل M3U یا M3U8 را باز کنید."
            }
        }
    }

    private fun resolveAndOpen() {
        val raw = urlInput.text.toString().trim()
        if (raw.isBlank()) {
            toast("ابتدا آدرس را وارد کنید")
            return
        }
        val uri = runCatching { Uri.parse(raw) }.getOrNull()
        val scheme = uri?.scheme?.lowercase().orEmpty()
        if (scheme !in listOf("http", "https", "rtsp")) {
            toast("آدرس باید با http، https یا rtsp شروع شود")
            return
        }
        val path = uri?.lastPathSegment?.lowercase().orEmpty()
        val isWebPage = path.isEmpty() || (!path.contains(".") && !path.contains("m3u"))
        val isPlaylist = path.endsWith(".m3u") || path.endsWith(".m3u8") && raw.contains("get.php").not()
        if (activeMode == "web" || (isWebPage && scheme != "rtsp")) {
            showMode("web")
            webView.loadUrl(raw)
            status.text = "در حال بازکردن صفحه تلویزیون..."
        } else if (isPlaylist && activeMode == "list") {
            webView.loadUrl(raw)
        } else {
            showMode("direct")
            playStream(raw)
        }
    }

    private fun playStream(url: String) {
        try {
            if (player == null) {
                player = ExoPlayer.Builder(this).build().also { exo ->
                    playerView.player = exo
                    exo.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            status.text = when (state) {
                                Player.STATE_BUFFERING -> "در حال دریافت جریان..."
                                Player.STATE_READY -> "در حال پخش"
                                Player.STATE_ENDED -> "پخش پایان یافت"
                                else -> "آماده پخش"
                            }
                        }
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            status.text = "خطای پخش: ${error.message ?: "منبع یا قالب پشتیبانی نمی‌شود"}"
                        }
                    })
                }
            }
            val item = MediaItem.Builder().setUri(url).apply {
                val lower = url.substringBefore('?').lowercase()
                when {
                    lower.endsWith(".m3u8") -> setMimeType(MimeTypes.APPLICATION_M3U8)
                    lower.endsWith(".mpd") -> setMimeType(MimeTypes.APPLICATION_MPD)
                    lower.endsWith(".ism") || lower.endsWith(".isml") -> setMimeType(MimeTypes.APPLICATION_SS)
                }
            }.build()
            player?.setMediaItem(item)
            player?.prepare()
            player?.play()
            status.text = "در حال اتصال..."
        } catch (e: Exception) {
            status.text = "خطا: ${e.message ?: "نامشخص"}"
        }
    }

    private fun loadPlaylist(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("فایل قابل خواندن نیست")
            playlistBox.removeAllViews()
            var pendingChannelName = "شبکه بدون نام"
            var count = 0
            for (rawLine in text.lineSequence()) {
                val line = rawLine.trim()
                if (line.startsWith("#EXTINF", ignoreCase = true)) {
                    pendingChannelName = line.substringAfter(',', "شبکه بدون نام")
                        .trim()
                        .ifBlank { "شبکه بدون نام" }
                } else if (
                    line.isNotBlank() &&
                    !line.startsWith("#") &&
                    (line.startsWith("http://", ignoreCase = true) ||
                     line.startsWith("https://", ignoreCase = true) ||
                     line.startsWith("rtsp://", ignoreCase = true))
                ) {
                    val streamUrl = line
                    val channelName = pendingChannelName
                    val channelButton = Button(this).apply {
                        this.text = channelName
                        isAllCaps = false
                        setOnClickListener {
                            urlInput.setText(streamUrl)
                            showMode("direct")
                            playStream(streamUrl)
                        }
                    }
                    playlistBox.addView(
                        channelButton,
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    )
                    count += 1
                    pendingChannelName = "شبکه بدون نام"
                }
            }
            showMode("list")
            status.text = "$count شبکه از فهرست بارگذاری شد."
            if (count == 0) toast("در فایل، آدرس شبکه قابل شناسایی پیدا نشد")
        } catch (e: Exception) {
            toast("خطا در خواندن فهرست: ${e.message}")
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        webView.destroy()
        super.onDestroy()
    }
}
