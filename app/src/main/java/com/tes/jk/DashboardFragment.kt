package com.tes.jk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.tes.jk.databinding.FragmentDashboardBinding
import java.io.File

class DashboardFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val buildReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BuildService.ACTION_LOG_UPDATE -> {
                    val log = intent.getStringExtra(BuildService.EXTRA_LOG) ?: return
                    viewModel.appendLog(log)
                }
                BuildService.ACTION_FINISHED -> {
                    binding.progressBuild.visibility = View.INVISIBLE
                    val success = intent.getBooleanExtra(BuildService.EXTRA_SUCCESS, false)
                    if (success) viewModel.appendLog("\n[SUCCESS] Build Complete")
                    else viewModel.appendLog("\n[ERROR] Build Failed")
                    viewModel.setBuildStatus(false)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClear.setOnClickListener {
            viewModel.clearLog()
        }

        viewModel.terminalLog.observe(viewLifecycleOwner) { log ->
            updateLogWithColors(log)
            binding.scrollViewLog.post { binding.scrollViewLog.fullScroll(View.FOCUS_DOWN) }
        }

        viewModel.projectPath.observe(viewLifecycleOwner) { path ->
            if (path.isNotEmpty()) {
                startCompilation(path)
                viewModel.setProjectPath("")
            }
        }

        viewModel.isBuilding.observe(viewLifecycleOwner) { active ->
            binding.progressBuild.visibility = if (active) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun startCompilation(projectPath: String) {
        val ndkDir = File(requireContext().filesDir, "ndk")
        val ndkBuildFile = ndkDir.walkTopDown().find { it.name == "ndk-build" }

        if (ndkBuildFile == null || !ndkBuildFile.exists()) {
            viewModel.appendLog("[ERROR] ${getString(R.string.ndk_not_found)}")
            return
        }

        viewModel.appendLog("\n" + "=".repeat(30))
        viewModel.appendLog("[INFO] ${getString(R.string.build_started)}")
        viewModel.appendLog("[INFO] Target: $projectPath")
        viewModel.appendLog("=".repeat(30) + "\n")

        viewModel.setBuildStatus(true)

        val intent = Intent(requireContext(), BuildService::class.java).apply {
            putExtra("ndk_build_path", ndkBuildFile.absolutePath)
            putExtra("project_path", projectPath)
        }
        requireContext().startService(intent)
    }

    private fun updateLogWithColors(fullLog: String) {
        val spannable = SpannableString(fullLog)
        val lines = fullLog.split("\n")
        var currentPos = 0
        
        for (line in lines) {
            val color = when {
                line.contains("error:", true) || line.contains("failed", true) -> Color.parseColor("#F44336")
                line.contains("warning:", true) -> Color.parseColor("#FFC107")
                line.contains("success", true) -> Color.parseColor("#4CAF50")
                line.contains("[INFO]", true) -> Color.parseColor("#2196F3")
                else -> Color.parseColor("#E0E0E0")
            }
            val endPos = currentPos + line.length
            if (endPos <= spannable.length) {
                spannable.setSpan(ForegroundColorSpan(color), currentPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            currentPos = endPos + 1
        }
        binding.tvLog.text = spannable
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter()
        filter.addAction(BuildService.ACTION_LOG_UPDATE)
        filter.addAction(BuildService.ACTION_FINISHED)
        requireContext().registerReceiver(buildReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        try { requireContext().unregisterReceiver(buildReceiver) } catch (e: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}