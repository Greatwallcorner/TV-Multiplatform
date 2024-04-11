package com.corner.ui.search

import SiteViewModel
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.corner.bean.Suggest
import com.corner.catvodcore.util.KtorClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils

@Composable
fun SearchBar(
    modifier: Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    initValue: String,
    onSearch: (String) -> Unit,
    isSearching: Boolean
) {
    var searchText by remember { mutableStateOf(initValue) }
    var isGettingSuggestion by remember { mutableStateOf(false) }
    var job: Job = remember {
        val j = Job()
        j.complete()
        j
    }

    var suggestions by remember { mutableStateOf<Suggest>(Suggest()) }

    TextField(
        modifier = modifier
            .focusRequester(focusRequester)
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.6f)
            .background(color = Color.Gray.copy(0.3f), shape = RoundedCornerShape(50)),
        onValueChange = { i ->
            searchText = i
            if (job.isActive) return@TextField
            job = SiteViewModel.viewModelScope.launch {
                if (isGettingSuggestion || searchText.isBlank()) return@launch
                delay(500)
                isGettingSuggestion = true
                try {
                    val response =
                        KtorClient.client.get("https://suggest.video.iqiyi.com/?if=mobile&key=" + searchText)
                    if (response.status == HttpStatusCode.OK) {
                        suggestions = Suggest.objectFrom(response.bodyAsText())
                    }
                } finally {
                    isGettingSuggestion = false
                }
                focusRequester.requestFocus()
            }
        },
        shape = RoundedCornerShape(50),
        placeholder = {
            Text(
                "请输入",
                modifier = Modifier.fillMaxSize(),
                fontSize = TextUnit(12f, TextUnitType.Sp)
            )
        },
        value = searchText,
        leadingIcon = {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxHeight(),
                    color = MaterialTheme.colors.secondary,
                    backgroundColor = MaterialTheme.colors.secondaryVariant
                )
            }
        },
        trailingIcon = {
            Icon(
                Icons.Outlined.Search, contentDescription = "Search", modifier = Modifier
                    .clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = rememberRipple(
                            bounded = true,
                            radius = 8.dp,
                            color = Color.LightGray.copy(0.5f)
                        ),
                        onClick = { onSearch(searchText) }, enabled = StringUtils.isNotBlank(searchText)
                    )
            )
        },
        textStyle = TextStyle(fontSize = TextUnit(12f, TextUnitType.Sp)),
        maxLines = 1,
        keyboardActions = KeyboardActions(onDone = { onSearch(searchText) }),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Text,
            autoCorrect = true
        ),
        colors = TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = Color.Transparent, // 焦点时下划线颜色
            unfocusedIndicatorColor = Color.Transparent,
            backgroundColor = Color.Gray.copy(alpha = 0.3f),
            textColor = MaterialTheme.colors.onBackground
        )
    )

    val scrollState = remember { ScrollState(0) }
    DropdownMenu(
        expanded = searchText.isNotEmpty() && !suggestions.isEmpty(), scrollState = scrollState,
        offset = DpOffset(100.dp, 3.dp),
        onDismissRequest = {
            suggestions = Suggest()
        },
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            clippingEnabled = true
        ),
        modifier = Modifier.animateContentSize(animationSpec = spring())
            .clip(RoundedCornerShape(15.dp))
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 5.dp)) {
            suggestions.data?.forEach {
                DropdownMenuItem(
                    modifier = Modifier,
                    onClick = {
                        suggestions.clear()
                        onSearch(it.name)
                    },
                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 5.dp),
                ) {
                    Text(
                        it.name,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.align(alignment = Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}