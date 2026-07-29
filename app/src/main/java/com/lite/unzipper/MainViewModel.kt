package com.lite.unzipper

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lite.unzipper.archive.ArchiveExtractor
import com.lite.unzipper.archive.ExtractRequest
import com.lite.unzipper.archive.ProgressCallback
import com.lite.unzipper.settings.AppSettings
import com.lite.unzipper.ui.FileItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val extractor = ArchiveExtractor(application)
    private val settings = AppSettings(application)
    private val appContext = application.applicationContext

    private val _state = MutableLiveData<ExtractState>(ExtractState.Idle)
    val state: LiveData<ExtractState> = _state

    private val _selectedFiles = MutableLiveData<List<FileItem>>(emptyList())
    val selectedFiles: LiveData<List<FileItem>> = _selectedFiles

    private val _passwordRequest = MutableSharedFlow<FileItem>(extraBufferCapacity = 8)
    val passwordRequest: SharedFlow<FileItem> = _passwordRequest.asSharedFlow()

    private val _pickTargetDirRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pickTargetDirRequest: SharedFlow<Unit> = _pickTargetDirRequest.asSharedFlow()

    private var extractJob: Job? = null
    private var cancelled = false
    private val pendingPasswordItem = java.util.concurrent.atomic.AtomicReference<FileItem?>(null)

    fun addFiles(items: List<FileItem>) {
        val current = _selectedFiles.value.orEmpty().toMutableList()
        for (item in items) {
            if (current.none { it.uri == item.uri }) current.add(item)
        }
        _selectedFiles.value = current
    }

    fun toggleSelection(item: FileItem) {
        val current = _selectedFiles.value.orEmpty().map {
            if (it.uri == item.uri) it.copy(isSelected = !it.isSelected) else it
        }
        _selectedFiles.value = current
    }

    fun removeItem(item: FileItem) {
        _selectedFiles.value = _selectedFiles.value.orEmpty().filterNot { it.uri == item.uri }
    }

    fun clearAll() { _selectedFiles.value = emptyList() }

    fun startExtraction() {
        val files = _selectedFiles.value.orEmpty().filter { it.isSelected }
        if (files.isEmpty()) return
        val encrypted = files.firstOrNull { it.isEncrypted }
        if (encrypted != null) {
            viewModelScope.launch { _passwordRequest.emit(encrypted) }
            return
        }
        requestPickTargetDir()
    }

    fun onPasswordProvided(password: String?) {
        val files = _selectedFiles.value.orEmpty().map {
            if (it.isEncrypted) it.copy(isSelected = true) else it
        }
        _selectedFiles.value = files.map { it.copy() }.also { items ->
            if (password != null) {
                passwordCache.clear()
                items.filter { it.isEncrypted }.forEach { passwordCache[it.uri] = password }
            }
        }
        requestPickTargetDir()
    }

    private fun requestPickTargetDir() {
        viewModelScope.launch { _pickTargetDirRequest.emit(Unit) }
    }

    fun extractBatch(targetDir: DocumentFile) {
        val files = _selectedFiles.value.orEmpty().filter { it.isSelected }
        if (files.isEmpty()) return
        cancelled = false
        extractJob?.cancel()
        extractJob = viewModelScope.launch {
            _state.value = ExtractState.Running(0, files.size.toLong(), files.first().name, 0, files.size)
            val threadCount = settings.threadCount
            val semaphore = Semaphore(threadCount)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val successCount = java.util.concurrent.atomic.AtomicInteger(0)
            val failureCount = java.util.concurrent.atomic.AtomicInteger(0)
            val failureMessages = java.util.concurrent.ConcurrentLinkedQueue<String>()
            val needPasswordFiles = java.util.concurrent.ConcurrentLinkedQueue<FileItem>()

            val deferreds = files.mapIndexed { index, item ->
                scope.async {
                    semaphore.withPermit {
                        if (cancelled) return@withPermit
                        val splitParts = ArchiveExtractor.findSplitParts(appContext, item.uri, item.name)
                        val password = passwordCache[item.uri]
                        val request = ExtractRequest(sourceUri = item.uri, sourceName = item.name,
                            password = password, splitParts = splitParts)
                        val result = extractSingle(request, targetDir, index, files.size)
                        if (result.first) {
                            successCount.incrementAndGet()
                        } else {
                            val msg = result.second
                            if (msg != null && isPasswordError(msg) && password == null) {
                                needPasswordFiles += item.copy(isEncrypted = true)
                            } else {
                                failureCount.incrementAndGet()
                                if (msg != null) failureMessages += "${item.name}: $msg"
                                else failureMessages += "${item.name}: 解压失败"
                            }
                        }
                    }
                }
            }
            try { deferreds.awaitAll() } finally { scope.cancel() }
            passwordCache.clear()
            if (cancelled) {
                _state.value = ExtractState.Error("已取消")
            } else if (needPasswordFiles.isNotEmpty()) {
                val needItem = needPasswordFiles.first()
                pendingPasswordItem.set(needItem)
                val updated = _selectedFiles.value.orEmpty().map { existing ->
                    val match = needPasswordFiles.any { it.uri == existing.uri }
                    if (match) existing.copy(isEncrypted = true) else existing
                }
                _selectedFiles.value = updated
                pendingSplitTargetDir = targetDir
                viewModelScope.launch { _passwordRequest.emit(needItem) }
            } else {
                if (failureMessages.isEmpty()) {
                    _state.value = ExtractState.BatchSuccess(successCount.get(), failureCount.get(), emptyList())
                } else {
                    _state.value = ExtractState.BatchSuccess(successCount.get(), failureCount.get(), failureMessages.toList())
                }
            }
        }
    }

    private fun isPasswordError(message: String): Boolean {
        val lower = message.lowercase()
        return listOf("密码", "password", "encrypted", "加密", "aes", "wrong", "bad padding",
            "crc", "checksum", "invalid key", "无法解密", "decrypt").any { it in lower }
    }

    private var pendingSplitTargetDir: DocumentFile? = null

    fun resumeAfterPasswordRetry(password: String?) {
        val targetDir = pendingSplitTargetDir ?: return
        pendingSplitTargetDir = null
        val files = _selectedFiles.value.orEmpty().filter { it.isSelected && it.isEncrypted }
        files.forEach { item -> if (password != null) passwordCache[item.uri] = password }
        extractBatch(targetDir)
    }

    private suspend fun extractSingle(request: ExtractRequest, targetDir: DocumentFile,
        index: Int, total: Int): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        var success = false
        var errorMsg: String? = null
        val callback = object : ProgressCallback {
            override fun onProgress(current: Long, total: Long, message: String) {
                _state.postValue(ExtractState.Running(current, total, message, index, total))
            }
            override fun onComplete(extractedCount: Int) { success = true }
            override fun onError(error: Throwable) {
                success = false
                errorMsg = error.message ?: error.javaClass.simpleName
            }
            override fun isCancelled(): Boolean = cancelled
        }
        try { extractor.extract(request, targetDir, callback) }
        catch (t: Throwable) { success = false; errorMsg = t.message ?: t.javaClass.simpleName }
        success to errorMsg
    }

    fun cancelExtraction() {
        cancelled = true
        extractJob?.cancel()
        _state.value = ExtractState.Error("已取消")
    }

    override fun onCleared() {
        super.onCleared()
        extractJob?.cancel()
    }

    private val passwordCache = mutableMapOf<Uri, String>()

    sealed class ExtractState {
        object Idle : ExtractState()
        data class Running(val current: Long, val total: Long, val currentMessage: String,
            val batchIndex: Int, val batchTotal: Int) : ExtractState()
        data class Success(val count: Int) : ExtractState()
        data class BatchSuccess(val success: Int, val failure: Int,
            val failureDetails: List<String> = emptyList()) : ExtractState()
        data class Error(val message: String) : ExtractState()
    }
}
