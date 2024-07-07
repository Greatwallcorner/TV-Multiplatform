package com.corner.ui.video

import AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.corner.catvod.enum.bean.Vod
import com.seiko.imageloader.ui.AutoSizeImage

@Composable
fun HorizontalItem(modifier: Modifier, vod:Vod, onClick:(Vod)->Unit){
    Box(modifier.padding(8.dp)
        .background(MaterialTheme.colorScheme.primaryContainer,RoundedCornerShape(8.dp))
        .clip(RoundedCornerShape(8.dp))
//        .border(1.dp, Color.Gray,RoundedCornerShape(8.dp))
    ){
        Row(Modifier.padding(8.dp).fillMaxSize()) {
            AutoSizeImage(url = vod.vodPic ?: "",
                modifier = Modifier/*.height(150.dp).width(130.dp)*/,
                contentDescription = vod.vodName,
                contentScale = ContentScale.Fit,
                placeholderPainter = { painterResource("/pic/empty.png") },
                errorPainter = { painterResource("/pic/empty.png") })
            Spacer(Modifier.size(15.dp))
            Text(vod.vodName ?: "", modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@androidx.compose.desktop.ui.tooling.preview.Preview
@Composable
fun previewHorizonTaoItem(){
    AppTheme {
        val v = Vod(vodName = "testedahdkasjfdkladjflkadfdsf")
        HorizontalItem(Modifier.height(80.dp), v){}
    }
}