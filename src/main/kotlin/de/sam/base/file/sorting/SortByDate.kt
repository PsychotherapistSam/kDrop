package de.sam.base.file.sorting

import de.sam.base.database.FileDTO

class SortByDate : FileSortingDirection("Oldest", "oldest") {
    override fun compare(a: FileDTO, b: FileDTO): Int {
        return a.created!!.compareTo(b.created!!)
    }
}