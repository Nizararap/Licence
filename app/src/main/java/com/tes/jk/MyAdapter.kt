package com.tes.jk

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tes.jk.databinding.ItemListBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyAdapter(
    private var fileList: List<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = fileList[position]
        holder.binding.tvTitle.text = file.name
        
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
        val dateStr = sdf.format(Date(file.lastModified()))

        if (file.isDirectory) {
            holder.binding.ivIcon.setImageResource(R.drawable.ic_folder)
            holder.binding.tvSubtitle.text = "Folder • $dateStr"
        } else {
            holder.binding.ivIcon.setImageResource(getFileIcon(file.name))
            val size = file.length() / 1024
            holder.binding.tvSubtitle.text = "$size KB • $dateStr"
        }

        holder.binding.root.setOnClickListener { onItemClick(file) }
    }

    override fun getItemCount(): Int = fileList.size

    fun updateData(newList: List<File>) {
        fileList = newList
        notifyDataSetChanged()
    }

    private fun getFileIcon(fileName: String): Int {
        val name = fileName.lowercase()
        return when {
            name.endsWith(".cpp") || name.endsWith(".c") || name.endsWith(".h") -> R.drawable.ic_code
            name.endsWith(".java") || name.endsWith(".kt") -> R.drawable.ic_code
            name.endsWith(".apk") -> R.drawable.ic_apk
            name.endsWith(".zip") || name.endsWith(".rar") -> R.drawable.ic_file
            else -> R.drawable.ic_file
        }
    }
}