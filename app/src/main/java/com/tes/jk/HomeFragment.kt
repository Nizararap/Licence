package com.tes.jk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tes.jk.databinding.FragmentHomeBinding
import java.io.File

class HomeFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MyAdapter
    private var currentDir: File = Environment.getExternalStorageDirectory()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
            loadDirectory(currentDir)
        } else {
            Toast.makeText(context, "Permission denied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MyAdapter(emptyList()) { file ->
            if (file.isDirectory) loadDirectory(file)
            else handleFileClick(file)
        }
        binding.recyclerView.adapter = adapter

        binding.fabBuild.setOnClickListener {
            viewModel.setProjectPath(currentDir.absolutePath)
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.dashboardFragment
        }

        binding.btnGrantPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${requireContext().packageName}")
                startActivity(intent)
            } else {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                )
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentDir.absolutePath != Environment.getExternalStorageDirectory().absolutePath) {
                    currentDir.parentFile?.let { loadDirectory(it) }
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadDirectory(currentDir)
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun loadDirectory(dir: File) {
        currentDir = dir
        binding.tvCurrentPath.text = dir.absolutePath

        if (!hasStoragePermission()) {
            binding.layoutPermission.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            binding.fabBuild.visibility = View.GONE
            adapter.updateData(emptyList())
            return
        }

        if (!dir.canRead()) {
            Toast.makeText(context, getString(R.string.access_denied), Toast.LENGTH_SHORT).show()
            adapter.updateData(emptyList())
            return
        }

        binding.layoutPermission.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE

        val files = dir.listFiles()?.toList()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        adapter.updateData(files)
        updateBuildFabVisibility(dir)
    }

    private fun updateBuildFabVisibility(dir: File) {
        val jniDir = File(dir, "jni")
        val hasAndroidMk = File(jniDir, "Android.mk").exists()
        val hasApplicationMk = File(jniDir, "Application.mk").exists()
        binding.fabBuild.visibility = if (hasAndroidMk || hasApplicationMk) View.VISIBLE else View.GONE
    }

    private fun handleFileClick(file: File) {
        if (file.name.endsWith(".apk")) installApk(file)
        else Toast.makeText(context, getString(R.string.opening_file_not_supported), Toast.LENGTH_SHORT).show()
    }

    private fun installApk(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}