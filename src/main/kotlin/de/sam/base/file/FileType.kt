package de.sam.base.file

enum class FileType {
    PDF,
    JSON,
    PLAIN,
    VIDEO_MP4,
    AUDIO,
    IMAGE,
    OTHER;

    companion object {
        fun fromMimeType(mimeType: String): FileType {
            return when {
                mimeType == "application/pdf" -> PDF
                mimeType == "application/json" -> JSON
                mimeType == "text/plain" -> PLAIN
                mimeType == "video/mp4" -> VIDEO_MP4
                mimeType.startsWith("audio/") -> AUDIO
                mimeType.startsWith("image/") -> IMAGE
                else -> OTHER
            }
        }
    }
}