package com.example.of1.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow // Import asStateFlow

object AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    var currentUrl: String? = null
        private set

    // --- StateFlow to observe playback state ---
    // Internal MutableStateFlow
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Stopped())
    // Exposed immutable StateFlow for observation
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow() // Use asStateFlow()

    sealed class PlaybackState {
        data class Stopped(val previousUrl: String? = null) : PlaybackState()
        data class Playing(val url: String) : PlaybackState()
        // data class Error(val url: String, val message: String) : PlaybackState()
    }
    // --- End StateFlow ---


    fun play(context: Context, url: String) {
        Log.d("AudioPlayerManager", "play() called for URL: $url")

        val currentState = _playbackState.value // Get current state

        if (currentState is PlaybackState.Playing && currentState.url == url) {
            Log.d("AudioPlayerManager", "Toggling OFF playback for: $url")
            stop() // stop() will update the StateFlow
            return
        }

        stop() // Stop any previous playback first
        Log.d("AudioPlayerManager", "Stopped previous playback (if any)")
        currentUrl = url
        // Don't set state to Playing yet, wait for onPrepared

        mediaPlayer = MediaPlayer().apply {
            try {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { mp ->
                    Log.d("AudioPlayerManager", "MediaPlayer prepared for $url")
                    try {
                        mp.start()
                        // Update state AFTER start() is called and likely successful
                        _playbackState.value = PlaybackState.Playing(url)
                        Log.d("AudioPlayerManager", "MediaPlayer started for $url. State updated. isPlaying() = ${mp.isPlaying}")
                    } catch (e: IllegalStateException) {
                        Log.e("AudioPlayerManager", "Error starting MediaPlayer after prepare", e)
                        // _playbackState.value = PlaybackState.Error(url, "Error starting playback") // Optional Error state
                        stop() // Clean up and set state to Stopped
                    }
                }
                setOnCompletionListener {
                    Log.d("AudioPlayerManager", "MediaPlayer playback completed for $url")
                    stop() // stop() updates state to Stopped
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("AudioPlayerManager", "MediaPlayer error: what=$what, extra=$extra for $url")
                    // _playbackState.value = PlaybackState.Error(url, "MediaPlayer error ($what, $extra)") // Optional
                    stop() // Clean up and set state to Stopped
                    true
                }
                Log.d("AudioPlayerManager", "Calling prepareAsync() for $url")
                prepareAsync()
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error setting data source or preparing MediaPlayer", e)
                // _playbackState.value = PlaybackState.Error(url, "Error setting data source") // Optional
                stop() // Clean up and set state to Stopped
            }
        }
    }

    fun stop() {
        Log.d("AudioPlayerManager", "stop() called. Current MediaPlayer: $mediaPlayer")
        val urlBeforeStopping = currentUrl  // Save the URL before clearing

        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop() // Stop the actual mediaplayer playback
                }
                reset()
                release()
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error during stop/reset/release", e)
            }
        }
        mediaPlayer = null

        // Update state ONLY if it wasn't already stopped
        if (_playbackState.value !is PlaybackState.Stopped) {
            Log.d("AudioPlayerManager", "Setting state to Stopped")
            _playbackState.value = PlaybackState.Stopped(urlBeforeStopping)
        } else {
            Log.d("AudioPlayerManager", "State was already Stopped")
        }

        currentUrl = null  // Clear currentUrl AFTER updating state
    }

    // isPlaying is now just a direct check on the mediaPlayer
    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying ?: false

    // This can be removed or kept for convenience, but the StateFlow is primary
    // fun isCurrentlyPlaying(url: String): Boolean {
    //     return _playbackState.value.let { it is PlaybackState.Playing && it.url == url }
    // }
}