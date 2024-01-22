@file:OptIn(DelicateCoroutinesApi::class)

package de.sam.base.tasks.queue

import de.sam.base.tasks.TaskStatus
import de.sam.base.tasks.types.Task
import de.sam.base.tasks.types.files.FileCleanupTask
import de.sam.base.tasks.types.files.FileParityCheckTask
import kotlinx.coroutines.*
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import org.tinylog.kotlin.Logger

@OptIn(ExperimentalCoroutinesApi::class)
class TaskQueue {
    private val taskQueue = Channel<TaskWithStatus>(Channel.UNLIMITED)
    private val allTasks = mutableListOf<TaskWithStatus>()
    private val taskSemaphores = mutableMapOf<String, Semaphore>()

    // total number of tasks that can run at the same time
    private val globalSemaphore = Semaphore(10)
    private var idCounter = 0

    var onTaskStatusChange: (() -> Unit)? = null

    init {
        startTaskProcessor()
    }

    private var isActive = false

    private fun startTaskProcessor() {
        isActive = true
        CoroutineScope(coroutineContext).launch {
            while (isActive) {
                val taskWithStatus = taskQueue.receive()
                val semaphore =
                    taskSemaphores.getOrPut(taskWithStatus.task::class.simpleName!!) {
                        Semaphore(taskWithStatus.task.concurrency)
                    }
                globalSemaphore.acquire()
                semaphore.acquire()
                launch {
                    try {
                        taskWithStatus.status = TaskStatus.PROCESSING
                        notifyTaskStatusChange()
                        taskWithStatus.task.execute()
                        notifyTaskStatusChange()
                        taskWithStatus.status = TaskStatus.COMPLETED
                        taskWithStatus.task.hasFinished.complete(true)
                        notifyTaskStatusChange()
                    } catch (e: Exception) {
                        taskWithStatus.status = TaskStatus.FAILED
                        taskWithStatus.task.hasFinished.complete(true)
                        notifyTaskStatusChange()
                        Logger.error(e, "Task ${taskWithStatus.task.id} failed")
                    } finally {
                        semaphore.release()
                        globalSemaphore.release()
                    }
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
            allTasks.sortWith(
                compareByDescending<TaskWithStatus> { it.priority }.thenBy { it.task.id }
            )
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
