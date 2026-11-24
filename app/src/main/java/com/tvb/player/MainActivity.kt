package com.tvb.player

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var urlInput: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 20, 16, 16)
            setBackgroundColor(0xFF101820.toInt())
        }
        val title = TextView(this).apply {
            text = "TV R"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, 14)
        }
        urlInput = EditText(this).apply {
            hint = "آدرس پخش زنده یا لینک M3U8"
            singleLine = true
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFFB0BEC5.toInt())
            setSelectAllOnFocus(false)
        }
        playerView = PlayerView(this).apply {
            useController = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        status = TextView(this).apply {
            text = "آماده پخش"
            setTextColor(0xFFFFC400.toInt())
            setPadding(0, 10, 0, 8)
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val play = Button(this).apply { text = "پخش" }
        val stop = Button(this).apply { text = "توقف پخش" }
        val record = Button(this).apply { text = "شروع ضبط" }
        val stopRecord = Button(this).apply { text = "پایان ضبط" }
        listOf(play, stop, record, stopRecord).forEach {
            row.addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        page.addView(title)
        page.addView(urlInput, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        page.addView(playerView)
        page.addView(status)
        page.addView(row)
        setContentView(page)

        play.setOnClickListener { playStream() }
        stop.setOnClickListener {
            player?.stop()
            status.text = "پخش متوقف شد"
        }
        record.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isBlank()) {
                toast("ابتدا آدرس پخش را وارد کنید")
            } else {
                RecorderService.start(this, url)
                status.text = "ضبط جریان آغاز شد"
            }
        }
        stopRecord.setOnClickListener {
            RecorderService.stop(this)
            status.text = "درخواست توقف ضبط ارسال شد"
        }
    }

    private fun playStream() {
        val url = urlInput.text.toString().trim()
        if (url.isBlank()) {
            toast("ابتدا آدرس پخش را وارد کنید")
            return
        }
        try {
            if (player == null) {
                player = ExoPlayer.Builder(this).build().also {
                    playerView.player = it
                    it.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            status.text = when (playbackState) {
                                Player.STATE_BUFFERING -> "در حال بارگذاری..."
                                Player.STATE_READY -> "در حال پخش"
                                Player.STATE_ENDED -> "پخش پایان یافت"
                                else -> "آماده پخش"
                            }
                        }
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            status.text = "خطا در پخش: ${error.message ?: "نامشخص"}"
                        }
                    })
                }
            }
            player?.setMediaItem(MediaItem.fromUri(url))
            player?.prepare()
            player?.play()
        } catch (e: Exception) {
            status.text = "خطا: ${e.message ?: "نامشخص"}"
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
