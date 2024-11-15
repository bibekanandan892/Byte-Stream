

[![](https://jitpack.io/v/bibekanandan892/Byte-Stream.svg)](https://jitpack.io/#bibekanandan892/Byte-Stream)

# Byte-Stream

## A Feature-Rich Android File Downloading Library in Kotlin with Parallel Download and Pause/Resume Capabilities

<p align="center">
  <img src="https://github.com/user-attachments/assets/d048d1d6-6eb6-4d42-9811-726f8fd23fda" alt="Byte-Stream Logo">
</p>

### About Byte-Stream

Byte-Stream is a robust and customizable file downloading library for Android written entirely in Kotlin. It simplifies the file downloading process in Android applications by providing extensive support for parallel downloads, pause/resume capabilities, and fine-grained download control.

<p align="center">
  <img height="500" alt = "High level design" src=https://github.com/user-attachments/assets/c0620ee3-4672-4604-96ce-4472c3f30ad6>
</p>

---

### Key Features of Byte-Stream

- **Versatile Downloads**: Supports various file types (e.g., jpg, png, gif, mp4, mp3, pdf).
- **Pause and Resume**: Download files with the flexibility to pause and resume tasks.
- **Multiple Parallel Downloads**: Download files simultaneously with configurable parts.
- **Customizable Notifications**: Receive notifications with download progress, speed, and time left.
- **Observable Progress**: Track download status through Flow.
- **Error Handling**: Retry failed downloads or cancel them directly.

---

### Installation

1. Add JitPack repository to your `settings.gradle`:
   ```groovy
   dependencyResolutionManagement {
       repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
       repositories {
           google()
           mavenCentral()
           maven { url 'https://jitpack.io' } // Add JitPack
       }
   }
   ```

2. Add Byte-Stream dependency in your `build.gradle` file:
   ```groovy
   dependencies {
       implementation 'com.github.bibekanandan892:Byte-Stream:1.0.0' // Replace with latest version
   }
   ```

---

### Usage Guide

#### Basic Setup

Initialize Byte-Stream in your Application `onCreate()` method:

```kotlin
class DownloadApplication : Application() {

    lateinit var byteStream: ByteStream

    override fun onCreate() {
        super.onCreate()
        byteStream = ByteStream.create(applicationContext) 
    }
}
```

#### Starting a Download

```kotlin
private lateinit var byteStream: ByteStream
 byteStream.download(
      url = "https://sample-videos.com/video321/mp4/480/big_buck_bunny_480p_20mb.mp4",
      path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
      fileName = "Video${UUID.randomUUID().toString().substring(0, 10)}.mp4",
      tag = "Video",
      metaData = "158"
      )

```

#### Download Control Options

- **Cancel**: Cancel a download using its ID or tag:
  ```kotlin
  byteStream.cancel(downloadId)  // Cancel by ID
  byteStream.cancel(tag)         // Cancel by tag
  byteStream.cancelAll()         // Cancel all downloads
  ```

- **Pause**: Pause a download using its ID or tag:
  ```kotlin
  byteStream.pause(downloadId)  // Pause by ID
  byteStream.pause(tag)         // Pause by tag
  byteStream.pauseAll()         // Pause all downloads
  ```

- **Resume**: Resume a paused download using its ID or tag:
  ```kotlin
  byteStream.resume(downloadId)  // Resume by ID
  byteStream.resume(tag)         // Resume by tag
  byteStream.resumeAll()         // Resume all downloads
  ```

- **Retry**: Retry a failed download using its ID or tag:
  ```kotlin
  byteStream.retry(downloadId)  // Retry by ID
  byteStream.retry(tag)         // Retry by tag
  byteStream.retryAll()         // Retry all failed downloads
  ```

- **Delete**: Delete a download from the database (optionally, skip file deletion):
  ```kotlin
  byteStream.clearDb(downloadId)               // Delete entry and file by ID
  byteStream.clearDb(downloadId, false)        // Delete only the entry, not the file
  byteStream.clearDb(tag)                      // Delete entries by tag
  byteStream.clearAllDb()                      // Clear all entries
  byteStream.clearDb(timeInMillis)             // Delete entries older than specified time
  ```


#### Notification Configuration

Enable notifications by setting up in the Byte-Stream builder:

```kotlin
class DownloadApplication : Application() {

    lateinit var byteStream: ByteStream

    override fun onCreate() {
        super.onCreate()
        byteStream = ByteStream.create(applicationContext) {
            configureNotification {
                enabled = true
                smallIcon = R.drawable.download_icon
            }
        }
    }
}
```
Important Note: Add `FOREGROUND_SERVICE_DATA_SYNC`, `WAKE_LOCK`, and `INTERNET` permissions. Check out the sample app for reference.

### Advanced Customization

- **Custom Headers**: Provide custom headers for download requests.
- **Parallel Download Parts**: Set the number of parts for parallel download.
- **Custom Timeout**: Configure connection and read timeout values.

---

### TODO

- **Multi-part download.
- **Many bug fixes.
- 
---


### CREDITS

- **[Ketch](https://github.com/khushpanchal/Ketch)** - The design and functionality of Byte-Stream were inspired by Ketch, which provided valuable insights into structuring efficient downloading features.

- **[PRDownloader](https://github.com/amitshekhariitbhu/PRDownloader)** - A special mention to [Amit Shekhar](https://github.com/amitshekhariitbhu), whose work on PRDownloader has been a significant learning resource. From his library, I gained a deep understanding of file download processes and best practices in Android development, which contributed to the development of Byte-Stream.

---

### License

```
  Copyright (C) 2024 Bibekananda Nayak

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
