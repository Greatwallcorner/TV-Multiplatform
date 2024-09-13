package com.corner.ui.search

import SiteViewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    modifier: Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    initValue: String,
    onSearch: (String) -> Unit,
    isSearching: Boolean
) {
    var searchText by remember { mutableStateOf(initValue) }
    val searching by rememberUpdatedState(isSearching)
    var isGettingSuggestion by remember { mutableStateOf(false) }
    var job: Job = remember {
        val j = Job()
        j.complete()
        j
    }

    var suggestions by remember { mutableStateOf(Suggest()) }

    val searchFun = fun(text: String) {
        onSearch(text)
        suggestions = Suggest()
    }

    TextField(
        modifier = modifier
            .focusRequester(focusRequester)
            .fillMaxWidth(0.9f)
            .fillMaxHeight()
            .padding(top = 2.dp)
            .background(color = Color.Gray.copy(0.3f), shape = RoundedCornerShape(50)),
        singleLine = true,
        onValueChange = { i ->
            searchText = i
            if (job.isActive) return@TextField
            job = SiteViewModel.viewModelScope.launch {
                if (isGettingSuggestion || searchText.isBlank()) return@launch
                delay(500)
                isGettingSuggestion = true
                try {
                    val response =
                        KtorClient.client.get("https://suggest.video.iqiyi.com/?if=mobile&key=$searchText")
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
        value = searchText,
        leadingIcon = {
            AnimatedVisibility(
                visible = searching,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxHeight(),
//                    color = MaterialTheme.colors.secondary,
//                    backgroundColor = MaterialTheme.colors.secondaryVariant
                )
            }
        },
        trailingIcon = {
            IconButton(
                modifier = Modifier,
                onClick = { searchFun(searchText) },
                enabled = StringUtils.isNotBlank(searchText)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = "Search", modifier = Modifier)
            }
        },
        textStyle = TextStyle(fontSize = TextUnit(12f, TextUnitType.Sp)),
        keyboardActions = KeyboardActions(onDone = { searchFun(searchText) }),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Text,
            autoCorrect = true
        ),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent, // 焦点时下划线颜色
            unfocusedIndicatorColor = Color.Transparent,
//            backgroundColor = Color.Gray.copy(alpha = 0.3f),
//            textColor = MaterialTheme.colors.onBackground
        )
    )

    val scrollState = remember { ScrollState(0) }
    val showSuggestion = derivedStateOf { searchText.isNotEmpty() && !suggestions.isEmpty() }
    DropdownMenu(
        expanded = showSuggestion.value,
        scrollState = scrollState,
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
                        searchFun(it.name)
                    },
                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 5.dp), text = {
                        Text(
                            it.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                        )
                    }
                )
            }
        }
    }
}