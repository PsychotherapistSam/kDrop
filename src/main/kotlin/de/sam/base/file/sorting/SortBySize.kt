package de.sam.base.file.sorting

import de.sam.base.database.FileDTO

class SortBySize : FileSortingDirection("Smallest", "smallest") {
    override fun compare(a: FileDTO, b: FileDTO): Int {
        return a.size!!.compareTo(b.size!!)
    }
}