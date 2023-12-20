package de.sam.base.tasks.queue

import de.sam.base.tasks.TaskStatus
import de.sam.base.tasks.types.Task
import de.sam.base.tasks.types.files.FileCleanupTask
import de.sam.base.tasks.types.files.FileParityCheckTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import org.tinylog.kotlin.Logger

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class TaskQueue {
    private val taskQueue = Channel<TaskWithStatus>(Channel.UNLIMITED)
    private val allTasks = mutableListOf<TaskWithStatus>()
    private lateinit var taskProcessor: ReceiveChannel<TaskWithStatus>
    private var idCounter = 0

    var onTaskStatusChange: (() -> Unit)? = null

    init {
        startTaskProcessor()
    }

    private fun startTaskProcessor() {
        taskProcessor = CoroutineScope(coroutineContext).produce {
            while (true) {
                val taskWithStatus = taskQueue.receive()
                try {
                    taskWithStatus.status = TaskStatus.PROCESSING
                    notifyTaskStatusChange()
                    taskWithStatus.task.execute()
//                    delay(2000)
                    notifyTaskStatusChange()
                    taskWithStatus.status = TaskStatus.COMPLETED
                    taskWithStatus.task.hasFinished.complete(true)
                    notifyTaskStatusChange()
                } catch (e: Exception) {
                    taskWithStatus.status = TaskStatus.FAILED
                    taskWithStatus.task.hasFinished.complete(true)
                    notifyTaskStatusChange()
                    Logger.error(e, "Task ${taskWithStatus.task.id} failed")
                }
                delay(1)
            }
        }
    }

    fun notifyTaskStatusChange() {
        onTaskStatusChange?.invoke()
    }

    fun enqueueTask(task: Task, priority: Int = 0) {
        task.id = idCounter++

        val taskWithStatus = TaskWithStatus(task, TaskStatus.QUEUED, priority)
        synchronized(allTasks) {
            allTasks.add(taskWithStatus)
            allTasks.sortByDescending { it.priority }
        }
        if (taskQueue.trySend(taskWithStatus).isFailure) {
            Logger.warn("Task queue is full. Task ${taskWithStatus.task.id} was not added.")
        }
        notifyTaskStatusChange()
    }

    fun getTasksWithStatus(status: TaskStatus): List<TaskWithStatus> {
        return allTasks.filter { it.status == status }
    }

    fun getAllTasks(): List<TaskWithStatus> {
        return allTasks.toList()
    }

    fun withStartupTasks(): TaskQueue {
        enqueueTask(FileParityCheckTask())
        enqueueTask(FileCleanupTask())
        return this
    }
}