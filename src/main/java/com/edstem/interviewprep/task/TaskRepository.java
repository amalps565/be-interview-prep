package com.edstem.interviewprep.task;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

  List<Task> findByStatusOrderByIdAsc(TaskStatus status);

  List<Task> findAllByOrderByIdAsc();
}
