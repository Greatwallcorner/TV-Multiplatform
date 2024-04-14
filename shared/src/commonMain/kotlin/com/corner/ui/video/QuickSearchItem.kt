package com.corner.ui.video

import AppTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import com.corner.ui.scene.ToolTipText

@Composable
fun QuickSearchItem(vod:Vod, onClick:()->Unit){
    Box(modifier = Modifier/*.background(MaterialTheme.colors.surface, shape = RoundedCornerShape(8.dp))*/
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = {onClick()})
        .padding(horizontal = 15.dp, vertical = 10.dp)){
        Column {
            ToolTipText(vod.vodName?:"",
                TextStyle(fontWeight = FontWeight.Bold, fontSize = TextUnit(18f, TextUnitType.Sp)))
            Text(vod.site?.name ?: "", fontSize = TextUnit(15f, TextUnitType.Sp)/*,color = MaterialTheme.colors.onSurface*/)
            ToolTipText(vod.vodRemarks ?: "", TextStyle(fontSize = TextUnit(15f, TextUnitType.Sp)/*, color = MaterialTheme.colors.onSurface)*/))
        }
    }
}

@Composable
@Preview
fun previewQuickSearchItem(){
    AppTheme {
        val vod =
            Vod(vodName = "teseeeeeeeeeeet", site = Site(name = "OK5555", key = "key", type = 1, api = "yyy"), vodRemarks = "kkkjj")
        QuickSearchItem(vod, {})
    }
}