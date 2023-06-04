package com.sarahang.playback.ui.sheet

import android.support.v4.media.MediaMetadataCompat
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sarahang.playback.core.PlaybackConnection
import com.sarahang.playback.core.artwork
import com.sarahang.playback.core.models.LocalPlaybackConnection
import com.sarahang.playback.core.playPause
import com.sarahang.playback.ui.R
import com.sarahang.playback.ui.components.CoverImage
import com.sarahang.playback.ui.theme.coloredRippleClickable

@Composable
internal fun PlaybackArtwork(
    artwork: String?,
    nowPlaying: MediaMetadataCompat,
    modifier: Modifier = Modifier,
    contentColor: Color = LocalContentColor.current,
    onClick: (() -> Unit)? = null,
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current,
) {
    CoverImage(
        data = artwork?.toUri(),
        shape = RoundedCornerShape(8.dp),
        containerColor = Color.Transparent,
        contentColor = contentColor,
        bitmapPlaceholder = nowPlaying.artwork,
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .then(modifier),
        imageModifier = Modifier
            .coloredRippleClickable(
                onClick = {
                    if (onClick != null) onClick.invoke()
                    else playbackConnection.mediaController?.playPause()
                },
                onClickLabel = stringResource(R.string.cd_play_pause),
                color = contentColor,
                rippleRadius = Dp.Unspecified,
            ),
    )
}
