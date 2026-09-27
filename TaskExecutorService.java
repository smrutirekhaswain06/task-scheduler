package com.taskscheduler.service;

import com.taskscheduler.model.Task;
import com.taskscheduler.model.TaskStatus;
import com.taskscheduler.repository.TaskRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * The heart of the scheduler.
 *
 * Two thread pools are used, mirroring how real job-scheduling systems
 * (Quartz, Celery, Sidekiq) are designed internally:
 *
 *  - workerPool   : fixed pool that actually executes task "work"
 *  - retryPool    : scheduled pool that fires delayed retries without
 *                    blocking the main polling loop
 *
 * Flow:
 *  1. pollForDueTasks() runs every 5 seconds and looks for PENDING
 *     tasks whose scheduledTime has arrived.
 *  2. Each due task is submitted to workerPool -> executeTask(id).
 *  3. executeTask marks it RUNNING, does the simulated work, and on
 *     success marks COMPLETED (re-queuing itself if recurring).
 *  4. On failure, retryCount is incremented. If retries remain, a
 *     retry is scheduled after an exponential backoff delay
 *     (2^retryCount seconds). Otherwise the task is marked FAILED.
 */
@Service
public class TaskExecutorService {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutorService.class);
    private static final String REPORTS_DIR = "reports";

    private final TaskRepository taskRepository;

    // Pool of worker threads that actually "do the work" of a task
    private final ExecutorService workerPool = Executors.newFixedThreadPool(5);

    // Separate pool purely for scheduling delayed retries
    private final ScheduledExecutorService retryPool = Executors.newScheduledThreadPool(2);

    @Autowired
    public TaskExecutorService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
        try {
            Files.createDirectories(Paths.get(REPORTS_DIR));
        } catch (IOException e) {
            log.warn("Could not create reports directory: {}", e.getMessage());
        }
    }

    /**
     * The poller. Runs every 5 seconds on Spring's scheduling thread and
     * hands off any due work to the worker pool so this loop never blocks.
     */
    @Scheduled(fixedRate = 5000)
    public void pollForDueTasks() {
        List<Task> dueTasks = taskRepository.findByStatusAndScheduledTimeLessThanEqual(
                TaskStatus.PENDING, LocalDateTime.now());

        for (Task task : dueTasks) {
            // Claim it immediately so the next poll (5s later) doesn't pick it up again
            task.setStatus(TaskStatus.RUNNING);
            taskRepository.save(task);
            workerPool.submit(() -> executeTask(task.getId()));
        }
    }

    private void executeTask(Long taskId) {
        Optional<Task> optionalTask = taskRepository.findById(taskId);
        if (optionalTask.isEmpty()) {
            return; // task was deleted before it could run
        }
        Task task = optionalTask.get();
        log.info("Executing task [{}] '{}' (attempt {})", task.getId(), task.getName(), task.getRetryCount() + 1);

        task.setLastRunAt(LocalDateTime.now());

        try {
            String result = doWork(task);
            task.setStatus(TaskStatus.COMPLETED);
            task.setLastResult(result);
            task.setRetryCount(0);

            // Recurring task: schedule the next run instead of leaving it COMPLETED forever
            if (task.isRecurring()) {
                task.setScheduledTime(LocalDateTime.now().plusSeconds(task.getIntervalSeconds()));
                task.setStatus(TaskStatus.PENDING);
            }
            taskRepository.save(task);
            log.info("Task [{}] completed successfully", task.getId());

        } catch (Exception ex) {
            handleFailure(task, ex);
        }
    }

    private void handleFailure(Task task, Exception ex) {
        int attempt = task.getRetryCount() + 1;
        task.setRetryCount(attempt);
        task.setLastResult("Error: " + ex.getMessage());

        if (attempt < task.getMaxRetries()) {
            // Exponential backoff: 2s, 4s, 8s, ...
            long delaySeconds = (long) Math.pow(2, attempt);
            task.setStatus(TaskStatus.PENDING); // will be picked up again, but only after the delay below
            taskRepository.save(task);

            log.warn("Task [{}] failed (attempt {}/{}). Retrying in {}s",
                    task.getId(), attempt, task.getMaxRetries(), delaySeconds);

            retryPool.schedule(() -> {
                // Re-fetch to get the freshest state, then re-run
                executeTask(task.getId());
            }, delaySeconds, TimeUnit.SECONDS);

        } else {
            task.setStatus(TaskStatus.FAILED);
            taskRepository.save(task);
            log.error("Task [{}] permanently FAILED after {} attempts", task.getId(), attempt);
        }
    }

    /**
     * Simulated "work". In a real system this is where you'd call an
     * email service, generate an actual report, hit another microservice, etc.
     */
    private String doWork(Task task) throws Exception {
        switch (task.getType()) {

            case LOG_MESSAGE -> {
                String msg = "[" + LocalDateTime.now() + "] " + task.getName() + " -> " + task.getDescription();
                log.info(msg);
                return "Logged successfully";
            }

            case GENERATE_REPORT -> {
                String fileName = REPORTS_DIR + "/report_" + task.getId() + "_" +
                        System.currentTimeMillis() + ".txt";
                try (FileWriter writer = new FileWriter(fileName)) {
                    writer.write("Report for task: " + task.getName() + "\n");
                    writer.write("Generated at: " +
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
                    writer.write("Description: " + task.getDescription() + "\n");
                }
                return "Report generated: " + fileName;
            }

            case SEND_NOTIFICATION -> {
                // Simulate an unreliable external service (e.g. an email/SMS provider)
                // ~30% of the time so the retry mechanism has something real to demonstrate.
                if (Math.random() < 0.3) {
                    throw new RuntimeException("Notification provider timeout");
                }
                return "Notification sent for: " + task.getName();
            }

            default -> throw new IllegalArgumentException("Unknown task type: " + task.getType());
        }
    }

    @PreDestroy
    public void shutdown() {
        workerPool.shutdown();
        retryPool.shutdown();
    }
}
