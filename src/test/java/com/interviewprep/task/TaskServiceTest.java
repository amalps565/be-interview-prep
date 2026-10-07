package com.interviewprep.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.interviewprep.common.error.InvalidFieldException;
import com.interviewprep.common.error.ResourceNotFoundException;
import com.interviewprep.task.dto.TaskRequest;
import com.interviewprep.task.dto.TaskResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);
  private static final LocalDate YESTERDAY = TODAY.minusDays(1);

  @Mock private TaskRepository taskRepository;

  private TaskService taskService;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);
    taskService = new TaskService(taskRepository, clock);
  }

  @Test
  void overdueTaskCanBeMarkedDoneWithoutChangingItsDueDate() {
    Task overdue = new Task("Pay invoice", null, TaskStatus.IN_PROGRESS, YESTERDAY);
    given(taskRepository.findById(1L)).willReturn(Optional.of(overdue));

    TaskResponse updated =
        taskService.update(1L, new TaskRequest("Pay invoice", null, TaskStatus.DONE, YESTERDAY));

    assertThat(updated.status()).isEqualTo(TaskStatus.DONE);
    assertThat(updated.dueDate()).isEqualTo(YESTERDAY);
  }

  @Test
  void updateRejectsMovingTheDueDateIntoThePast() {
    Task task = new Task("Pay invoice", null, TaskStatus.TODO, TODAY);
    given(taskRepository.findById(1L)).willReturn(Optional.of(task));
    TaskRequest request = new TaskRequest("Pay invoice", null, TaskStatus.TODO, YESTERDAY);

    InvalidFieldException error =
        assertThrows(InvalidFieldException.class, () -> taskService.update(1L, request));

    assertThat(error.getField()).isEqualTo("dueDate");
    assertThat(task.getDueDate()).isEqualTo(TODAY);
  }

  @Test
  void updateOfUnknownTaskThrowsNotFound() {
    given(taskRepository.findById(9L)).willReturn(Optional.empty());
    TaskRequest request = new TaskRequest("A", null, null, null);

    assertThrows(ResourceNotFoundException.class, () -> taskService.update(9L, request));
  }
}
