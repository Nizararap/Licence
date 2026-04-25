package com.tes.jk

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    // Menyimpan path folder yang mau di-build
    private val _projectPath = MutableLiveData<String>()
    val projectPath: LiveData<String> get() = _projectPath

    // Menyimpan log terminal
    private val _terminalLog = MutableLiveData<String>("Terminal Ready...\n")
    val terminalLog: LiveData<String> get() = _terminalLog

    // Status Build
    private val _isBuilding = MutableLiveData(false)
    val isBuilding: LiveData<Boolean> get() = _isBuilding

    fun setProjectPath(path: String) {
        _projectPath.value = path
    }

    fun appendLog(text: String) {
        val current = _terminalLog.value ?: ""
        _terminalLog.postValue(current + text + "\n")
    }

    fun clearLog() {
        _terminalLog.postValue("Terminal Ready...\n")
    }
    
    fun setBuildStatus(active: Boolean) {
        _isBuilding.postValue(active)
    }
}