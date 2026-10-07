package com.interviewprep.task;

import com.interviewprep.common.error.InvalidFieldException;
import com.interviewprep.common.error.ResourceNotFoundException;
import com.interviewprep.task.dto.TaskRequest;
import com.interviewprep.task.dto.TaskResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

  private static final String RESOURCE = "Task";

  private final TaskRepository taskRepository;
  private final Clock clock;

  public TaskService(TaskRepository taskRepository, Clock clock) {
    this.taskRepository = taskRepository;
    this.clock = clock;
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
    rejectNewPastDueDate(task, request.dueDate());
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

  private void rejectNewPastDueDate(Task task, LocalDate dueDate) {
    boolean changed = !Objects.equals(task.getDueDate(), dueDate);
    if (changed && dueDate != null && dueDate.isBefore(LocalDate.now(clock))) {
      throw new InvalidFieldException("dueDate", TaskRequest.PAST_DUE_DATE);
    }
  }

  private Task find(Long id) {
    return taskRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
  }
}
