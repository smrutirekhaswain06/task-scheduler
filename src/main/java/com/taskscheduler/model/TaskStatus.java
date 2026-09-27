package com.taskscheduler.model;

/**
 * Lifecycle states a Task can be in.
 *
 * PENDING   -> waiting for its scheduled time to arrive
 * RUNNING   -> currently being executed by a worker thread
 * COMPLETED -> finished successfully
 * FAILED    -> exhausted all retry attempts and gave up
 */
public enum TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
