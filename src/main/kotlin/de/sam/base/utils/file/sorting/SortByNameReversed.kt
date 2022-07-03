package de.sam.base.utils.file.sorting

import de.sam.base.database.FileDTO
import de.sam.base.utils.file.NaturalOrderComparator

class SortByNameReversed : FileSortingDirection("Name Reversed (Z-A)", "namerev") {
    override fun compare(a: FileDTO, b: FileDTO): Int {
        return NaturalOrderComparator.CASEINSENSITIVE_NUMERICAL_ORDER.compare(b.name, a.name)
    }
}