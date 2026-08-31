package com.yifeplayte.wommo.activity.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeplayte.wommo.R
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun StatusCard(isActive: Boolean) {
    if (isActive) {
        ActiveCard()
    } else {
        InactiveCard()
    }
}

@Composable
private fun ActiveCard() {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        colors = CardDefaults.defaultColors(
            color = if (isSystemInDarkTheme()) Color(0xFF1A3825) else Color(0xFFDFFAE4)
        ),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(27.dp, 31.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Icon(
                    modifier = Modifier.size(110.dp),
                    imageVector = Icons.Rounded.CheckCircleOutline,
                    tint = Color(0xFF36D167),
                    contentDescription = null,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp, 14.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.module_status_active),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = stringResource(R.string.module_status_active_desc),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun InactiveCard() {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        BasicComponent(
            title = stringResource(R.string.module_status_inactive),
            summary = stringResource(R.string.module_status_inactive_desc),
            startAction = {
                Icon(
                    Icons.Rounded.ErrorOutline,
                    stringResource(R.string.module_status_inactive),
                    modifier = Modifier.padding(end = 16.dp),
                )
            },
        )
    }
}
