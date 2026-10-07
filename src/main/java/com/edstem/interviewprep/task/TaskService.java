package com.edstem.interviewprep.task;

import com.edstem.interviewprep.common.error.ResourceNotFoundException;
import com.edstem.interviewprep.task.dto.TaskRequest;
import com.edstem.interviewprep.task.dto.TaskResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

  private static final String RESOURCE = "Task";

  private final TaskRepository taskRepository;

  public TaskService(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Transactional
  public TaskResponse create(TaskRequest request) {
    Task task =
        new Task(
            request.title().strip(),
            request.description(),
            request.statusOrDefault(),
            request.dueDate());
    return TaskResponse.from(taskRepository.save(task));
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> list(TaskStatus status) {
    List<Task> tasks =
        status == null
            ? taskRepository.findAllByOrderByIdAsc()
            : taskRepository.findByStatusOrderByIdAsc(status);
    return tasks.stream().map(TaskResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public TaskResponse get(Long id) {
    return TaskResponse.from(find(id));
  }

  @Transactional
  public TaskResponse update(Long id, TaskRequest request) {
    Task task = find(id);
    task.update(
        request.title().strip(),
        request.description(),
        request.statusOrDefault(),
        request.dueDate());
    return TaskResponse.from(task);
  }

  @Transactional
  public void delete(Long id) {
    taskRepository.delete(find(id));
  }

  private Task find(Long id) {
    return taskRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
  }
}
