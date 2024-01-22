package de.sam.base.tasks.types.files

import de.sam.base.services.FileService
import de.sam.base.tasks.queue.TaskQueue
import de.sam.base.tasks.types.Task
import kotlinx.coroutines.delay
import org.koin.core.component.inject

class EvaluateMissingHashesTask : Task(name = "Evaluate missing hashes", concurrency = 1) {
    private val fileService: FileService by inject()
    private val taskQueue: TaskQueue by inject()

    override suspend fun execute() {
        pushDescription("Starting to evaluate missing hashes", true, 0)

        fileService.getFilesWithoutHashes().forEach {
            pushDescription("Enqueuing hash task for file ${it.id}", true, 0)
            taskQueue.enqueueTask(HashFileTask(it))
        }

        // to keep the task in the list for a bit
        delay(2000)
    }
}