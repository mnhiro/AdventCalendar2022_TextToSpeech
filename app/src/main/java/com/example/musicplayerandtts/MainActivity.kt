package com.example.musicplayerandtts

import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

private const val MUSIC_PLAYER_AND_TEXT_TO_SPEECH = "MusicPlayerAndTextToSpeech"
private const val READ_MUSIC_INFO = "ReadMusicInfo"

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var musicList = mutableListOf<Music>()

    // ここは出来ればシングルトンに出来ると良い
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var player: ExoPlayer

    // 発話リスナーの中で直接PlayerにアクセスできないのでViewModelを介して操作
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        musicList.addAll(getMediaMetadataInfo())

        // 現在の曲が何かを指定する変数 -> ViewModelに移したほうが良さそう
        var count = 0

        // 次に再生される楽曲の情報を読み上げてPlayerを動かす
        viewModel.doneSpeak.observe(this) { index ->
            if (index < musicList.size) {
                player.addMediaItem(
                    MediaItem.fromUri(
                        musicList[index].contentUri
                    )
                )
                player.prepare()
                player.play()
            }
        }

        textToSpeech = TextToSpeech(this, this)
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            // 今回は省略
            override fun onStart(p0: String?) {
            }

            // 発話が完了したタイミングでViewModelに発話が完了したことを伝える
            override fun onDone(p0: String?) {
                viewModel.viewModelScope.launch {
                    if (p0.equals(MUSIC_PLAYER_AND_TEXT_TO_SPEECH)) {
                        viewModel.doneSpeak(count)
                    }
                }
            }

            // 今回は省略
            override fun onError(p0: String?) {
            }
        })

        player = ExoPlayer.Builder(this).build()
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                // 楽曲が終わったタイミングで処理を入れる
                if (player.playbackState == Player.STATE_ENDED){
                    // 次の曲に切り替える
                    count++

                    // 楽曲が切り替わるタイミングで再生を停止
                    player.stop()

                    // リストの終わりを検知する
                    if (count < musicList.size) {
                        // 次の曲を読み上げ
                        startSpeak(
                            generateReadingText(musicList[count]),
                            MUSIC_PLAYER_AND_TEXT_TO_SPEECH
                        )
                    }

                    // 楽曲リストの最後まで読み上げたら、最初の曲を指すように変更する
                    if (count == musicList.size) {
                        count = 0
                    }
                }
            }
        })

        setContent {
            Column {
                MusicList(musicList = musicList)
                TextToSpeechMusicPlayer()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale.JAPAN
            if (textToSpeech.isLanguageAvailable(locale) > TextToSpeech.LANG_AVAILABLE) {
                textToSpeech.language = Locale.JAPAN
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.not_available_japanese),
                    Toast.LENGTH_SHORT
                ).show()
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
        player.release()
        super.onDestroy()
    }

    private fun startSpeak(text: String, id: String) {
        println(text)
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
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
            Button(onClick = { startSpeak(generateReadingText(music), READ_MUSIC_INFO) } ) {
                Text(text = getString(R.string.text_read_music))
            }
            Column {
                Text(text = "sample曲名")
                Text(text = "sampleアーティスト")
//                Text(text = music.title)
//                Text(text = music.artist)
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
                onClick = {
                    startSpeak(
                        generateReadingText(musicList[0]),
                        MUSIC_PLAYER_AND_TEXT_TO_SPEECH
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = getString(R.string.text_start_player)
                )
            }
        }
    }

    private fun generateReadingText(music: Music): String {
        return "sampleアーティストさんで、sample曲名です"
//        return music.artist + "さんで、" + music.title + "です"
    }
}
