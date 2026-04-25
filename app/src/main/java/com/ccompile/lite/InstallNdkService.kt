package com.ccompile.lite

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
        const val ACTION_PROGRESS = "com.ccompile.lite.INSTALL_PROGRESS"
        const val ACTION_FINISHED = "com.ccompile.lite.INSTALL_FINISHED"
        const val EXTRA_MSG = "message"
        const val EXTRA_SUCCESS = "success"
        const val NOTIF_ID = 1002
        
        // PERBAIKAN: Flag global agar Fragment tahu saat kita pindah-pindah tab
        var isInstalling = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isInstalling) return START_NOT_STICKY // Jangan jalankan ganda
        isInstalling = true
        
        val uri = intent?.data
        val destDirPath = intent?.getStringExtra("dest_dir")
        
        if (uri == null || destDirPath == null) {
            isInstalling = false
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, createNotification("Starting extraction..."))

        serviceScope.launch {
            try {
                val destDir = File(destDirPath)
                if (destDir.exists()) destDir.deleteRecursively()
                destDir.mkdirs()

                var totalExtracted = 0

                contentResolver.openInputStream(uri)?.let { inputStream ->
                    BufferedInputStream(inputStream).use { bis ->
                        ZipInputStream(bis).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                val file = File(destDir, entry.name)
                                if (file.canonicalPath.startsWith(destDir.canonicalPath)) {
                                    if (entry.isDirectory) {
                                        file.mkdirs()
                                    } else {
                                        file.parentFile?.mkdirs()
                                        FileOutputStream(file).use { out -> zis.copyTo(out) }
                                        totalExtracted++
                                        
                                        // Broadcast real-time setiap kelipatan 100 agar UI tidak lag tapi tetap update
                                        if (totalExtracted % 100 == 0) {
                                            val msg = "Extracted $totalExtracted files..."
                                            broadcastProgress(msg)
                                            updateNotification(msg)
                                        }
                                    }
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }
                    }
                }

                val chmodMsg = "Applying permissions (chmod)..."
                broadcastProgress(chmodMsg)
                updateNotification(chmodMsg)

                try {
                    Runtime.getRuntime().exec(arrayOf("sh", "-c", "chmod -R 755 ${destDir.absolutePath}")).waitFor()
                } catch (e: Exception) {
                    // Abaikan jika device tidak support chmod via shell
                }

                broadcastFinished(true, "NDK Installed Successfully")
                showFinalNotification("Success", "NDK ready to use.")

            } catch (e: Exception) {
                broadcastFinished(false, "Error: ${e.message}")
                showFinalNotification("Failed", "Installation failed.")
            } finally {
                isInstalling = false
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun broadcastProgress(msg: String) {
        sendBroadcast(Intent(ACTION_PROGRESS).putExtra(EXTRA_MSG, msg))
    }

    private fun broadcastFinished(success: Boolean, msg: String) {
        sendBroadcast(Intent(ACTION_FINISHED).putExtra(EXTRA_SUCCESS, success).putExtra(EXTRA_MSG, msg))
    }

    private fun updateNotification(message: String) {
        val notif = NotificationCompat.Builder(this, MainActivity.CHANNEL_INSTALL)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentTitle(getString(R.string.installing_ndk))
            .setContentText(message)
            .setProgress(0, 0, true) // Mode Indeterminate (Putar terus tanpa persen)
            .setOngoing(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager).notify(NOTIF_ID, notif)
    }
    
    private fun showFinalNotification(title: String, message: String) {
        val notif = NotificationCompat.Builder(this, MainActivity.CHANNEL_INSTALL)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(false)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager).notify(NOTIF_ID, notif)
    }

    private fun createNotification(message: String) = NotificationCompat.Builder(this, MainActivity.CHANNEL_INSTALL)
        .setSmallIcon(android.R.drawable.ic_menu_upload)
        .setContentTitle(getString(R.string.installing_ndk))
        .setContentText(message)
        .setProgress(0, 0, true)
        .setOngoing(true)
        .build()

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}