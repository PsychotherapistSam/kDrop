package de.sam.base.tasks.types

interface RunnableTask {
    suspend fun execute()
}