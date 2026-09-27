package com.taskscheduler.service;

import com.taskscheduler.model.Task;
import com.taskscheduler.model.TaskStatus;
import com.taskscheduler.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task createTask(Task task) {
        // Fresh tasks always start life as PENDING with a clean retry counter
        task.setStatus(TaskStatus.PENDING);
        task.setRetryCount(0);
        return taskRepository.save(task);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    public Map<String, Long> getStats() {
        return Map.of(
                "pending", taskRepository.countByStatus(TaskStatus.PENDING),
                "running", taskRepository.countByStatus(TaskStatus.RUNNING),
                "completed", taskRepository.countByStatus(TaskStatus.COMPLETED),
                "failed", taskRepository.countByStatus(TaskStatus.FAILED),
                "total", taskRepository.count()
        );
    }
}
