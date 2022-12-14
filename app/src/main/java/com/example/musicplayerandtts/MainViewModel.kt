package com.example.musicplayerandtts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _doneSpeak = MutableLiveData<Int>()
    val doneSpeak: LiveData<Int> = _doneSpeak
    fun doneSpeak(index: Int) {
        _doneSpeak.value = index
    }
}