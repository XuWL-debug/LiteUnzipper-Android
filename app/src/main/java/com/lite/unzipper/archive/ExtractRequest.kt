package com.lite.unzipper.archive

import android.net.Uri

data class ExtractRequest(
    val sourceUri: Uri,
    val sourceName: String,
    val password: String? = null,
    val splitParts: List<Uri>? = null
)

interface ProgressCallback {
    fun onProgress(current: Long, total: Long, message: String)
    fun onComplete(extractedCount: Int)
    fun onError(error: Throwable)
    fun isCancelled(): Boolean = false
}
