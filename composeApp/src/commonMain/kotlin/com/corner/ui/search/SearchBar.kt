package com.corner.ui.search

import com.corner.catvodcore.viewmodel.SiteViewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.corner.bean.Suggest
import com.corner.ui.nav.vm.SearchViewModel
import com.corner.util.network.KtorClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.window.PopupProperties


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SearchBar(
    vm: SearchViewModel,
    modifier: Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    initValue: String,
    onSearch: (String) -> Unit,
    isSearching: Boolean,
    onFocusRequested: () -> Unit = { focusRequester.requestFocus() }
) {
    var focusState by remember { mutableStateOf<FocusState?>(null) }
    val modelState = vm.state.collectAsState()
    var textFieldValue by remember { mutableStateOf(TextFieldValue(initValue)) }
    var searchText by remember { mutableStateOf(initValue) }
    val searching by rememberUpdatedState(isSearching)
    var isGettingSuggestion by remember { mutableStateOf(false) }
    var showSearchSiteDialog by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var suggestions by remember { mutableStateOf(Suggest()) }

    // 自动获取焦点并全选文本
    LaunchedEffect(focusRequester) {
        snapshotFlow { focusState?.isFocused == true && textFieldValue.text.isNotEmpty() }
            .collect { isFocused ->
                if (isFocused && textFieldValue.text.isNotEmpty()) {
                    delay(50)
                    textFieldValue = textFieldValue.copy(
                        selection = TextRange(0, textFieldValue.text.length)
                    )
                }
            }
    }

    val searchFun = { text: String ->
        onSearch(text)
        suggestions = Suggest()
    }

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp).wrapContentHeight()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            // 网站选择按钮
            Button(
                onClick = { showSearchSiteDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = "Filter",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        modelState.value.searchBarText,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 搜索框
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = if (focusState?.isFocused == true)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                BasicTextField(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .focusRequester(focusRequester)
                        .onFocusEvent { focusState = it },
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        searchText = newValue.text
                        job?.cancel()
                        job = SiteViewModel.viewModelScope.launch {
                            if (isGettingSuggestion || searchText.isBlank()) return@launch
                            delay(500)
                            isGettingSuggestion = true
                            try {
                                val response = KtorClient.createHttpClient { }
                                    .get("https://suggest.video.iqiyi.com/?if=mobile&key=$searchText")
                                if (response.status == HttpStatusCode.OK) {
                                    val body = response.bodyAsText()
                                    suggestions = Suggest.objectFrom(body)
                                }
                            } catch (e:Exception) {
                            }finally {
                                isGettingSuggestion = false
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (searching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                if (textFieldValue.text.isEmpty()) {
                                    Text(
                                        "搜索...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                                innerTextField()
                            }

                            if (searchText.isNotBlank()) {
                                IconButton(
                                    onClick = { searchFun(searchText) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                )
            }
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
                            textFieldValue = TextFieldValue(it.name) // 更新文本字段值
                            searchText = it.name // 更新搜索文本
                            searchFun(it.name) // 执行搜索
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

    // 网站选择对话框
    if (showSearchSiteDialog) {
        val parentState = remember {
            derivedStateOf {
                when {
                    modelState.value.searchableSites.all { it.isSearchable() } -> ToggleableState.On
                    modelState.value.searchableSites.none { it.isSearchable() } -> ToggleableState.Off
                    else -> ToggleableState.Indeterminate
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showSearchSiteDialog = false },
            title = {
                Text("选择搜索网站",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary)
            },
            text = {
                Column {
                    // 全选行
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "全选",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f))
                        TriStateCheckbox(
                            state = parentState.value,
                            onClick = {
                                vm.updateModel {
                                    val newState = parentState.value != ToggleableState.On
                                    it.searchableSites.forEach { site ->
                                        site.searchable = if (newState) 1 else 0
                                    }
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // 网站列表
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(modelState.value.searchableSites.toList()) { site ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Checkbox(
                                    checked = site.isSearchable(),
                                    onCheckedChange = {
                                        vm.updateModel {
                                            it.searchableSites.first { iSite -> iSite.key == site.key }
                                                ?.apply { toggleSearchable() }
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    site.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSearchSiteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("确定", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}