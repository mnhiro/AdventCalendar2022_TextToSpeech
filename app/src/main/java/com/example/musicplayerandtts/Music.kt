package com.example.musicplayerandtts

import android.net.Uri

data class Music(
    val title: String,
    val artist: String,
    val contentUri: Uri
)
