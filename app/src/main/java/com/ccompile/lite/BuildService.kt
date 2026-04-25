package com.ccompile.lite

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class BuildService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_LOG_UPDATE = "com.ccompile.lite.BUILD_LOG"
        const val ACTION_FINISHED = "com.ccompile.lite.BUILD_FINISHED"
        const val EXTRA_LOG = "log"
        const val EXTRA_SUCCESS = "success"
        const val NOTIF_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ndkBuildPath = intent?.getStringExtra("ndk_build_path") ?: return START_NOT_STICKY
        val projectPath = intent.getStringExtra("project_path") ?: return START_NOT_STICKY
        val isClean = intent.getBooleanExtra("is_clean", false)

        startForeground(NOTIF_ID, createNotification("Starting task..."))

        serviceScope.launch {
            try {
                // PERBAIKAN: Jika Clean, buang output clean ke /dev/null agar tidak tampil di log
                val cmd = if (isClean) {
                    "cd \"$projectPath\" && \"$ndkBuildPath\" NDK_PROJECT_PATH=. clean > /dev/null 2>&1 && \"$ndkBuildPath\" NDK_PROJECT_PATH=."
                } else {
                    "cd \"$projectPath\" && \"$ndkBuildPath\" NDK_PROJECT_PATH=."
                }

                val process = ProcessBuilder("sh", "-c", cmd)
                    .directory(File(projectPath))
                    .redirectErrorStream(true)
                    .start()

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    broadcastLog(line!!)
                }

                val exitCode = process.waitFor()
                broadcastFinished(exitCode == 0)
            } catch (e: Exception) {
                broadcastLog("[ERROR] ${e.message}")
                broadcastFinished(false)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_REDELIVER_INTENT
    }

    private fun broadcastLog(message: String) {
        val intent = Intent(ACTION_LOG_UPDATE).putExtra(EXTRA_LOG, message)
        sendBroadcast(intent)
    }

    private fun broadcastFinished(success: Boolean) {
        val intent = Intent(ACTION_FINISHED).putExtra(EXTRA_SUCCESS, success)
        sendBroadcast(intent)

        val msg = if (success) "Task Successful" else "Task Failed"
        val notif = NotificationCompat.Builder(this, MainActivity.CHANNEL_BUILD)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle(getString(R.string.building_project))
            .setContentText(msg)
            .setOngoing(false)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager).notify(NOTIF_ID, notif)
    }

    private fun createNotification(message: String) = NotificationCompat.Builder(this, MainActivity.CHANNEL_BUILD)
        .setSmallIcon(android.R.drawable.ic_menu_manage)
        .setContentTitle(getString(R.string.building_project))
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}