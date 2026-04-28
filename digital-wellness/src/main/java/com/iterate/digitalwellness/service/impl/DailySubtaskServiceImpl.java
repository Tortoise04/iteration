package com.iterate.digitalwellness.service.impl;

import com.iterate.digitalwellness.entity.DailySubtask;
import com.iterate.digitalwellness.entity.Goal;
import com.iterate.digitalwellness.repository.DailySubtaskRepository;
import com.iterate.digitalwellness.service.DailySubtaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DailySubtaskServiceImpl implements DailySubtaskService {

    @Autowired
    private DailySubtaskRepository dailySubtaskRepository;

    @Override
    public DailySubtask save(DailySubtask dailySubtask) {
        if (dailySubtask.getCreatedAt() == null) {
            dailySubtask.setCreatedAt(LocalDateTime.now());
        }
        dailySubtask.setUpdatedAt(LocalDateTime.now());
        return dailySubtaskRepository.save(dailySubtask);
    }

    @Override
    public List<DailySubtask> findAll() {
        return dailySubtaskRepository.findAll();
    }

    @Override
    public DailySubtask findById(Long id) {
        return dailySubtaskRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteById(Long id) {
        dailySubtaskRepository.deleteById(id);
    }

    @Override
    public List<DailySubtask> findByGoalId(Long goalId) {
        return dailySubtaskRepository.findByGoalId(goalId);
    }

    @Override
    public List<Map<String, Object>> findTodayTasksByUserId(Long userId) {
        List<DailySubtask> tasks = dailySubtaskRepository.findByUserIdAndTargetDate(userId, LocalDate.now());
        List<Map<String, Object>> result = new ArrayList<>();
        for (DailySubtask task : tasks) {
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("id", task.getId());
            taskMap.put("taskContent", task.getTaskContent());
            taskMap.put("targetDate", task.getTargetDate());
            taskMap.put("isCompleted", task.getIsCompleted());
            taskMap.put("goalName", task.getGoal().getGoal());
            result.add(taskMap);
        }
        return result;
    }

    @Override
    public DailySubtask completeTask(Long id, String completionNote) {
        DailySubtask task = findById(id);
        if (task != null) {
            task.setIsCompleted(true);
            task.setCompletionDate(LocalDate.now());
            task.setCompletionNote(completionNote);
            return save(task);
        }
        return null;
    }

    @Override
    @Transactional
    public void batchSave(List<DailySubtask> subtasks) {
        for (DailySubtask subtask : subtasks) {
            if (subtask.getCreatedAt() == null) {
                subtask.setCreatedAt(LocalDateTime.now());
            }
            subtask.setUpdatedAt(LocalDateTime.now());
        }
        dailySubtaskRepository.saveAll(subtasks);
    }

    @Override
    public List<DailySubtask> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return dailySubtaskRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }
}
