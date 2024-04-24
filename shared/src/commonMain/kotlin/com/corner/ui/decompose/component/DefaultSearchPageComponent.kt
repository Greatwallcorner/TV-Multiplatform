package com.corner.ui.decompose.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.corner.bean.SettingStore.getHistoryList
import com.corner.catvodcore.bean.Collect
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.SearchComponent


class DefaultSearchPageComponent(componentContext:ComponentContext):SearchComponent, ComponentContext by componentContext {

    private val _model = MutableValue(SearchComponent.Model(hotList = GlobalModel.hotList.value, historyList = getHistoryList()))

    override val model: MutableValue<SearchComponent.Model> = _model

    override fun search(searchText: String, isLoadMore: Boolean) {
    }

    override fun clear() {
    }

    override fun onClickCollection(item: Collect) {

    }

}