package com.lite.unzipper.archive

enum class ArchiveType {
    ZIP, RAR, SEVEN_Z, TAR, GZIP, BZIP2, XZ, ZSTD,
    TAR_GZ, TAR_BZ2, TAR_XZ, TAR_ZSTD,
    ZIP_SPLIT, SEVEN_Z_SPLIT, RAR_SPLIT, GENERIC_SPLIT,
    UNKNOWN;

    companion object {
        fun fromFileName(name: String): ArchiveType {
            val lower = name.lowercase().trim()
            if (lower.matches(Regex(".*\\.7z\\.\\d{3}$"))) return SEVEN_Z_SPLIT
            if (lower.matches(Regex(".*\\.part\\d+\\.rar$"))) return RAR_SPLIT
            if (lower.matches(Regex(".*\\.r\\d{2,3}$"))) return RAR_SPLIT
            if (lower.matches(Regex(".*\\.z\\d{2}$"))) return ZIP_SPLIT
            if (lower.matches(Regex(".*\\.\\d{3}$")) && !lower.endsWith(".7z.001")) return GENERIC_SPLIT
            if (lower.endsWith(".tar.zst") || lower.endsWith(".tzst")) return TAR_ZSTD
            if (lower.endsWith(".zst") || lower.endsWith(".zstandard")) return ZSTD
            if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) return TAR_GZ
            if (lower.endsWith(".tar.bz2") || lower.endsWith(".tbz2")) return TAR_BZ2
            if (lower.endsWith(".tar.xz") || lower.endsWith(".txz")) return TAR_XZ
            val dotIndex = lower.lastIndexOf('.')
            if (dotIndex < 0 || dotIndex == lower.length - 1) return UNKNOWN
            val ext = lower.substring(dotIndex + 1)
            return when (ext) {
                "zip" -> ZIP
                "rar" -> RAR
                "7z" -> SEVEN_Z
                "tar" -> TAR
                "gz", "gzip" -> GZIP
                "bz2", "bzip2" -> BZIP2
                "xz" -> XZ
                else -> UNKNOWN
            }
        }

        fun isSplitPart(name: String): Boolean {
            val type = fromFileName(name)
            return type == ZIP_SPLIT || type == SEVEN_Z_SPLIT ||
                    type == RAR_SPLIT || type == GENERIC_SPLIT
        }

        fun splitBaseName(name: String): String {
            val lower = name.lowercase()
            return when {
                lower.matches(Regex(".*\\.7z\\.\\d{3}$")) -> name.substring(0, name.length - 4)
                lower.matches(Regex(".*\\.part\\d+\\.rar$")) -> {
                    val idx = lower.indexOf(".part")
                    name.substring(0, idx)
                }
                lower.matches(Regex(".*\\.r\\d{2,3}$")) -> {
                    val idx = lower.lastIndexOf(".r")
                    name.substring(0, idx)
                }
                lower.matches(Regex(".*\\.z\\d{2}$")) -> {
                    val idx = lower.lastIndexOf(".z")
                    name.substring(0, idx)
                }
                lower.matches(Regex(".*\\.\\d{3}$")) -> {
                    val idx = lower.lastIndexOf(".")
                    name.substring(0, idx)
                }
                else -> name
            }
        }
    }
}
