package com.app.aparatdownloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.aparatdownloader.data.api.Playlist
import com.app.aparatdownloader.ui.common.toFa
import com.app.aparatdownloader.ui.theme.*

@Composable
fun CourseCard(playlist: Playlist, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // کاور: گرادیان پشت، تصویر روی آن (اگر نبود یا نیامد، گرادیان دیده می‌شود)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Brush.linearGradient(listOf(PrimaryDark, Accent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = Surface.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
                if (!playlist.cover_url.isNullOrBlank()) {
                    AsyncImage(
                        model = playlist.cover_url,
                        contentDescription = playlist.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 24.sp,
                    maxLines = 2
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = PrimaryLight, shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayCircle, null, tint = PrimaryDark, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${playlist.video_count ?: 0} درس تصویری".toFa(),
                                color = PrimaryDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Text("مشاهده دروس", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
