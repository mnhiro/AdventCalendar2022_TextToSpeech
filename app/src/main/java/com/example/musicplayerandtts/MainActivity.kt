package com.example.musicplayerandtts

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column {
                Button(
                    onClick = { startSpeak(getString(R.string.this_is_test), true) }
                ) {
                    Text(text = getString(R.string.start_voice))
                }
                Button(onClick = { /*TODO*/ }) {
                    Text(text = getString(R.string.transition_to_music_player))
                }
            }
        }

        textToSpeech = TextToSpeech(this, this)

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(p0: String?) {
            }

            override fun onDone(p0: String?) {
            }

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

    private fun startSpeak(text: String, isImmediately: Boolean) {
        if (isImmediately) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "uniqueId")
        } else {
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, "uniqueId")
        }
    }
}