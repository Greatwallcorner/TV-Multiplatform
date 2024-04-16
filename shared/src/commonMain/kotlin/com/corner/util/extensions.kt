package com.corner.util

import com.corner.catvod.enum.bean.Site

fun Site.isEmpty():Boolean{
    return key.isEmpty() || name.isEmpty()
}