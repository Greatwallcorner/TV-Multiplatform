package com.corner.ui.decompose.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.pages.*
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.SearchComponent
import com.corner.ui.decompose.SearchPagesComponent
import kotlinx.serialization.Serializable

@OptIn(ExperimentalDecomposeApi::class)
class DefaultSearchPagesComponent(component: ComponentContext):SearchPagesComponent, ComponentContext by component{
    private val navigation = PagesNavigation<SearchPageType>()

    private val _model = MutableValue(SearchPagesComponent.Model(""))

    override val model: MutableValue<SearchPagesComponent.Model> = _model

    val sharedComponent = DefaultSearchComponent(component)

    override val pages: Value<ChildPages<*, SearchComponent>> =
        childPages(
            key = "DefaultSearchChild",
            serializer = SearchPageType.serializer(),
            source = navigation,
            initialPages = {Pages(items = listOf(SearchPageType.SearchPage, SearchPageType.SearchResult), selectedIndex = 0)},
            handleBackButton = true){ type,childComponentContext->
            when(type){
                is SearchPageType.SearchPage -> DefaultSearchPageComponent(sharedComponent)
                is SearchPageType.SearchResult -> sharedComponent
            }

        }

    override fun selectPage(index: Int) {
        navigation.select(index)
    }


    override fun onSearch(keyword: String) {
        GlobalModel.keyword.value = keyword
        navigation.selectNext(circular = true)
    }
    @Serializable
    sealed interface SearchPageType{
        data object SearchPage:SearchPageType
        data object SearchResult:SearchPageType
    }
}