package com.corner.ui.decompose.component

import com.corner.ui.decompose.SearchComponent


class DefaultSearchPageComponent(private val searchComponent: DefaultSearchComponent):SearchComponent by searchComponent {
    fun getSearchComponent(): DefaultSearchComponent = searchComponent
}