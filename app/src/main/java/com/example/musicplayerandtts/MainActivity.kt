package com.example.musicplayerandtts

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech

    data class Music(
        val uri: Uri,
        val displayName: String,
        val title: String,
        val artist: String,
        val duration: Int,
        val size: Int
    )

    private val musicList = mutableListOf<Music>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
        )

        val selection = null

        val sortOrder = null

        val resolver = this.contentResolver

        val query = resolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                musicList += Music(
                    contentUri,
                    displayName,
                    title,
                    artist,
                    duration,
                    size
                )
            }
        }

        setContent {
            Column {
                TalkToSpeechScreen()
                FeatureThatRequiresExternalPermission()
                MusicList(musicList = musicList)
            }
        }

        textToSpeech = TextToSpeech(this, this)

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(p0: String?) {
            }

            override fun onDone(p0: String?) {
            }

            @Deprecated("", ReplaceWith(""))
            override fun onError(p0: String?) {
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.let { tts ->
                val locale = Locale.JAPAN
                if (tts.isLanguageAvailable(locale) > TextToSpeech.LANG_AVAILABLE) {
                    tts.language = Locale.JAPAN
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.not_available_japanese),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                this,
                getString(R.string.error_init_tts),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        textToSpeech.shutdown()
        super.onDestroy()
    }

    private fun startSpeak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "uniqueId")
    }

    @Composable
    fun TalkToSpeechScreen() {
        Column {
            Button(
                onClick = { startSpeak(getString(R.string.this_is_test)) }
            ) {
                Text(text = getString(R.string.start_voice))
            }
            Button(onClick = { /*TODO*/ }) {
                Text(text = getString(R.string.transition_to_music_player))
            }
        }
    }

    @Composable
    fun MusicList(musicList: List<Music>) {
        LazyColumn{
            items(musicList.size) { index ->
                MusicRow(musicList[index])
            }
        }
    }

    @Composable
    fun MusicRow(music: Music) {
        Column {
            Text(text = music.displayName)
            Text(text = music.artist)
            Text(text = music.duration.toString())
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun FeatureThatRequiresExternalPermission() {

        val externalStoragePermissionState = rememberPermissionState(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        if (externalStoragePermissionState.status.isGranted) {
            Text("Read External Storage permission Granted")
        } else {
            Column {
                val textToShow = if (externalStoragePermissionState.status.shouldShowRationale) {
                    "The reading external storage is important for this app. Please grant the permission."
                } else {
                    "Reading external storage not available"
                }

                Text(textToShow)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { externalStoragePermissionState.launchPermissionRequest() }) {
                    Text("Request permission")
                }
            }
        }
    }
}