package de.sam.base.utils.file.sorting

import de.sam.base.database.FileDTO

class SortByDate : FileSortingDirection("Oldest", "oldest") {
    override fun compare(a: FileDTO, b: FileDTO): Int {
        return a.created.compareTo(b.created)
    }
}