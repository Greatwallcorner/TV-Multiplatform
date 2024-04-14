package com.corner.catvodcore.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
class Type {
    constructor(id:String, name:String){
        this.typeId = id
        this.typeName = name
    }

    @SerialName("type_id")
    var typeId: String = ""

    @SerialName("type_name")
    var typeName: String = ""

    @Transient
    var selected:Boolean = false

    companion object{
        fun home():Type{
            val type = Type("home", "推荐")
            type.selected = true
            return type
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Type

        if (typeId != other.typeId) return false
        if (typeName != other.typeName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = typeId.hashCode()
        result = 31 * result + typeName.hashCode()
        return result
    }
}