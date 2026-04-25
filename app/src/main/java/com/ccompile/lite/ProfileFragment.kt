package com.ccompile.lite

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ccompile.lite.databinding.FragmentProfileBinding
import java.io.File

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var ndkInstalled = false

    private val pickZipLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri -> startInstallService(uri) }
        }
    }

    private val installReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                InstallNdkService.ACTION_PROGRESS -> {
                    val msg = intent.getStringExtra(InstallNdkService.EXTRA_MSG) ?: "Processing..."
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.isIndeterminate = true
                    binding.tvNdkStatus.text = msg
                    binding.btnInstallNdk.isEnabled = false
                    binding.btnResetNdk.isEnabled = false
                }
                InstallNdkService.ACTION_FINISHED -> {
                    val success = intent.getBooleanExtra(InstallNdkService.EXTRA_SUCCESS, false)
                    val msg = intent.getStringExtra(InstallNdkService.EXTRA_MSG) ?: ""
                    
                    if (!success) {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                    // Cek status akhir secara riil
                    checkNdkStatus()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkNdkStatus()

        binding.btnInstallNdk.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
            pickZipLauncher.launch(intent)
        }

        binding.btnResetNdk.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.remove_ndk_confirm_title))
                .setMessage(getString(R.string.remove_ndk_confirm_msg))
                .setPositiveButton(getString(R.string.yes)) { _, _ -> removeNdk() }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        checkNdkStatus()
    }

    private fun startInstallService(uri: Uri) {
        val intent = Intent(requireContext(), InstallNdkService::class.java).apply {
            data = uri
            putExtra("dest_dir", File(requireContext().filesDir, "ndk").absolutePath)
        }
        requireContext().startService(intent)
        
        // Langsung perbarui UI dengan asumsi service baru saja start
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.isIndeterminate = true
        binding.tvNdkStatus.text = "Initializing extraction..."
        binding.btnInstallNdk.isEnabled = false
        binding.btnResetNdk.isEnabled = false
    }

    private fun removeNdk() {
        val destDir = File(requireContext().filesDir, "ndk")
        if (destDir.exists()) {
            destDir.deleteRecursively()
            Toast.makeText(requireContext(), "NDK Removed", Toast.LENGTH_SHORT).show()
        }
        checkNdkStatus()
    }

    private fun checkNdkStatus() {
        // PERBAIKAN: Jika service bilang sedang running, JANGAN CEK FILE DULU!
        // Ini mencegah UI loncat ke "Installed" saat kita pindah tab ke Explorer dan kembali lagi.
        if (InstallNdkService.isInstalling) {
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.isIndeterminate = true
            binding.tvNdkStatus.text = "Installation running in background..."
            binding.tvNdkStatus.setTextColor(Color.parseColor("#2196F3"))
            binding.btnInstallNdk.isEnabled = false
            binding.btnResetNdk.isEnabled = false
            return
        }

        // Jika Service MATI, barulah kita cek aktual filenya di memory HP
        val ndkDir = File(requireContext().filesDir, "ndk")
        val ndkBuild = ndkDir.walkTopDown().find { it.name == "ndk-build" }

        if (ndkBuild != null && ndkBuild.exists()) {
            binding.tvNdkStatus.text = "${getString(R.string.ndk_installed)}\nPath: ${ndkBuild.parentFile?.name}"
            binding.tvNdkStatus.setTextColor(Color.parseColor("#4CAF50"))
            ndkInstalled = true
        } else {
            if (!ndkDir.exists() || ndkDir.listFiles()?.isEmpty() == true) {
                binding.tvNdkStatus.text = getString(R.string.ndk_not_installed)
                binding.tvNdkStatus.setTextColor(Color.parseColor("#F44336"))
            } else {
                binding.tvNdkStatus.text = "Extraction failed or incomplete!"
                binding.tvNdkStatus.setTextColor(Color.parseColor("#FF9800"))
            }
            ndkInstalled = false
        }

        binding.progressBar.visibility = View.GONE
        binding.btnInstallNdk.isEnabled = !ndkInstalled
        binding.btnResetNdk.isEnabled = ndkInstalled
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(InstallNdkService.ACTION_PROGRESS)
            addAction(InstallNdkService.ACTION_FINISHED)
        }
        ContextCompat.registerReceiver(
            requireContext(), 
            installReceiver, 
            filter, 
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        try { requireContext().unregisterReceiver(installReceiver) } catch (e: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}