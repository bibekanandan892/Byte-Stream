package com.bibek.bytestream

import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import com.bibek.bytestream.component.DownloadItem
import com.bibek.bytestream.component.SampleButtonLayout
import com.bibek.bytestream.internal.utils.Status
import com.bibek.bytestream.ui.theme.ByteStreamTheme
import java.io.File
import java.util.UUID

class DownloadActivity : ComponentActivity() {
    private lateinit var byteStream: ByteStream
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        byteStream = (this.applicationContext as DownloadApplication).byteStream
        val downloadListFlow = byteStream.observeDownloads()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        var permissions = arrayOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !getSystemService(NotificationManager::class.java).areNotificationsEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions = permissions.plus(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val getpermission = Intent()
            getpermission.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            startActivity(getpermission)
        } else if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                permissions = permissions.plus(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(permissions, 101)
            Toast.makeText(this, "Notification and Storage Permission Required", Toast.LENGTH_SHORT)
                .show()
            return
        }
        enableEdgeToEdge()
        setContent {
            ByteStreamTheme {
                val downloadFlowList = downloadListFlow.collectAsState(emptyList())
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        SampleButtonLayout(
                            sample1OnClick = {
                                byteStream.download(
                                    url = "https://sample-videos.com/video321/mp4/480/big_buck_bunny_480p_20mb.mp4",
                                    path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
                                    fileName = "Video${
                                        UUID.randomUUID().toString().substring(0, 10)
                                    }.mp4",
                                    tag = "Video",
                                    metaData = "158"
                                )
                            }
                        )
                        LazyColumn {
                            items(downloadFlowList.value.size) { index ->

                                val downloadItem = downloadFlowList.value[index]
                                DownloadItem(
                                    fileName = downloadItem.fileName,
                                    progressPercentage = "${downloadItem.progress}%",
                                    fileSize = Util.getCompleteText(
                                        downloadItem.speedInBytePerMs,
                                        downloadItem.progress,
                                        downloadItem.total
                                    ) + " " + Util.getTotalLengthText(downloadItem.total),
                                    downloadProgress = downloadItem.progress / 100f,
                                    downloadStatus = downloadItem.status,
                                    onDownloadClick = {
                                        byteStream.download(
                                            url = downloadItem.url,
                                            fileName = downloadItem.fileName,
                                            path = downloadItem.path,
                                            tag = downloadItem.tag,
                                            metaData = downloadItem.metaData
                                        )
                                    },
                                    onCancelClick = {
                                        byteStream.cancel(downloadItem.id)
                                    },
                                    onPauseClick = {
                                        byteStream.pause(downloadItem.id)
                                    },
                                    onResumeClick = {
                                        byteStream.resume(downloadItem.id)
                                    },
                                    onDeleteClick = {
                                        byteStream.clearDb(downloadItem.id)
                                    },
                                    onRetryClick = {
                                        byteStream.retry(downloadItem.id)
                                    },
                                    onFileClick = {
                                        if (downloadItem.status == Status.SUCCESS) {
                                            val file =
                                                File(downloadItem.path, downloadItem.fileName)
                                            if (file.exists()) {
                                                val uri =
                                                    this@DownloadActivity.applicationContext?.let {
                                                        FileProvider.getUriForFile(
                                                            it,
                                                            it.packageName + ".provider",
                                                            file
                                                        )
                                                    }
                                                if (uri != null) {
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(
                                                            uri,
                                                            this@DownloadActivity.contentResolver.getType(
                                                                uri
                                                            )
                                                        )
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    try {
                                                        startActivity(intent)
                                                    } catch (_: Exception) {

                                                    }
                                                }
                                            } else {
                                                Toast.makeText(
                                                    this@DownloadActivity,
                                                    "Something went wrong",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
