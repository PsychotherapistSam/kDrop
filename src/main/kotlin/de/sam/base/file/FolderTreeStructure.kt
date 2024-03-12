package de.sam.base.file

import java.util.*

class FolderTreeStructure(val name: String, val id: UUID, var folders: MutableList<FolderTreeStructure>)