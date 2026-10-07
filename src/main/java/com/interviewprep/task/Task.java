package com.interviewprep.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "tasks", indexes = @Index(name = "idx_tasks_status", columnList = "status"))
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TaskStatus status;

  private LocalDate dueDate;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  protected Task() {}

  public Task(String title, String description, TaskStatus status, LocalDate dueDate) {
    update(title, description, status, dueDate);
  }

  public void update(String title, String description, TaskStatus status, LocalDate dueDate) {
    this.title = title;
    this.description = description;
    this.status = status;
    this.dueDate = dueDate;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
