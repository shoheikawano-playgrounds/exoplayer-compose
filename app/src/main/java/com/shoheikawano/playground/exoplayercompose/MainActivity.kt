package com.shoheikawano.playground.exoplayercompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import com.shoheikawano.playground.exoplayercompose.ui.theme.ExoPlayerComposePlaygroundTheme

private const val VIDEO_URL =
    "https://storage.googleapis.com/exoplayer-test-media-1/gen-3/screens/dash-vod-single-segment/video-vp9-360.webm"

/**
 * A simple playground app for using ExoPlayer with Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<PlaygroundViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExoPlayerComposePlaygroundTheme {
                PlaygroundScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun PlaygroundScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaygroundViewModel,
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            viewModel.appendLogText("player.build: $this, preparing url=$VIDEO_URL")

            addAnalyticsListener(EventLogger())
            addMediaItem(MediaItem.fromUri(VIDEO_URL))
            prepare()
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            addListener(object : Player.Listener {
                override fun onIsLoadingChanged(isLoading: Boolean) {
                    viewModel.appendLogText("onIsLoadingChanged called: isLoading=$isLoading")
                    viewModel.isLoading = isLoading
                }

                override fun onPlayerError(error: PlaybackException) {
                    viewModel.appendLogText("onPlayerError called: error=$error")
                }
            })
        }
    }

    DisposableEffect(context) {
        onDispose {
            viewModel.appendLogText("onDispose called: player=$player")
            player.release()
        }
    }

    Scaffold(modifier = modifier) { innerPadding ->
        PlayerInLazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            viewModel = viewModel, player = player
        )
//        PlayerInColumn(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(innerPadding)
//                .verticalScroll(rememberScrollState()),
//            viewModel = viewModel,
//            player = player,
//        )
    }
}

@Composable
private fun PlayerInColumn(
    viewModel: PlaygroundViewModel,
    player: ExoPlayer,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VideoSurface(
            player = player,
            logging = viewModel::appendLogText
        )

        if (viewModel.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(4.dp))
        }

        Spacer(modifier = Modifier.size(24.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            fontSize = 8.sp,
            text = viewModel.logText,
            lineHeight = 10.sp,
        )
    }
}

@Composable
private fun PlayerInLazyColumn(
    viewModel: PlaygroundViewModel,
    player: ExoPlayer,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(20) {
            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = "Item $it",
                fontSize = 24.sp,
            )
        }

        item {
            VideoSurface(
                player = player,
                logging = viewModel::appendLogText
            )
        }

        (5..80).forEach {
            item {
                Spacer(modifier = Modifier.size(24.dp))
            }

            item {
                Text(
                    text = "Item $it",
                    fontSize = 24.sp,
                )
            }
        }

        item {
            if (viewModel.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(4.dp))
            }
        }


        item {
            Spacer(modifier = Modifier.size(24.dp))
        }

        item {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                text = viewModel.logText,
            )
        }
    }
}

@Composable
private fun VideoSurface(
    player: ExoPlayer,
    logging: (String) -> Unit,
) {
    AndroidExternalSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .aspectRatio(1.78f)
            .clip(RoundedCornerShape(16.dp)),
        onInit = {
            onSurface { surface, _, _ ->
                logging("onSurface called: surface=$surface")
                player.setVideoSurface(surface)
                surface.onDestroyed {
                    logging("surface.onDestroyed called: surface=$surface")
                    player.clearVideoSurface()
                }
            }
        }
    )
}

internal class PlaygroundViewModel : ViewModel() {
    var logText by mutableStateOf("--- Logs ----")
    var isLoading by mutableStateOf(false)

    fun appendLogText(text: String) {
        logText += "\n$text\n"
    }
}