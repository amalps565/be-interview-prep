package com.edstem.interviewprep.task;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.common.error.ResourceNotFoundException;
import com.edstem.interviewprep.task.dto.TaskRequest;
import com.edstem.interviewprep.task.dto.TaskResponse;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TaskService taskService;

  @Test
  void createReturns201WithLocation() throws Exception {
    TaskResponse created =
        new TaskResponse(
            1L, "Write tests", null, TaskStatus.TODO, LocalDate.now().plusDays(1), Instant.now());
    given(taskService.create(any(TaskRequest.class))).willReturn(created);

    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Write tests\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/tasks/1"))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status").value("TODO"));
  }

  @Test
  void invalidInputReturns400WithAMessagePerField() throws Exception {
    String body =
        "{\"title\":\"%s\",\"dueDate\":\"%s\"}"
            .formatted("x".repeat(101), LocalDate.now().minusDays(1));

    mockMvc
        .perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.path").value("/api/tasks"))
        .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
        .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("title", "dueDate")));
  }

  @Test
  void missingTitleReturns400() throws Exception {
    mockMvc
        .perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
        .andExpect(jsonPath("$.fieldErrors[0].message").value("is required"));
  }

  @Test
  void unknownStatusInBodyReturns400ForThatField() throws Exception {
    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"A\",\"status\":\"LATER\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
  }

  @Test
  void unknownStatusFilterReturns400() throws Exception {
    mockMvc
        .perform(get("/api/tasks").param("status", "LATER"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
  }

  @Test
  void unknownTaskReturns404InTheSameFormat() throws Exception {
    given(taskService.get(99L)).willThrow(new ResourceNotFoundException("Task", 99L));

    mockMvc
        .perform(get("/api/tasks/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Task 99 not found"))
        .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
  }

  @Test
  void unexpectedErrorReturns500WithoutDetails() throws Exception {
    given(taskService.get(1L)).willThrow(new IllegalStateException("database exploded"));

    mockMvc
        .perform(get("/api/tasks/1"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
  }
}
