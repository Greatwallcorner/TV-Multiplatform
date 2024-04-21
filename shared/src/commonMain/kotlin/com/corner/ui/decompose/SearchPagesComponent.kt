package com.corner.ui.decompose

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value


interface SearchPagesComponent {
    @OptIn(ExperimentalDecomposeApi::class)
    val pages: Value<ChildPages<*, SearchComponent>>

    fun selectPage(index:Int)

    val model:Value<Model>

    fun onSearch(keyword: String)

    data class Model(
        val keyword:String,
        var isSearching:Boolean = false,
    )
}