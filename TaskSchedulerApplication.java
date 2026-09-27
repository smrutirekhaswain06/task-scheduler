package com.taskscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the Task Scheduler & Job Monitoring System.
 *
 * @EnableScheduling turns on Spring's @Scheduled annotation support,
 * which powers the background poller that picks up due tasks.
 */
@SpringBootApplication
@EnableScheduling
public class TaskSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskSchedulerApplication.class, args);
        System.out.println("========================================");
        System.out.println(" Task Scheduler is running!");
        System.out.println(" Dashboard: http://localhost:8080");
        System.out.println(" API base:  http://localhost:8080/api/tasks");
        System.out.println("========================================");
    }
}
