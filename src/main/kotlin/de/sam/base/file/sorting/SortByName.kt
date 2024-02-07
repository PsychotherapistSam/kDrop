package de.sam.base.file.sorting

import de.sam.base.database.FileDTO
import de.sam.base.file.NaturalOrderComparator

class SortByName : FileSortingDirection("Name (A-Z)", "name") {
    override fun compare(a: FileDTO, b: FileDTO): Int {
        return NaturalOrderComparator.CASEINSENSITIVE_NUMERICAL_ORDER.compare(a.name, b.name)
    }
}