package de.sam.base.file

import de.sam.base.database.FileDTO
import java.util.*

class FileCache {
    private val map = mutableMapOf<UUID, Pair<Long, FileDTO>>()

    fun containsKey(id: UUID): Boolean {
        return map.containsKey(id)
    }

    fun remove(id: UUID) {
        map.remove(id)
    }

    operator fun get(id: UUID): Pair<Long, FileDTO>? {
        return map[id]
    }

    operator fun set(fileId: UUID, value: Pair<Long, FileDTO>) {
        map[fileId] = value
    }
}