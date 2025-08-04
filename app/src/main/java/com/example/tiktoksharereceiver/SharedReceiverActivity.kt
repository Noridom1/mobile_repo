package com.example.tiktoksharereceiver

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.*
import java.net.URL

class ShareReceiverActivity : AppCompatActivity() {
    private lateinit var textView: TextView
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textView = TextView(this)
        textView.textSize = 18f
        textView.setPadding(24, 48, 24, 24)
        setContentView(textView)

        // Handle the incoming intent
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                textView.text = "Received shared content:\n\n$sharedText"
                downloadVideo(sharedText)
            } else {
                textView.text = "No shared content received."
            }
        } else {
            textView.text = "Unsupported intent."
        }
    }

    private fun downloadVideo(videoUrl: String) {
        coroutineScope.launch {
            try {
                val input = URL(videoUrl).openStream()
                val fileName = "tiktok_${System.currentTimeMillis()}.mp4"

                val resolver = contentResolver
                val videoCollection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TikTokDownloads")
                }

                val videoUri = resolver.insert(videoCollection, contentValues)

                if (videoUri != null) {
                    resolver.openOutputStream(videoUri)?.use { outputStream ->
                        input.copyTo(outputStream)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ShareReceiverActivity, "Video saved to gallery", Toast.LENGTH_LONG).show()
                        textView.text = "${textView.text}\n\n✅ Downloaded and saved!"
                    }
                } else {
                    throw IOException("Failed to create new MediaStore record.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textView.text = "${textView.text}\n\n❌ Failed to download video: ${e.message}"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
