package com.corner.ui.scene

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BackRow(modifier: Modifier, onClickBack:()->Unit,content: @Composable ()->Unit){
    Row(modifier = modifier/*.background(MaterialTheme.colorScheme.surface)*/.height(80.dp).fillMaxWidth().padding(start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            modifier = Modifier.align(Alignment.CenterVertically),
            onClick = { onClickBack() }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "back to video home",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        content()
    }
}