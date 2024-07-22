package com.corner.ui.player

import AppTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.value.update
import com.corner.catvodcore.util.Utils
import com.corner.ui.decompose.DetailComponent
import com.corner.ui.player.vlcj.VlcjFrameController
import com.corner.util.formatTimestamp
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToLong

@Composable
fun DefaultControls(modifier: Modifier = Modifier, controller: VlcjFrameController, component: DetailComponent) {

    val playerState by controller.state.collectAsState()

    val animatedTimestamp by animateFloatAsState(playerState.timestamp.toFloat())

    Column(
        modifier.padding(horizontal = 8.dp, vertical = 1.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Slider(
            value = animatedTimestamp,
            onValueChange = { controller.seekTo(it.roundToLong()) },
            valueRange = 0f..playerState.duration.toFloat(),
            modifier = Modifier.fillMaxWidth().height(15.dp).padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 20.dp),
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.secondary, disabledActiveTrackColor = MaterialTheme.colorScheme.tertiary)
        )
        Row(
            Modifier.fillMaxWidth().height(50.dp).padding(bottom = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButtonTransparent(if(playerState.opening == -1L) "片头" else Utils.formatMilliseconds(playerState.opening)){
                controller.updateOpening(component.model.value.detail)
            }
            TextButtonTransparent(if(playerState.ending == -1L) "片尾" else Utils.formatMilliseconds(playerState.ending)){
                controller.updateEnding(component.model.value.detail)
            }

            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Text("${playerState.timestamp.formatTimestamp()} / ${playerState.duration.formatTimestamp()}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (playerState.state == PlayState.PLAY) {
                IconButton(controller::pause) {
                    Icon(Icons.Rounded.Pause, "pause media", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                IconButton(controller::play) {
                    Icon(Icons.Rounded.PlayArrow, "play media", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (playerState.isMuted || playerState.volume == 0f) IconButton(controller::toggleSound) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeOff, "volume off", tint = MaterialTheme.colorScheme.primary)
                    }
                    else {
                        if (playerState.volume < .5f) IconButton(controller::toggleSound) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeDown, "volume low", tint = MaterialTheme.colorScheme.primary)
                        } else IconButton(controller::toggleSound) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, "volume high", tint = MaterialTheme.colorScheme.primary )
                        }
                    }
                    Slider(
                        value = playerState.volume,
                        onValueChange = controller::setVolume,
                        modifier = Modifier.width(128.dp),
                        valueRange = 0f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.secondary, disabledActiveTrackColor = MaterialTheme.colorScheme.tertiary)
                    )
                    Spacer(Modifier.size(5.dp))
                    Speed(initialValue = playerState.speed, Modifier.width(85.dp).height(45.dp)) { controller.speed(it ?: 1F) }
                    IconButton({controller.toggleFullscreen()}){
                        Icon(Icons.Default.Fullscreen, contentDescription = "fullScreen/UnFullScreen", tint = MaterialTheme.colorScheme.primary)
                    }
                    TextButtonTransparent("选集"){
                        component.model.update { it.copy(showEpChooserDialog = !component.model.value.showEpChooserDialog) }
                        println("选集")
                    }
                }
            }
        }
    }
}

@Composable
fun TextButtonTransparent(text:String, onClick:()->Unit){
    androidx.compose.material3.TextButton(onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary)
    ){
        Text(text, color = MaterialTheme.colorScheme.primary, fontSize = TextUnit(12f, TextUnitType.Sp))
    }
}

@Preview
@Composable
fun previewControlBar(){
    AppTheme {
//        DefaultControls(Modifier, VlcjFrameController(), null)
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
        value = input,

        modifier = modifier.padding(0.dp),
        singleLine = true,
        textStyle = TextStyle.Default.copy(color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center,
            fontSize = TextUnit(12f, TextUnitType.Sp)),
        leadingIcon = {
            Icon(
                painter = painterResource("pic/speed.svg"),
                contentDescription = "Speed",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
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

@androidx.compose.desktop.ui.tooling.preview.Preview
@Composable
fun previewSpeed(){
    Speed(1f, Modifier.width(85.dp).height(45.dp), onChange = {})
}