package de.sam.base.tasks.types

import de.sam.base.tasks.queue.TaskQueue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.util.concurrent.CompletableFuture

abstract class Task(
    var id: Int = -1,
    val name: String,
    var description: String = "Not yet started",
    val concurrency: Int = 1,
) : RunnableTask, KoinComponent {
    companion object {
        const val DEBOUNCE_TIME = 150 // 100 ms until next update
    }

    private val taskQueue: TaskQueue by inject()
    private var lastUpdate: Long = 0

    var hasFinished: CompletableFuture<Boolean> = CompletableFuture()

    private fun pushUpdate() {
        if (lastUpdate + DEBOUNCE_TIME > System.currentTimeMillis()) {
            return
        }
        taskQueue.notifyTaskStatusChange()
        lastUpdate = System.currentTimeMillis()
    }

    fun pushDescription(description: String, log: Boolean = false, logLevel: Int = 0) {
        this.description = description
        if (log) {
            when (logLevel) {
                0 -> Logger.tag("Tasks").info(description)
                1 -> Logger.tag("Tasks").warn(description)
                2 -> Logger.tag("Tasks").error(description)
            }
        }
        pushUpdate()
    }
}

