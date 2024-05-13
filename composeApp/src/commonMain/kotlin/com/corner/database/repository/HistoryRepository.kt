package com.corner.database.repository

import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.config.ApiConfig
import com.corner.database.Database
import com.corner.database.Db
import com.corner.database.History
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface HistoryRepository {
    fun save(key: String,
             vodPic:String,
             vodName: String,
             vodFlag: String,
             vodRemarks:String,
             episodeUrl: String,
             cid: Long);

    fun create(vod:Vod, flag:String, vodRemarks: String)

    fun findAll(cId: Long?): List<History>

    fun deleteBatch(ids:List<String>): Boolean

    fun deleteAll():Boolean
}

class HistoryRepositoryImpl:HistoryRepository, KoinComponent{
    private val database: Database by inject()
    private val historyQueries = database.historyQueries

    private fun getHistoryKey(key:String, id:String, cId:String): String {
        return key + Db.SYMBOL + id + Db.SYMBOL + ApiConfig.api.id
    }

    /**
     * key = KEY@@@ID@@@CID
     */
    override fun save(key: String,
                      vodPic:String,
                      vodName: String,
                      vodFlag: String,
                      vodRemarks:String,
                      episodeUrl: String,
                      cid: Long) {
        historyQueries.save(
            key,
            vodPic,
            vodName,
            vodFlag,
            vodRemarks,
            episodeUrl,
            cid
        )
    }

    override fun create(vod:Vod, flag:String, vodRemarks: String){
        val historyKey = vod.site?.key + Db.SYMBOL + vod.vodId + Db.SYMBOL + ApiConfig.api.id
        val his = historyQueries.findByKey(historyKey).executeAsOneOrNull()
        if(his == null){
            save(historyKey,
                vod.vodPic ?:"",
                vod.vodName!!,
                flag,
                vodRemarks,
                vod.vodPlayUrl!!,
                ApiConfig.api.cfg.value?.id!!)
        }else{
            historyQueries.updateSome(flag, vodRemarks, vod.vodPlayUrl, historyKey)
        }
    }

    override fun findAll(cId: Long?): List<History> {
        if(cId == null) return listOf()
       return historyQueries.getAll(cId).executeAsList()
    }

    override fun deleteBatch(ids: List<String>):Boolean {
        if(ids.isEmpty()) return true
        try {
            historyQueries.deleteBatch(ids)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    override fun deleteAll(): Boolean {
        val id = ApiConfig.api.cfg.value?.id
        if(id != null){
            historyQueries.deleteAll(id)
        }
        return true
    }

}


fun History.getSiteKey():String{
    return key.split(Db.SYMBOL)[0]
}

fun History.buildVod():Vod{
    val keySplit = key.split(Db.SYMBOL)
    val vod = Vod()
    vod.vodName = vodName
    vod.vodId = keySplit[1]
    vod.vodPic = vodPic
    vod.vodRemarks = vodRemarks
    return vod
}




