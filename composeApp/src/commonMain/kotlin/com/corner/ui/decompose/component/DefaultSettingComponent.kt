package com.corner.ui.decompose.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.corner.bean.Setting
import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.database.Db
import com.corner.database.entity.Config
import com.corner.ui.decompose.BaseComponent
import com.corner.ui.decompose.SettingComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DefaultSettingComponent(componentContext: ComponentContext):SettingComponent,BaseComponent(Dispatchers.IO), ComponentContext by componentContext {
    private val _model = MutableValue(SettingComponent.Model())

    override val model: MutableValue<SettingComponent.Model> = _model
    override fun sync() {
        _model.update { it.copy(settingList = SettingStore.getSettingList(), version = _model.value.version + 1) }
    }

    fun getConfigAll() {
        scope.launch {
            val flow = Db.Config.getAll()
            flow.collect( {
                li ->
                _model.update { it.copy(dbConfigList = li.toMutableList()) }
            }
            )
        }
    }

    fun deleteHistoryById(config: Config) {
        scope.launch {
            Db.Config.deleteById(config.id)
            val dbConfigList = _model.value.dbConfigList
            dbConfigList.remove(config)
            _model.update { it.copy(dbConfigList = dbConfigList) }
        }
    }
}

fun List<Setting>.getSetting(type:SettingType):Setting?{
    return this.find { it.id == type.id }
}