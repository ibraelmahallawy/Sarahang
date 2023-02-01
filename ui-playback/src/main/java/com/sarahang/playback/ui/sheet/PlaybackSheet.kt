package com.sarahang.playback.ui.sheet

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.pager.rememberPagerState
import com.sarahang.playback.core.NONE_PLAYBACK_STATE
import com.sarahang.playback.core.PlaybackConnection
import com.sarahang.playback.core.artwork
import com.sarahang.playback.core.isIdle
import com.sarahang.playback.core.models.LocalPlaybackConnection
import com.sarahang.playback.core.models.PlaybackQueue
import com.sarahang.playback.core.models.QueueTitle
import com.sarahang.playback.ui.R
import com.sarahang.playback.ui.audio.*
import com.sarahang.playback.ui.components.ResizableLayout
import com.sarahang.playback.ui.components.copy
import com.sarahang.playback.ui.components.isWideLayout
import com.sarahang.playback.ui.theme.Specs
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

@Composable
fun PlaybackSheet(onClose: (() -> Unit)?, goToItem: () -> Unit = {}) {
    val listState = rememberLazyListState()
    val audioActionHandler = audioActionHandler()
    CompositionLocalProvider(LocalAudioActionHandler provides audioActionHandler) {
        PlaybackSheet(
            onClose = onClose,
            goToItem = goToItem,
            listState = listState,
            queueListState = rememberLazyListState()
        )
    }
}

@Composable
internal fun PlaybackSheet(
    onClose: (() -> Unit)?,
    goToItem: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    queueListState: LazyListState = rememberLazyListState(),
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current,
) {
    val playbackState by playbackConnection.playbackState.collectAsStateWithLifecycle()
    val playbackQueue by rememberFlowWithLifecycle(playbackConnection.playbackQueue)
    val nowPlaying by playbackConnection.nowPlaying.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(playbackQueue.currentIndex)

    val adaptiveColor by adaptiveColor(
        image = nowPlaying.artwork,
        initial = MaterialTheme.colorScheme.onBackground,
        gradientEndColor = MaterialTheme.colorScheme.background,
    )
    val contentColor by animateColorAsState(adaptiveColor.color, ADAPTIVE_COLOR_ANIMATION)

    LaunchedEffect(playbackConnection) {
        playbackConnection.playbackState
            .filter { it != NONE_PLAYBACK_STATE }
            .collectLatest { if (it.isIdle) if (onClose != null) onClose() }
    }

    if (playbackState == NONE_PLAYBACK_STATE) {
        Row(Modifier.fillMaxSize()) { CircularProgressIndicator() }
        return
    }

    MaterialTheme {
        BoxWithConstraints {
            val isWideLayout = isWideLayout()
            val maxWidth = maxWidth

            Row(Modifier.fillMaxSize()) {
                if (isWideLayout) {
                    ResizablePlaybackQueue(
                        maxWidth = maxWidth,
                        playbackQueue = playbackQueue,
                        queueListState = queueListState,
                        adaptiveColor = adaptiveColor
                    )
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .background(adaptiveColor.gradient)
                        .weight(1f)
                ) { paddings ->
                    LazyColumn(
                        state = listState,
                        contentPadding = paddings.copy(top = 0.dp),
                    ) {
                        if (onClose != null) {
                            item {
                                PlaybackSheetTopBar(
                                    playbackQueue = playbackQueue,
                                    onClose = onClose
                                )
                            }
                        }

                        item {
                            PlaybackArtworkPagerWithNowPlayingAndControls(
                                nowPlaying = nowPlaying,
                                playbackState = playbackState,
                                pagerState = pagerState,
                                contentColor = contentColor,
                                onTitleClick = goToItem,
                                onArtistClick = { },
                                artworkVerticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillParentMaxHeight(fraction = 0.70f)
                                    .padding(vertical = 12.dp),
                            )
                        }

                        if (!isWideLayout && playbackQueue.isNotEmpty()) {
                            playbackQueue(
                                playbackQueue = playbackQueue,
                                playbackConnection = playbackConnection,
                                adaptiveColor = adaptiveColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ResizablePlaybackQueue(
    maxWidth: Dp,
    playbackQueue: PlaybackQueue,
    queueListState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    resizableLayoutViewModel: ResizablePlaybackSheetLayoutViewModel = hiltViewModel(),
    dragOffset: State<Float> = resizableLayoutViewModel.dragOffset.collectAsStateWithLifecycle(),
    setDragOffset: (Float) -> Unit = resizableLayoutViewModel::setDragOffset,
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current,
    adaptiveColor: AdaptiveColorResult
) {
    ResizableLayout(
        availableWidth = maxWidth,
        initialWeight = 0.6f,
        minWeight = 0.4f,
        maxWeight = 1.25f,
        dragOffset = dragOffset,
        setDragOffset = setDragOffset,
        analyticsPrefix = "playbackSheet.layout",
        modifier = modifier,
    ) { resizableModifier ->
        val labelMod = Modifier.padding(top = Specs.padding)
        Surface {
            LazyColumn(
                state = queueListState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxHeight()
            ) {
                playbackQueueLabel(resizableModifier.then(labelMod))

                if (playbackQueue.isLastAudio) {
                    item {
                        Text(
                            text = stringResource(R.string.playback_queue_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Specs.padding)
                        )
                    }
                }

                playbackQueue(
                    playbackQueue = playbackQueue,
                    playbackConnection = playbackConnection,
                    adaptiveColor = adaptiveColor
                )
            }
            Divider(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .then(resizableModifier)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackSheetTopBar(
    playbackQueue: PlaybackQueue,
    onClose: () -> Unit
) {
    // TODO: Remove after https://android-review.googlesource.com/c/platform/frameworks/support/+/2209896/ is available
    // override colorScheme for TopAppBar so we can make the background transparent
    // but revert it back for actions since PlaybackSheetTopBarActions uses a Surface
    val colorScheme = MaterialTheme.colorScheme
    MaterialTheme(colorScheme.copy(surface = Color.Transparent)) {
        TopAppBar(
            title = { PlaybackSheetTopBarTitle(playbackQueue) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        rememberVectorPainter(Icons.Default.KeyboardArrowDown),
                        modifier = Modifier.size(Specs.iconSize),
                        contentDescription = null,
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun PlaybackSheetTopBarTitle(
    playbackQueue: PlaybackQueue,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .basicMarquee()
            .offset(x = (-8).dp)
    ) {
        val context = LocalContext.current
        val queueTitle = QueueTitle.from(playbackQueue.title.orEmpty())
        Text(
            text = queueTitle.localizeValue(context).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

private fun LazyListScope.playbackQueueLabel(modifier: Modifier = Modifier) {
    item {
        Row(modifier = modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.playback_queue_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(Specs.padding)
            )
        }
    }
}

private fun LazyListScope.playbackQueue(
    playbackQueue: PlaybackQueue,
    playbackConnection: PlaybackConnection,
    adaptiveColor: AdaptiveColorResult
) {
    itemsIndexed(playbackQueue, key = { _, a -> a.id }) { index, audio ->
        AudioRow(
            audio = audio,
            imageSize = 40.dp,
            audioIndex = index,
            adaptiveColor = adaptiveColor,
            onPlayAudio = {
                playbackConnection.transportControls?.skipToQueueItem(index.toLong())
            },
            modifier = Modifier.animateItemPlacement()
        )
    }
}

