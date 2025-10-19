package com.corner.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.corner.catvodcore.bean.Vod
import com.corner.catvodcore.config.ApiConfig
import com.corner.database.Db
import com.corner.database.entity.History
import kotlinx.coroutines.flow.Flow


@Dao
interface HistoryDao {

    /**
     * key = KEY@@@ID@@@CID
     */
    @Insert
    suspend fun save(history: History)

    suspend fun create(vod:Vod, flag:String, vodRemarks: String): History {
        val historyKey = vod.site?.key + Db.SYMBOL + vod.vodId + Db.SYMBOL + ApiConfig.api.cfg?.id!!
        var history = findHistory(historyKey)
        if(history == null){
            history = History(
                key = historyKey,
                vodPic = vod.vodPic ?: "",
                vodName = vod.vodName!!,
                vodFlag = flag,
                vodRemarks = vodRemarks,
                episodeUrl = vod.vodPlayUrl!!,
                cid = ApiConfig.api.cfg?.id!!,
                createTime = System.currentTimeMillis(),
            )
            save(history)
        }
        return history
    }

    @Query("SELECT * FROM History where cid = :cId order by createTime desc")
    fun findAll(cId: Long?): Flow<List<History>>

    @Query("DELETE FROM History WHERE `key` in (:ids)")
    suspend fun deleteBatch(ids:List<String>): Int

    @Query("DELETE FROM History")
    suspend fun deleteAll():Int

    @Query("SELECT * FROM History WHERE `key` = :historyKey")
    suspend fun findHistory(historyKey: String): History?

    @Query("UPDATE History SET opening = :opening, ending = :ending where `key` = :key")
    suspend fun updateOpeningEnding(opening: Long, ending: Long, key: String)

    @Update
    suspend fun update(en: History)

//    fun updateSome(flag:String, vodRemarks:String, playUrl:String, position:Long, speed:Float,opening: Long, ending: Long, historyKey:String)
}

//class HistoryRepositoryImpl:HistoryRepository, KoinComponent{
//    private val database: Database by inject()
//    private val historyQueries = database.historyQueries
//
//    /**
//     * key = KEY@@@ID@@@CID
//     */
//    override fun save(key: String,
//                      vodPic:String,
//                      vodName: String,
//                      vodFlag: String,
//                      vodRemarks:String,
//                      episodeUrl: String,
//                      cid: Long) {
//        historyQueries.save(
//            key,
//            vodPic,
//            vodName,
//            vodFlag,
//            vodRemarks,
//            episodeUrl,
//            cid
//        )
//    }
//
//    override fun create(vod:Vod, flag:String, vodRemarks: String){
//        val historyKey = vod.site?.key + Db.SYMBOL + vod.vodId + Db.SYMBOL + ApiConfig.api.cfg.value?.id!!
//        val his = historyQueries.findByKey(historyKey).executeAsOneOrNull()
//        if(his == null){
//            save(historyKey,
//                vod.vodPic ?:"",
//                vod.vodName!!,
//                flag,
//                vodRemarks,
//                vod.vodPlayUrl!!,
//                ApiConfig.api.cfg.value?.id!!)
//        }/*else{
//            historyQueries.updateSome(flag, vodRemarks, vod.vodPlayUrl,  historyKey)
//        }*/
//    }
//
//    override fun findAll(cId: Long?): List<History> {
//        if(cId == null) return listOf()
//       return historyQueries.getAll(cId).executeAsList()
//    }
//
//    override fun deleteBatch(ids: List<String>):Boolean {
//        if(ids.isEmpty()) return true
//        try {
//            historyQueries.deleteBatch(ids)
//        } catch (e: Exception) {
//            return false
//        }
//        return true
//    }
//
//    override fun deleteAll(): Boolean {
//        val id = ApiConfig.api.cfg.value?.id
//        if(id != null){
//            historyQueries.deleteAll(id)
//        }
//        return true
//    }
//
//    override fun findHistory(historyKey: String): History? {
//        return historyQueries.findByKey(historyKey).executeAsOneOrNull()
//    }
//
//    override fun updateOpeningEnding(opening: Long, ending: Long, key: String) {
//        historyQueries.updateOpeningEnding(opening = opening, ending = ending, key)
//    }
//
//    override fun updateSome(flag: String, vodRemarks: String, playUrl: String, position: Long,speed:Float, opening: Long, ending: Long, historyKey: String) {
//        historyQueries.updateSome(flag, vodRemarks, playUrl, position,speed.toDouble(),opening, ending, historyKey)
//    }
//
//}


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




