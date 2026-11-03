package com.smartschedule.controller;

import com.smartschedule.entity.Goal;
import com.smartschedule.entity.User;
import com.smartschedule.dto.GoalRequest;
import com.smartschedule.exception.BusinessException;
import com.smartschedule.repository.GoalRepository;
import com.smartschedule.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import javax.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalRepository goalRepo;
    private final UserRepository userRepo;

    @GetMapping
    @Transactional(readOnly = true)
    public List<Goal> list(@AuthenticationPrincipal User user) {
        return goalRepo.findByUserIdAndIsActiveTrue(user.getId());
    }

    @PostMapping
    @Transactional
    public Goal create(@AuthenticationPrincipal User user, @Valid @RequestBody GoalRequest req) {
        User managedUser = userRepo.getReferenceById(user.getId());
        Goal goal = Goal.builder()
                .user(managedUser)
                .name(req.getName())
                .description(req.getDescription())
                .category(req.getCategory() != null ? req.getCategory() : "OTHER")
                .energyLevel(req.getEnergyLevel() != null ? req.getEnergyLevel() : "MEDIUM")
                .priority(req.getPriority() != null ? req.getPriority() : 5)
                .defaultDuration(req.getDefaultDuration() != null ? req.getDefaultDuration() : 60)
                .weeklyFrequency(req.getWeeklyFrequency() != null ? req.getWeeklyFrequency() : 3)
                .isActive(true)
                .build();
        return goalRepo.save(goal);
    }

    @PutMapping("/{id}")
    @Transactional
    public Goal update(@AuthenticationPrincipal User user, @PathVariable Long id,
                        @Valid @RequestBody GoalRequest req) {
        Goal goal = goalRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("目标不存在"));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new BusinessException("无权操作");
        }
        if (req.getName() != null) goal.setName(req.getName());
        if (req.getDescription() != null) goal.setDescription(req.getDescription());
        if (req.getCategory() != null) goal.setCategory(req.getCategory());
        if (req.getEnergyLevel() != null) goal.setEnergyLevel(req.getEnergyLevel());
        if (req.getPriority() != null) goal.setPriority(req.getPriority());
        if (req.getDefaultDuration() != null) goal.setDefaultDuration(req.getDefaultDuration());
        return goalRepo.save(goal);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Goal goal = goalRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("目标不存在"));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new BusinessException("无权操作");
        }
        goal.setIsActive(false);
        goalRepo.save(goal);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("message", "已删除");
        return ResponseEntity.ok(result);
    }
}
