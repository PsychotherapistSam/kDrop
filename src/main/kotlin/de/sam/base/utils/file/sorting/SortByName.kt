package de.sam.base.utils.file.sorting

import de.sam.base.database.FileDTO
import de.sam.base.utils.file.NaturalOrderComparator

class SortByName : FileSortingDirection("Name (A-Z)", "name") {
    override fun compare(a: FileDTO, b: FileDTO): Int {
        return NaturalOrderComparator.CASEINSENSITIVE_NUMERICAL_ORDER.compare(a.name, b.name)
    }
}