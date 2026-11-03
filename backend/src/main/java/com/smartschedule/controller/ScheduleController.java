package com.smartschedule.controller;

import com.smartschedule.dto.TaskStatusRequest;
import com.smartschedule.entity.*;
import com.smartschedule.exception.BusinessException;
import com.smartschedule.repository.DailyPlanRepository;
import com.smartschedule.repository.DailyTaskRepository;
import com.smartschedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final DailyPlanRepository planRepo;
    private final DailyTaskRepository taskRepo;

    @PostMapping("/generate")
    @Transactional
    public ResponseEntity<Map<String, Object>> generate(@AuthenticationPrincipal User user,
                                                         @RequestParam(required = false) String date) {
        LocalDate planDate;
        try {
            planDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        } catch (DateTimeParseException e) {
            throw new BusinessException("日期格式不正确，请使用 yyyy-MM-dd 格式");
        }
        DailyPlan plan = scheduleService.generateDailyPlan(user.getId(), planDate);
        List<DailyTask> tasks = withoutBreaks(taskRepo.findByDailyPlanIdOrderBySortOrderAsc(plan.getId()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plan", plan);
        result.put("tasks", tasks);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/today")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getToday(@AuthenticationPrincipal User user,
                                                         @RequestParam(required = false) String date) {
        LocalDate targetDate;
        try {
            targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        } catch (DateTimeParseException e) {
            throw new BusinessException("日期格式不正确，请使用 yyyy-MM-dd 格式");
        }
        DailyPlan plan = scheduleService.getPlanByDate(user.getId(), targetDate);
        Map<String, Object> result = new LinkedHashMap<>();
        if (plan == null) {
            result.put("plan", null);
            result.put("tasks", Collections.emptyList());
            result.put("date", targetDate.toString());
            result.put("message", "该日期暂无日程，请先生成日程");
        } else {
            List<DailyTask> tasks = withoutBreaks(taskRepo.findByDailyPlanIdOrderBySortOrderAsc(plan.getId()));
            result.put("plan", plan);
            result.put("tasks", tasks);
            result.put("date", targetDate.toString());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 契约 §5：历史遗留的 BREAK 任务不再下发。
     * /history 只返回 DailyPlan 本体（不含任务明细），那里没有可过滤的内容。
     */
    private List<DailyTask> withoutBreaks(List<DailyTask> tasks) {
        List<DailyTask> kept = new ArrayList<>();
        for (DailyTask task : tasks) {
            if (!"BREAK".equals(task.getTaskType())) {
                kept.add(task);
            }
        }
        return kept;
    }

    @PutMapping("/tasks/{taskId}/status")
    @Transactional
    public ResponseEntity<DailyTask> updateTaskStatus(@PathVariable Long taskId,
                                                       @Valid @RequestBody TaskStatusRequest req) {
        DailyTask task = scheduleService.updateTaskStatus(taskId, req.getStatus());
        return ResponseEntity.ok(task);
    }

    @GetMapping("/history")
    @Transactional(readOnly = true)
    public List<DailyPlan> history(@AuthenticationPrincipal User user) {
        return planRepo.findByUserIdOrderByPlanDateDesc(user.getId());
    }
}
