package de.sam.base.file.sorting

import de.sam.base.database.FileDTO

class SortByDateReversed : FileSortingDirection("Newest", "newest") {
    override fun compare(a: FileDTO, b: FileDTO): Int {
        return b.created!!.compareTo(a.created!!)
    }
}