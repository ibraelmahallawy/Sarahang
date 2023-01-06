package com.sarahang.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sarahang.playback.core.models.LocalPlaybackConnection
import com.sarahang.playback.ui.audio.AudioActionHost
import com.sarahang.playback.ui.audio.AudioRow
import com.sarahang.playback.ui.audio.PlaybackHost
import com.sarahang.playback.ui.player.mini.MiniPlayer
import com.sarahang.sample.FakeData.audios
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                PlaybackHost {
                    AudioActionHost {
                        Audios()
                    }
                }
            }
        }
    }
}

@Composable
fun Audios() {
    val playbackConnection = LocalPlaybackConnection.current
    val playbackState by playbackConnection.playbackState.collectAsStateWithLifecycle()
    val nowPlaying by playbackConnection.nowPlaying.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            audios.forEach {
                AudioRow(audio = it)
            }
        }

        MiniPlayer(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        )
    }
}
