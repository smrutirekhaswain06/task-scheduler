package com.taskscheduler.repository;

import com.taskscheduler.model.Task;
import com.taskscheduler.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // Used by the poller: find tasks that are due to run right now
    List<Task> findByStatusAndScheduledTimeLessThanEqual(TaskStatus status, LocalDateTime now);

    // Used by the dashboard filter buttons
    List<Task> findByStatus(TaskStatus status);

    long countByStatus(TaskStatus status);
}
