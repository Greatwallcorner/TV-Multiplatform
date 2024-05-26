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
    fun findHistory(historyKey: String): History?
    fun updateOpeningEnding(opening: Long, ending: Long, key: String)

    fun updateSome(flag:String, vodRemarks:String, playUrl:String, position:Long, speed:Float, historyKey:String)
}

class HistoryRepositoryImpl:HistoryRepository, KoinComponent{
    private val database: Database by inject()
    private val historyQueries = database.historyQueries

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
        val historyKey = vod.site?.key + Db.SYMBOL + vod.vodId + Db.SYMBOL + ApiConfig.api.cfg.value?.id!!
        val his = historyQueries.findByKey(historyKey).executeAsOneOrNull()
        if(his == null){
            save(historyKey,
                vod.vodPic ?:"",
                vod.vodName!!,
                flag,
                vodRemarks,
                vod.vodPlayUrl!!,
                ApiConfig.api.cfg.value?.id!!)
        }/*else{
            historyQueries.updateSome(flag, vodRemarks, vod.vodPlayUrl,  historyKey)
        }*/
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

    override fun findHistory(historyKey: String): History? {
        return historyQueries.findByKey(historyKey).executeAsOneOrNull()
    }

    override fun updateOpeningEnding(opening: Long, ending: Long, key: String) {
        historyQueries.updateOpeningEnding(opening = opening, ending = ending, key)
    }

    override fun updateSome(flag: String, vodRemarks: String, playUrl: String, position: Long,speed:Float, historyKey: String) {
        historyQueries.updateSome(flag, vodRemarks, playUrl, position,speed.toDouble(), historyKey)
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
    vod.site = ApiConfig.getSite(keySplit[0])
    return vod
}




