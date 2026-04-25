package com.ccompile.lite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    
    // Model untuk request build (Path Target + Apakah itu Clean Build?)
    data class BuildRequest(val path: String, val isClean: Boolean)

    private val _buildRequest = MutableLiveData<BuildRequest?>()
    val buildRequest: LiveData<BuildRequest?> get() = _buildRequest

    private val _terminalLog = MutableLiveData<String>("Terminal Ready...\n")
    val terminalLog: LiveData<String> get() = _terminalLog

    private val _isBuilding = MutableLiveData(false)
    val isBuilding: LiveData<Boolean> get() = _isBuilding

    fun startNdkAction(path: String, isClean: Boolean) {
        // Cegah eksekusi jika sedang proses build
        if (_isBuilding.value == true) return
        _buildRequest.value = BuildRequest(path, isClean)
    }

    fun clearBuildRequest() {
        _buildRequest.value = null
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

    // Fungsi utilitas untuk Copy ke clipboard
    fun getFullLog(): String {
        return _terminalLog.value ?: ""
    }
}