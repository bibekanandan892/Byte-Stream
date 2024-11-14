package com.bibek.bytestream.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    ButtonRow(
                        buttons = listOf(
                            ButtonData("Download", onDownloadClick),
                            ButtonData("Delete", onDeleteClick)
                        )
                    )
                }

                Status.QUEUED -> {
                    ButtonRow(
                        buttons = listOf(
                            ButtonData("Cancel", onCancelClick)
                        )
                    )
                }

                Status.STARTED, Status.IN_PROGRESS -> {
                    ButtonRow(
                        buttons = listOf(
                            ButtonData("Pause", onPauseClick),
                            ButtonData("Cancel", onCancelClick)
                        )
                    )
                }

                Status.PAUSED -> {
                    ButtonRow(
                        buttons = listOf(
                            ButtonData("Resume", onResumeClick),
                            ButtonData("Cancel", onCancelClick)
                        )
                    )
                }

                Status.CANCELLED -> {
                    ButtonRow(
                        buttons = listOf(
                            ButtonData("Retry", onRetryClick),
                            ButtonData("Delete", onDeleteClick)
                        )
                    )
                }

                Status.SUCCESS -> {
                    ButtonRow(
                        buttons = listOf(
                            ButtonData("Delete", onDeleteClick)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ButtonRow(buttons: List<ButtonData>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        buttons.forEach { buttonData ->
            Button(
                onClick = buttonData.onClick,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = buttonData.text, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(5.dp))
        }
    }
}

data class ButtonData(
    val text: String,
    val onClick: () -> Unit
)
