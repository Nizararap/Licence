package com.tes.jk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.app.NotificationChannel
import android.app.NotificationManager

class MainActivity : AppCompatActivity() {

    companion object {
        const val CHANNEL_BUILD = "build_channel"
        const val CHANNEL_INSTALL = "install_channel"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Buat Notification Channel (Wajib untuk Android 8+)
        createNotificationChannels()

        // Minta izin notifikasi untuk Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        requestStoragePermission()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setupWithNavController(navController)
    }

    private fun createNotificationChannels() {
        val buildChannel = NotificationChannel(
            CHANNEL_BUILD,
            getString(R.string.notif_channel_build),
            NotificationManager.IMPORTANCE_LOW // Suara rendah, tapi terlihat
        ).apply { description = "Build progress" }

        val installChannel = NotificationChannel(
            CHANNEL_INSTALL,
            getString(R.string.notif_channel_install),
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "NDK installation progress" }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(buildChannel)
        manager.createNotificationChannel(installChannel)
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } else {
            val perms = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (ContextCompat.checkSelfPermission(this, perms[0]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, perms, 100)
            }
        }
    }
}