package com.corner.quickjs.method

import com.corner.bean.LocalCache
import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import org.apache.commons.lang3.StringUtils
import org.mozilla.javascript.annotations.JSFunction
import java.util.concurrent.atomic.AtomicReference

class Local {
    private fun getKey(rule: String, key: String): String {
        return "cache_" + (if (StringUtils.isEmpty(rule)) "" else rule + "_") + key
    }

    @JSFunction
    fun get(rule: String, key: String): String {
        val s = AtomicReference("")
        SettingStore.doWithCache { map ->
            val localCache: LocalCache = map[SettingType.LOCAL_CACHE.id] as LocalCache
            val cache: String = localCache.get(getKey(rule, key))
            s.set(cache)
        }
        return s.get()
    }

    @JSFunction
    fun set(rule: String, key: String, value: String) {
        SettingStore.doWithCache { map ->
            val localCache: LocalCache = map.get(SettingType.LOCAL_CACHE.id) as LocalCache
            if (localCache == null) map[SettingType.LOCAL_CACHE.id] = LocalCache()
            val cache: LocalCache = map[getKey(rule, key)] as LocalCache
            cache.add(getKey(rule, key), value)
        }
    }

    @JSFunction
    fun delete(rule: String, key: String) {
        SettingStore.doWithCache { map ->
            val localCache: LocalCache = map.get(SettingType.LOCAL_CACHE.id) as LocalCache
            if (localCache == null) map.put(SettingType.LOCAL_CACHE.id, LocalCache())
            val cache: LocalCache = map.get(getKey(rule, key)) as LocalCache
            cache.del(getKey(rule, key))
        }
    }
}
