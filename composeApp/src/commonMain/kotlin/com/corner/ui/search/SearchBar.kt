package com.corner.ui.search

import SiteViewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.corner.bean.Suggest
import com.corner.ui.decompose.component.DefaultSearchComponent
import com.corner.util.KtorClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils

@Composable
fun SearchBar(
    component: DefaultSearchComponent,
    modifier: Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    initValue: String,
    onSearch: (String) -> Unit,
    isSearching: Boolean
) {
    val modelState = component.model.subscribeAsState()
    var searchText by remember { mutableStateOf(initValue) }
    val searching by rememberUpdatedState(isSearching)
    var isGettingSuggestion by remember { mutableStateOf(false) }
    var showSearchSiteDialog by remember { mutableStateOf(false) }
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

    Row {
        Button(onClick = { showSearchSiteDialog = true }) {
            Text(modelState.value.searchBarText)
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
                            KtorClient.createHttpClient { }
                                .get("https://suggest.video.iqiyi.com/?if=mobile&key=$searchText")
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
            ),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent, // 焦点时下划线颜色
                unfocusedIndicatorColor = Color.Transparent,
//            backgroundColor = Color.Gray.copy(alpha = 0.3f),
//            textColor = MaterialTheme.colors.onBackground
            )
        )
    }

    val scrollState = remember { ScrollState(0) }
    val showSuggestion = remember { derivedStateOf { searchText.isNotEmpty() && !suggestions.isEmpty() } }
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
    DropdownMenu(
        expanded = showSearchSiteDialog,
        onDismissRequest = { showSearchSiteDialog = false },
        offset = DpOffset(100.dp, 3.dp),
    ) {
        val parentState = remember {
            derivedStateOf {
                when {
                    modelState.value.searchableSites.all { it.isSearchable() } -> ToggleableState.On
                    modelState.value.searchableSites.none { it.isSearchable() } -> ToggleableState.Off
                    else -> ToggleableState.Indeterminate
                }
            }
        }
        Column(
            Modifier.padding(horizontal = 5.dp, vertical = 5.dp)
                .clip(shape = RoundedCornerShape(5.dp))
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("全选")
                TriStateCheckbox(state = parentState.value, onClick = {
                    component.updateModel {
                        val newState = parentState.value != ToggleableState.On
                        it.searchableSites.forEachIndexed { _, site ->
                            if (newState) {
                                site.searchable = 1
                            } else {
                                site.searchable = 0
                            }
                        }
                    }
                })
            }
            HorizontalDivider()
            val siteList = remember { derivedStateOf { modelState.value.searchableSites.toList() } }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(5.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.height(300.dp).width(450.dp)
            ) {
                items(items = siteList.value) { site ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(site.name)
                        Checkbox(site.isSearchable(), onCheckedChange = {
                            component.updateModel {
                                it.searchableSites.first { iSite -> iSite.key == site.key }
                                    ?.apply { toggleSearchable() }
                            }
                        })
                    }
                }
            }
        }
    }
}

