package com.interviewprep.task.dto;

import com.interviewprep.task.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequest(
    @NotBlank(message = "is required") @Size(max = 100, message = "must be at most 100 characters")
        String title,
    @Size(max = 2000, message = "must be at most 2000 characters") String description,
    TaskStatus status,
    @FutureOrPresent(groups = OnCreate.class, message = TaskRequest.PAST_DUE_DATE)
        LocalDate dueDate) {

  public static final String PAST_DUE_DATE = "cannot be in the past";

  public TaskStatus statusOrDefault() {
    return status != null ? status : TaskStatus.TODO;
  }
}
