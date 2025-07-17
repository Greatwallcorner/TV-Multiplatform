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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.corner.bean.Suggest
import com.corner.ui.nav.vm.SearchViewModel
import com.corner.util.KtorClient
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay


@Composable
fun SearchBar(
    vm: SearchViewModel,
    modifier: Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    initValue: String,
    onSearch: (String) -> Unit,
    isSearching: Boolean,
    // 新增：焦点请求回调
    onFocusRequested: () -> Unit = { focusRequester.requestFocus() }
) {
    var focusState by remember { mutableStateOf<FocusState?>(null) }
    val modelState = vm.state.collectAsState()
    var textFieldValue  by remember { mutableStateOf(TextFieldValue(initValue)) }
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

    LaunchedEffect(focusRequester) {
        snapshotFlow { focusState?.isFocused == true && textFieldValue.text.isNotEmpty()}
            .collect { isFocused ->
                if (isFocused && textFieldValue.text.isNotEmpty()) {
                    delay(50) // 等待焦点稳定
                    textFieldValue = textFieldValue.copy(
                        selection = TextRange(0, textFieldValue.text.length)
                    )
                }
            }
    }

    val searchFun = fun(text: String) {
        onSearch(text)
        suggestions = Suggest()
    }

    Column {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { showSearchSiteDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(10.dp), // 圆角形状
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp), // 添加阴影
                modifier = Modifier.height(48.dp) // 固定高度与搜索框一致
            ) {
                Text(modelState.value.searchBarText, style = TextStyle(fontSize = 12.sp))
            }
            //隔离搜索网站按钮与搜索栏
            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(48.dp)
                    .background(
                        color = Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 8.dp),  // 内部padding
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusEvent { focusState = it }, // 捕获焦点状态变化
                    value = textFieldValue,
                    onValueChange = { newValue  ->
                        textFieldValue = newValue
                        searchText = newValue.text
                        if (job?.isActive == true) return@BasicTextField
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
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(Color.White),//白色输入光标
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 加载指示器
                            if (searching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            // 文本输入区域
                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                innerTextField()
                            }

                            // 搜索图标
                            if (searchText.isNotBlank()) {
                                IconButton(
                                    onClick = { searchFun(searchText) },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .pointerHoverIcon(PointerIcon.Default)//悬停在搜索图标上时，显示为普通光标
                                ) {
                                    Icon(
                                        tint = Color.White,
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search"
                                    )
                                }
                            }
                        }
                    }
                )
            }
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
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("全选")
                TriStateCheckbox(state = parentState.value, onClick = {
                    vm.updateModel {
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
                            vm.updateModel {
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
