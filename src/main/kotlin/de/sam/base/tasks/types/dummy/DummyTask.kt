package de.sam.base.tasks.types.dummy

import de.sam.base.tasks.types.Task
import kotlinx.coroutines.delay

class DummyTask : Task(name = "Dummy Task", concurrency = 5) {
    override suspend fun execute() {
        pushDescription("[$id] sleeping for 10 seconds")
        delay(5000)
    }
}