package com.corner.catvod.enum.bean

import com.corner.catvodcore.bean.Episode
import com.corner.catvodcore.bean.Flag
import com.corner.database.History
import com.corner.util.Constants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Vod(
    @SerialName("vod_id") var vodId: String = "",
    @SerialName("vod_name") var vodName: String? = null,
    @SerialName("type_name") var typeName: String? = null,
    @SerialName("vod_pic") var vodPic: String? = null,
    @SerialName("vod_remarks") var vodRemarks: String? = null,
    @SerialName("vod_year") var vodYear: String? = null,
    @SerialName("vod_area") var vodArea: String? = null,
    @SerialName("vod_director") var vodDirector: String? = null,
    @SerialName("vod_actor") var vodActor: String? = null,
    @SerialName("vod_content") var vodContent: String? = null,
    @SerialName("vod_play_from") var vodPlayFrom: String? = null,
    @SerialName("vod_play_url") var vodPlayUrl: String? = null,
    @SerialName("vod_tag") var vodTag: String? = null,
    @SerialName("cate") var cate: String? = null,
    @SerialName("style") var style: String? = null,
    @SerialName("land") var land: String? = null,
    @SerialName("circle") var circle: String? = null,
    @SerialName("ratio") var ratio: String? = null,
    @Transient
    var vodFlags: MutableList<Flag?> = mutableListOf(),
    @Transient
    var site: Site? = null,
    @Transient
    var currentFlag: Flag? = null,
    @Transient
    var subEpisode: MutableList<Episode>? = mutableListOf(),
    @Transient
    var currentTabIndex: Int = 0,
) {
    companion object {

        fun Vod.getEpisode():Episode?{
            return subEpisode?.find { it.activated }
        }
        fun Vod.isEmpty():Boolean{
            return org.apache.commons.lang3.StringUtils.isBlank(vodId) || vodFlags.isEmpty()
        }
        fun Vod.setCurrentFlag(idx: Int) {
            currentFlag = vodFlags[idx]
            currentFlag?.activated = true
        }

        fun Vod.setCurrentFlag(flag: Flag?) {
            currentFlag = flag
            currentFlag?.activated = true
        }

        fun Vod.setVodFlags() {
            val playFlags: List<String>? = vodPlayFrom?.split("\\$\\$\\$".toRegex())
            val playUrls: List<String>? = vodPlayUrl?.split("\\$\\$\\$".toRegex())

            if (!playFlags.isNullOrEmpty() && !playUrls.isNullOrEmpty()) {
                for (i in 0 until playFlags.size) {
                    if (playFlags[i].isEmpty() || i >= playUrls.size) continue
                    val item = Flag.create(playFlags[i].trim())
                    item.createEpisode(playUrls[i])
                    vodFlags.add(item)
                }
            }
            for (item in vodFlags) {
                if (item?.urls == null) continue
                item.createEpisode(item.urls)
            }
            setCurrentFlag(0)
        }

        fun List<Episode>.getPage(index: Int): MutableList<Episode> {
            val list = this.subList(
                index * Constants.EpSize,
                if (index * Constants.EpSize + Constants.EpSize > size) size else index * Constants.EpSize + Constants.EpSize
            ).toMutableList()
            return list
        }
    }

    fun isFolder():Boolean{
        return VodTag.Folder.called == vodTag
    }

    fun findAndSetEpByName(history: History): Episode? {
        if (history.vodRemarks.isNullOrBlank()) return null
            currentFlag = vodFlags.find { it?.flag == history.vodFlag }
            val episode = currentFlag?.find(history.vodRemarks, true)
            if(episode != null){
                episode.activated = true
                val indexOf = currentFlag?.episodes?.indexOf(episode)
                // 32 15 16
                currentTabIndex = (indexOf?.plus(1))!! / Constants.EpSize
                subEpisode = currentFlag?.episodes?.getPage(currentTabIndex)!!
            }
            return episode
    }

    fun nextFlag():Flag?{
        val find = vodFlags.find { it?.activated ?: false }
        val indexOf = vodFlags.indexOf(find)
        if(indexOf + 1 >= vodFlags.size) return null
        val flag = vodFlags[indexOf + 1]
        vodFlags.forEach{
            it?.activated = flag?.flag == it?.flag
        }
//        flag.episodes.indexOf()
        return flag
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vod

        if (vodId != other.vodId) return false
        if (vodName != other.vodName) return false
        if (typeName != other.typeName) return false
        if (vodPic != other.vodPic) return false
        if (vodRemarks != other.vodRemarks) return false
        if (vodYear != other.vodYear) return false
        if (vodArea != other.vodArea) return false
        if (vodDirector != other.vodDirector) return false
        if (vodActor != other.vodActor) return false
        if (vodContent != other.vodContent) return false
        if (vodPlayFrom != other.vodPlayFrom) return false
        if (vodPlayUrl != other.vodPlayUrl) return false
        if (vodTag != other.vodTag) return false
        if (cate != other.cate) return false
        if (style != other.style) return false
        if (land != other.land) return false
        if (circle != other.circle) return false
        if (ratio != other.ratio) return false
        if (vodFlags != other.vodFlags) return false
        if (site != other.site) return false
        if (currentFlag != other.currentFlag) return false
        if (subEpisode != other.subEpisode) return false
        if (currentTabIndex != other.currentTabIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vodId.hashCode()
        result = 31 * result + (vodName?.hashCode() ?: 0)
        result = 31 * result + (typeName?.hashCode() ?: 0)
        result = 31 * result + (vodPic?.hashCode() ?: 0)
        result = 31 * result + (vodRemarks?.hashCode() ?: 0)
        result = 31 * result + (vodYear?.hashCode() ?: 0)
        result = 31 * result + (vodArea?.hashCode() ?: 0)
        result = 31 * result + (vodDirector?.hashCode() ?: 0)
        result = 31 * result + (vodActor?.hashCode() ?: 0)
        result = 31 * result + (vodContent?.hashCode() ?: 0)
        result = 31 * result + (vodPlayFrom?.hashCode() ?: 0)
        result = 31 * result + (vodPlayUrl?.hashCode() ?: 0)
        result = 31 * result + (vodTag?.hashCode() ?: 0)
        result = 31 * result + (cate?.hashCode() ?: 0)
        result = 31 * result + (style?.hashCode() ?: 0)
        result = 31 * result + (land?.hashCode() ?: 0)
        result = 31 * result + (circle?.hashCode() ?: 0)
        result = 31 * result + (ratio?.hashCode() ?: 0)
        vodFlags.forEach { result += 31 * result + it.hashCode() }
        result = 31 * result + (site?.hashCode() ?: 0)
        result = 31 * result + (currentFlag?.hashCode() ?: 0)
        result = 31 * result + (subEpisode?.hashCode() ?: 0)
        result = 31 * result + currentTabIndex
        return result
    }
}

enum class VodTag(val called: String) {
    Folder("folder"),
    File("file")
}
