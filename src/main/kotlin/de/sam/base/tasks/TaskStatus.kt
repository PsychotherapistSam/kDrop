package de.sam.base.tasks

enum class TaskStatus(val color: String) {
    QUEUED("grey"),
    PROCESSING("blue"),
    COMPLETED("green"),
    FAILED("red")
}