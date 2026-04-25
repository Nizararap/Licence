package com.ccompile.lite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.ccompile.lite.databinding.FragmentDashboardBinding
import java.io.File

class DashboardFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClear.setOnClickListener {
            viewModel.clearLog()
        }

        binding.btnCopy.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Terminal Log", viewModel.getFullLog())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Terminal log copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        viewModel.terminalLog.observe(viewLifecycleOwner) { log ->
            updateLogWithColorsFast(log)
            binding.scrollViewLog.post { binding.scrollViewLog.fullScroll(View.FOCUS_DOWN) }
        }

        viewModel.buildRequest.observe(viewLifecycleOwner) { request ->
            if (request != null) {
                startCompilation(request.path, request.isClean)
                viewModel.clearBuildRequest()
            }
        }

        viewModel.isBuilding.observe(viewLifecycleOwner) { active ->
            binding.progressBuild.visibility = if (active) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun startCompilation(projectPath: String, isClean: Boolean) {
        val ndkDir = File(requireContext().filesDir, "ndk")
        val ndkBuildFile = ndkDir.walkTopDown().find { it.name == "ndk-build" }

        if (ndkBuildFile == null || !ndkBuildFile.exists()) {
            viewModel.appendLog("[ERROR] ${getString(R.string.ndk_not_found)}")
            return
        }

        viewModel.appendLog("\n" + "=".repeat(30))
        // PERBAIKAN: Beritahu di terminal bahwa ini adalah proses Clean & Build
        if (isClean) {
            viewModel.appendLog("[INFO] CLEAN AND BUILD STARTED")
        } else {
            viewModel.appendLog("[INFO] ${getString(R.string.build_started)}")
        }
        viewModel.appendLog("[INFO] Target: $projectPath")
        viewModel.appendLog("=".repeat(30) + "\n")

        viewModel.setBuildStatus(true)

        val intent = Intent(requireContext(), BuildService::class.java).apply {
            putExtra("ndk_build_path", ndkBuildFile.absolutePath)
            putExtra("project_path", projectPath)
            putExtra("is_clean", isClean)
        }
        requireContext().startService(intent)
    }

    // PERBAIKAN: Fungsi ini sangat cepat. Ia hanya akan me-render dan mewarnai 
    // bagian teks baru yang masuk, bukan me-render ulang ribuan baris dari nol.
    private fun updateLogWithColorsFast(fullLog: String) {
        val currentLen = binding.tvLog.text.length
        
        // Jika log direset, bersihkan textview
        if (fullLog == "Terminal Ready...\n") {
            binding.tvLog.text = fullLog
            return
        }
        
        // Jika ada penambahan log baru
        if (fullLog.length > currentLen) {
            val newPart = fullLog.substring(currentLen)
            val builder = SpannableStringBuilder()
            val lines = newPart.split("\n")
            
            for (line in lines) {
                if (line.isEmpty()) continue
                val color = when {
                    line.contains("error:", true) || line.contains("failed", true) -> Color.parseColor("#F44336")
                    line.contains("warning:", true) -> Color.parseColor("#FFC107")
                    line.contains("success", true) -> Color.parseColor("#4CAF50")
                    line.contains("[INFO]", true) -> Color.parseColor("#2196F3")
                    else -> Color.parseColor("#E0E0E0")
                }
                val start = builder.length
                builder.append(line).append("\n")
                builder.setSpan(ForegroundColorSpan(color), start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            binding.tvLog.append(builder)
        } else if (fullLog.length < currentLen) {
            // Failsafe jika state kacau
            binding.tvLog.text = fullLog
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}