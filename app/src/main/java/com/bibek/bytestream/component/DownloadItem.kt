package com.bibek.bytestream.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibek.bytestream.R
import com.bibek.bytestream.internal.utils.Status

@Composable
fun DownloadItem(
    fileName: String,
    progressPercentage: String,
    fileSize: String,
    downloadProgress: Float,
    downloadStatus: String,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRetryClick: () -> Unit,
    onFileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clickable(onClick = onFileClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.onSecondary)
                .padding(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 5.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = progressPercentage,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = fileSize,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { downloadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = downloadStatus,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 5.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            when (downloadStatus) {
                Status.DEFAULT, Status.FAILED -> {
                    IconButtonRow(
                        icons = listOf(
                            IconButtonData(
                                icon = R.drawable.reload_icon,
                                onClick = onDownloadClick,
                                text = "Retry"
                            ),
                            IconButtonData(
                                icon = R.drawable.delete_icon,
                                onClick = onDeleteClick,
                                text = "Delete"
                            )
                        )
                    )
                }

                Status.QUEUED -> {
                    IconButtonRow(
                        icons = listOf(
                            IconButtonData(
                                icon = R.drawable.cancel_icon,
                                onClick = onCancelClick,
                                text = "Cancel"
                            )
                        )
                    )
                }

                Status.STARTED, Status.IN_PROGRESS -> {
                    IconButtonRow(
                        icons = listOf(
                            IconButtonData(
                                icon = R.drawable.pause_icon,
                                onClick = onPauseClick,
                                text = "Pause"
                            ),
                            IconButtonData(
                                icon = R.drawable.cancel_icon,
                                onClick = onCancelClick,
                                text = "Cancel"
                            )
                        )
                    )
                }

                Status.PAUSED -> {
                    IconButtonRow(
                        icons = listOf(
                            IconButtonData(
                                icon = R.drawable.download_icon,
                                onClick = onResumeClick,
                                text = "Resume"
                            ),
                        )
                    )
                }

                Status.CANCELLED -> {
                    IconButtonRow(
                        icons = listOf(
                            IconButtonData(
                                icon = R.drawable.reload_icon,
                                onClick = onRetryClick, text = "Retry"
                            ),
                            IconButtonData(
                                icon = R.drawable.delete_icon,
                                onClick = onDeleteClick,
                                text = "Delete"
                            )
                        )
                    )
                }

                Status.SUCCESS -> {
                    IconButtonRow(
                        icons = listOf(
                            IconButtonData(
                                icon = R.drawable.delete_icon,
                                onClick = onDeleteClick,
                                text = "Delete"
                            )
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun IconButtonRow(icons: List<IconButtonData>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically
    ) {
        icons.forEach { iconButtonData ->
            Column(
                modifier = Modifier.clickable(onClick = iconButtonData.onClick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                Icon(
                    painter = painterResource(iconButtonData.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (iconButtonData.text == "Cancel") 30.dp else 40.dp)
                )
                if (iconButtonData.text == "Cancel") {
                    Spacer(modifier = Modifier.height(7.dp))
                }
                Text(text = iconButtonData.text)
            }
            Spacer(modifier = Modifier.width(20.dp))
        }
    }
}


data class IconButtonData(
    val icon: Int,
    val text: String,
    val onClick: () -> Unit
)
