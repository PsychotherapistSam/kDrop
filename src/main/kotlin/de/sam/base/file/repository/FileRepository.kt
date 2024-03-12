package de.sam.base.file.repository

import de.sam.base.database.FileDTO
import de.sam.base.file.FolderTreeStructure
import org.jdbi.v3.core.Handle
import java.util.*

interface FileRepository {
    fun getFileById(fileID: UUID): FileDTO?
    fun getFileBreadcrumb(fileID: UUID): List<FileDTO>
    fun getFolderContentForUser(folderId: UUID, userId: UUID): List<FileDTO>
    fun getRootFolderForUser(userId: UUID): FileDTO?
    fun recalculateFolderSize(folderId: UUID, userId: UUID)
    fun createFile(handle: Handle, file: FileDTO): FileDTO
    fun updateFile(file: FileDTO): FileDTO?
    fun updateFile(handle: Handle, file: FileDTO): FileDTO?
    fun updateFilesBatch(files: List<FileDTO>)
    fun getFilesByIds(fileIDs: List<UUID>): List<FileDTO>
    fun getAllFilesFromFolderListRecursively(fileIDs: List<UUID>): List<FileDTO>
    fun deleteFilesAndShares(fileIDs: List<UUID>): List<FileDTO>
    fun searchFiles(userId: UUID, query: String, limit: Int = 25): List<FileDTO>
    fun searchFiles(userId: UUID, query: String, limit: Int = 25, type: String = "all"): List<FileDTO>
    fun deleteAllFilesFromUser(userId: UUID)
    fun countTotalFiles(): Int
    fun getFilesWithoutHashes(): List<FileDTO>
    fun getFolderTreeStructure(userId: UUID): FolderTreeStructure
}