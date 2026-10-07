package com.edstem.interviewprep.task;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TaskRepository taskRepository;

  @BeforeEach
  void clean() {
    taskRepository.deleteAll();
  }

  @Test
  void taskLifecycleAndStatusFilter() throws Exception {
    long id = create("{\"title\":\"Plan sprint\",\"status\":\"TODO\"}");
    create("{\"title\":\"Ship release\",\"status\":\"DONE\"}");

    mockMvc
        .perform(get("/api/tasks").param("status", "DONE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].title").value("Ship release"));

    mockMvc
        .perform(
            put("/api/tasks/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Plan sprint\",\"status\":\"IN_PROGRESS\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());

    mockMvc.perform(delete("/api/tasks/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/tasks/" + id)).andExpect(status().isNotFound());
    mockMvc.perform(get("/api/tasks")).andExpect(jsonPath("$", hasSize(1)));
  }

  private long create(String body) throws Exception {
    String response =
        mockMvc
            .perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asLong();
  }
}
