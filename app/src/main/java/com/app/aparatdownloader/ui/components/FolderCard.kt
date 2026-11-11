package com.app.aparatdownloader.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.aparatdownloader.ui.theme.*

@Composable
fun FolderCard(
    title: String,
    level: String,
    subfoldersCount: Int,
    videosCount: Int,
    progressPercent: Int,
    onEnterFolder: () -> Unit,
    onDownloadAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Level Badge & DRM status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = PrimaryLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "سطح $level • کدگذاری DRM",
                        color = PrimaryDark,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "$subfoldersCount زیرپوشه تخصصی", fontSize = 12.sp, color = TextSecondary)
                Text(text = "$videosCount درس تصویری", fontSize = 12.sp, color = TextSecondary)
                Text(text = "$progressPercent% تکمیل شده", fontSize = 12.sp, color = Primary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Primary,
                trackColor = SurfaceContainerLow
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDownloadAll,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Primary)
                ) {
                    Text("دانلود یکجای کل پوشه", color = Primary, fontSize = 13.sp)
                }
                Button(
                    onClick = onEnterFolder,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerLow)
                ) {
                    Text("ورود به پوشه", color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}
