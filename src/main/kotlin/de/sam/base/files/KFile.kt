package de.sam.base.files

import java.util.UUID

class KFile(
    val id: UUID,
    val name: String,
    val parent: String? = null, // KFile
    val size: String,
    val lastModified: String,
    val isDirectory: Boolean,
    val children: List<String> = listOf() // UUID
) {
}