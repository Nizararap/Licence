package com.ccompile.lite

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    // PERBAIKAN: Hubungkan dengan ViewModel untuk menyimpan log
    private val viewModel: MainViewModel by viewModels()

    companion object {
        const val CHANNEL_BUILD = "build_channel"
        const val CHANNEL_INSTALL = "install_channel"
    }

    // PERBAIKAN: Pindahkan Receiver ke sini agar log tidak stuck saat ganti tab
    private val buildReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BuildService.ACTION_LOG_UPDATE -> {
                    val log = intent.getStringExtra(BuildService.EXTRA_LOG) ?: return
                    viewModel.appendLog(log)
                }
                BuildService.ACTION_FINISHED -> {
                    val success = intent.getBooleanExtra(BuildService.EXTRA_SUCCESS, false)
                    if (success) viewModel.appendLog("\n[SUCCESS] Build Complete")
                    else viewModel.appendLog("\n[ERROR] Build Failed")
                    viewModel.setBuildStatus(false)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannels()

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

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(BuildService.ACTION_LOG_UPDATE)
            addAction(BuildService.ACTION_FINISHED)
        }
        // PERBAIKAN API 34: Tambahkan RECEIVER_NOT_EXPORTED
        ContextCompat.registerReceiver(this, buildReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        super.onStop()
        try { unregisterReceiver(buildReceiver) } catch (e: Exception) {}
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val buildChannel = NotificationChannel(
                CHANNEL_BUILD,
                getString(R.string.notif_channel_build),
                NotificationManager.IMPORTANCE_LOW
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