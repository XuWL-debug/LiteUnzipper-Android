package com.lite.unzipper.ui

import android.net.Uri
import android.text.format.Formatter
import com.lite.unzipper.archive.ArchiveType

data class FileItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val type: ArchiveType,
    val isEncrypted: Boolean = false,
    var isSelected: Boolean = false
) {
    fun formatMeta(context: android.content.Context): String {
        val sizeStr = Formatter.formatFileSize(context, size)
        val typeStr = when (type) {
            ArchiveType.ZIP -> "ZIP"
            ArchiveType.RAR -> "RAR"
            ArchiveType.SEVEN_Z -> "7z"
            ArchiveType.TAR -> "TAR"
            ArchiveType.GZIP -> "GZIP"
            ArchiveType.BZIP2 -> "BZIP2"
            ArchiveType.XZ -> "XZ"
            ArchiveType.ZSTD -> "Zstandard"
            ArchiveType.TAR_GZ -> "TAR.GZ"
            ArchiveType.TAR_BZ2 -> "TAR.BZ2"
            ArchiveType.TAR_XZ -> "TAR.XZ"
            ArchiveType.TAR_ZSTD -> "TAR.ZSTD"
            ArchiveType.ZIP_SPLIT -> "ZIP 分卷"
            ArchiveType.SEVEN_Z_SPLIT -> "7z 分卷"
            ArchiveType.RAR_SPLIT -> "RAR 分卷"
            ArchiveType.GENERIC_SPLIT -> "分卷"
            else -> "未知"
        }
        return "$sizeStr · $typeStr"
    }
}
