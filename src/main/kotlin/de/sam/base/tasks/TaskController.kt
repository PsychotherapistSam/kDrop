package de.sam.base.tasks

import de.sam.base.tasks.queue.TaskQueue
import de.sam.base.tasks.types.files.EvaluateMissingHashesTask
import de.sam.base.tasks.types.files.FileCleanupTask
import de.sam.base.tasks.types.files.FileParityCheckTask
import de.sam.base.tasks.types.user.SessionCleanupTask
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import io.javalin.http.Context
import io.javalin.http.sse.SseClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentLinkedQueue

class TaskController : KoinComponent {
    private val taskQueue: TaskQueue by inject()
    private val templateEngine: TemplateEngine by inject()
    private val clients = ConcurrentLinkedQueue<SseClient>()

    val onTaskStatusChange: (() -> Unit) = {
        val output = renderTaskList()

        clients.forEach {
            it.sendEvent("activeTasks", output)
        }
    }

    private fun renderTaskList(): String {
        val tasks = taskQueue.getAllTasks()
            .filter { it.status != TaskStatus.COMPLETED && it.status != TaskStatus.FAILED }
            .take(15)

        val output = StringOutput()
        templateEngine.render(
            "components/taskQueueComponent.kte",
            mapOf(
                "tasks" to tasks,
                "total" to taskQueue.getAllTasks()
                    .count { it.status == TaskStatus.QUEUED || it.status == TaskStatus.PROCESSING },
            ),
            output
        )
        return output.toString()
    }

    fun handleSSE(client: SseClient): SseClient {
        clients.add(client)
        client.keepAlive()

        client.sendEvent("activeTasks", renderTaskList())

        client.onClose { clients.remove(client) }
        return client
    }

    fun list(ctx: Context) {
        ctx.render("components/taskList.kte")
    }

    fun runSingle(ctx: Context) {
        val param = ctx.pathParam("action")
        when (param) {
            "delete-dangling-files" -> {
                taskQueue.enqueueTask(FileCleanupTask())
            }

            "calculate-missing-hashes" -> {
                taskQueue.enqueueTask(EvaluateMissingHashesTask())
            }

            "file-parity-check" -> {
                taskQueue.enqueueTask(FileParityCheckTask())
            }

            "remove-all-sessions" -> {
                taskQueue.enqueueTask(SessionCleanupTask())
            }

            else -> {
                ctx.status(404)
                return
            }
        }

        ctx.redirect("/admin/task")
    }


}