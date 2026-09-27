package com.taskscheduler.model;

/**
 * The kind of job a Task represents. Each type has different
 * simulated "work" performed by TaskExecutorService.
 */
public enum TaskType {
    LOG_MESSAGE,       // simply logs the description (always succeeds)
    GENERATE_REPORT,   // writes a small report file to disk (always succeeds)
    SEND_NOTIFICATION  // simulates sending a notification (can randomly fail, to demo retries)
}
