package com.iterate.digitalwellness.service;

import com.iterate.digitalwellness.entity.DailySubtask;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DailySubtaskService {
    DailySubtask save(DailySubtask dailySubtask);
    List<DailySubtask> findAll();
    DailySubtask findById(Long id);
    void deleteById(Long id);
    List<DailySubtask> findByGoalId(Long goalId);
    List<Map<String, Object>> findTodayTasksByUserId(Long userId);
    DailySubtask completeTask(Long id, String completionNote);
    void batchSave(List<DailySubtask> subtasks);
    List<DailySubtask> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
}
