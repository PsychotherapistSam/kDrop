package de.sam.base.tasks.queue

import de.sam.base.tasks.TaskStatus
import de.sam.base.tasks.types.Task

data class TaskWithStatus(val task: Task, var status: TaskStatus, val priority: Int)