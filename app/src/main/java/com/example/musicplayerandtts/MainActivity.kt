package com.example.musicplayerandtts

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import java.io.File
import java.util.*


class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var player: ExoPlayer

    data class Music(
        val title: String,
        val artist: String,
        val contentUri: Uri
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(this, this)
        player = ExoPlayer.Builder(this).build()

        val musicList = getMediaMetadataInfo()
        val playlist = musicList.map { music ->
            music.contentUri
        }

        playlist.map { uri ->
            player.addMediaItem(MediaItem.fromUri(uri))
        }
        
        player.prepare()

        setContent {
            Column {
                MusicList(musicList = musicList)
                TextToSpeechMusicPlayer()
            }
        }


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
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "musicRead")
    }

    private fun getMediaMetadataInfo(): MutableList<Music> {
        val arMediaPath: MutableList<String> = ArrayList()
        val musicList = mutableListOf<Music>()

        val fileDir: File =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)

        val strPath: String = fileDir.path
        val filePaths = File(strPath).listFiles()

        if (filePaths != null) {
            for (file in filePaths) {
                if (file.isFile) {
                    // ファイルパスを保存
                    arMediaPath.add(strPath + "/" + file.name)
                }
            }
        }

        // メタ情報を取得するためのクラス
        val mmr = MediaMetadataRetriever()

        for (i in arMediaPath.indices) {
            // ファイルパスをセット
            mmr.setDataSource(arMediaPath[i])

            // メタ情報を取得
            val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""

            musicList += Music(
                title = title,
                artist = artist,
                contentUri = arMediaPath[i].toUri()
            )
        }

        return musicList
    }

    @Composable
    fun MusicList(musicList: List<Music>) {
        LazyColumn(
            modifier = Modifier
                .width(400.dp)
                .height(400.dp)
        ) {
            items(musicList.size) { index ->
                MusicRow(musicList[index])
            }
        }
    }

    @Composable
    fun MusicRow(music: Music) {
        Row {
            Button(onClick = { startSpeak(generateReadingText(music)) } ) {
                Text(text = getString(R.string.text_read_music))
            }
            Column {
                Text(text = music.title)
                Text(text = music.artist)
            }
        }
    }
    
    @Composable
    fun TextToSpeechMusicPlayer() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = { player.play() }
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = getString(R.string.text_start_player)
                )
            }
        }
    }

    private fun generateReadingText(music: Music): String {
        return music.artist + "さんで" + music.title + "です"
    }
}