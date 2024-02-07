package de.sam.base.file.sorting

import de.sam.base.database.FileDTO

class SortBySizeReversed : FileSortingDirection("Largest", "largest") {
    override fun compare(a: FileDTO, b: FileDTO): Int {
        return b.size!!.compareTo(a.size!!)
    }
}