package de.sam.base.utils.file.sorting

import de.sam.base.database.FileDTO

abstract class FileSortingDirection(val prettyName: String, val name: String) {
    abstract fun compare(a: FileDTO, b: FileDTO): Int

    companion object {
        val sortDirections = listOf(
            SortByName(),
            SortByNameReversed(),
            SortBySize(),
            SortBySizeReversed(),
            SortByDate(),
            SortByDateReversed()
        )
    }
}