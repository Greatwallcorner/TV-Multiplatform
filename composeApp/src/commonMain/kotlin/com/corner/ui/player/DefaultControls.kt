package player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.corner.ui.player.PlayerController
import com.corner.util.formatTimestamp
import kotlin.math.roundToLong

@Composable
fun DefaultControls(modifier: Modifier = Modifier, controller: PlayerController) {

    val state by controller.state.collectAsState()

    val animatedTimestamp by animateFloatAsState(state.timestamp.toFloat())

    Column(
        modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Slider(
            value = animatedTimestamp,
            onValueChange = { controller.seekTo(it.roundToLong()) },
            valueRange = 0f..state.duration.toFloat(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp),
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.secondary, disabledActiveTrackColor = MaterialTheme.colorScheme.tertiary)
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Text("${state.timestamp.formatTimestamp()} / ${state.duration.formatTimestamp()}", color = MaterialTheme.colorScheme.onBackground)
            }
            if (state.isPlaying) {
                IconButton(controller::pause) {
                    Icon(Icons.Rounded.Pause, "pause media", tint = MaterialTheme.colorScheme.secondary)
                }
            } else {
                IconButton(controller::play) {
                    Icon(Icons.Rounded.PlayArrow, "play media", tint = MaterialTheme.colorScheme.secondary)
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isMuted || state.volume == 0f) IconButton(controller::toggleSound) {
                        Icon(Icons.AutoMirrored.Rounded. VolumeOff, "volume off", tint = MaterialTheme.colorScheme.secondary)
                    }
                    else {
                        if (state.volume < .5f) IconButton(controller::toggleSound) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeDown, "volume low", tint = MaterialTheme.colorScheme.secondary)
                        } else IconButton(controller::toggleSound) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, "volume high", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Slider(
                        value = state.volume,
                        onValueChange = controller::setVolume,
                        modifier = Modifier.width(128.dp),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.secondary, disabledActiveTrackColor = MaterialTheme.colorScheme.tertiary)
                    )
                    Speed(initialValue = 1F, Modifier) { controller.speed(it ?: 1F) }
                    IconButton({controller.toggleFullscreen()}){
                        Icon(Icons.Default.Fullscreen, contentDescription = "fullScreen/UnFullScreen", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

/**
 * See [this Stack Overflow post](https://stackoverflow.com/a/67765652).
 */
@Composable
fun Speed(
    initialValue: Float,
    modifier: Modifier = Modifier,
    onChange: (Float?) -> Unit
) {
    var input by remember { mutableStateOf(initialValue.toString()) }
    OutlinedTextField(
        colors = TextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.primary, unfocusedContainerColor = MaterialTheme.colorScheme.background),
        value = input,
        modifier = modifier,
        singleLine = true,
        textStyle = TextStyle.Default.copy(color = MaterialTheme.colorScheme.onBackground, background = MaterialTheme.colorScheme.background),
        leadingIcon = {
            Icon(
                painter = painterResource("pic/speed.svg"),
                contentDescription = "Speed",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        onValueChange = {
            input = if (it.isEmpty()) {
                it
            } else if (it.toFloatOrNull() == null) {
                input // Old value
            } else {
                it // New value
            }
            onChange(input.toFloatOrNull())
        },
    )
}