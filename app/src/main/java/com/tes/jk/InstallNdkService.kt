package com.tes.jk

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class InstallNdkService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_PROGRESS = "com.tes.jk.INSTALL_PROGRESS"
        const val ACTION_FINISHED = "com.tes.jk.INSTALL_FINISHED"
        const val EXTRA_PROGRESS = "progress"
        const val NOTIF_ID = 1002
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.data ?: return START_NOT_STICKY
        val destDirPath = intent.getStringExtra("dest_dir") ?: return START_NOT_STICKY

        startForeground(NOTIF_ID, createNotification("Preparing...", 0))

        serviceScope.launch {
            try {
                val destDir = File(destDirPath)
                if (destDir.exists()) destDir.deleteRecursively()
                destDir.mkdirs()

                contentResolver.openInputStream(uri)?.let { inputStream ->
                    BufferedInputStream(inputStream).use { bis ->
                        ZipInputStream(bis).use { zis ->
                            var entry = zis.nextEntry
                            var totalExtracted = 0
                            var currentProgress = 0
                            val estimatedTotal = 500

                            while (entry != null) {
                                val file = File(destDir, entry.name)
                                if (file.canonicalPath.startsWith(destDir.canonicalPath)) {
                                    if (entry.isDirectory) {
                                        file.mkdirs()
                                    } else {
                                        file.parentFile?.mkdirs()
                                        FileOutputStream(file).use { out -> zis.copyTo(out) }
                                        totalExtracted++
                                        val newProgress = ((totalExtracted.toFloat() / estimatedTotal) * 100).toInt().coerceAtMost(100)
                                        if (newProgress > currentProgress) {
                                            currentProgress = newProgress
                                            broadcastProgress(currentProgress)
                                            updateNotification("Extracting... $totalExtracted files", currentProgress)
                                        }
                                    }
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }
                    }
                }

                // Beri hak eksekusi pada seluruh folder NDK (agar ndk-build bisa dijalankan)
                try {
                    Runtime.getRuntime().exec(arrayOf("sh", "-c", "chmod -R 755 ${destDir.absolutePath}")).waitFor()
                } catch (e: Exception) {
                    // kalau gagal pun tidak masalah, karena build via shell
                }

                broadcastFinished()
                // Notification selesai
                val succNotif = NotificationCompat.Builder(this@InstallNdkService, MainActivity.CHANNEL_INSTALL)
                    .setSmallIcon(android.R.drawable.ic_menu_upload)
                    .setContentTitle("NDK Installed")
                    .setContentText("Successfully installed.")
                    .setProgress(0, 0, false)
                    .build()
                (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager).notify(NOTIF_ID, succNotif)

            } catch (e: Exception) {
                broadcastFinished()
            } finally {
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun broadcastProgress(progress: Int) {
        sendBroadcast(Intent(ACTION_PROGRESS).putExtra(EXTRA_PROGRESS, progress))
    }

    private fun broadcastFinished() {
        sendBroadcast(Intent(ACTION_FINISHED))
    }

    private fun updateNotification(message: String, progress: Int) {
        val notif = NotificationCompat.Builder(this, MainActivity.CHANNEL_INSTALL)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentTitle(getString(R.string.installing_ndk))
            .setContentText(message)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager).notify(NOTIF_ID, notif)
    }

    private fun createNotification(message: String, progress: Int) = NotificationCompat.Builder(this, MainActivity.CHANNEL_INSTALL)
        .setSmallIcon(android.R.drawable.ic_menu_upload)
        .setContentTitle(getString(R.string.installing_ndk))
        .setContentText(message)
        .setProgress(100, progress, false)
        .setOngoing(true)
        .build()

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}