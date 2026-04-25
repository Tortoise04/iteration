package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.entity.Achievement;
import com.iterate.digitalwellness.service.AchievementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private static final Logger logger = LoggerFactory.getLogger(AchievementController.class);

    @Autowired
    private AchievementService achievementService;

    @PostMapping
    public ResponseEntity<?> save(@RequestBody Achievement achievement) {
        try {
            logger.info("创建成果记录: {}", achievement.getAchievement());
            Achievement saved = achievementService.save(achievement);
            logger.info("成果创建成功, ID: {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("创建成果记录失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "创建失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @GetMapping
    public ResponseEntity<?> findAll() {
        try {
            logger.info("查询所有成果记录");
            List<Achievement> achievements = achievementService.findAll();
            logger.info("查询到 {} 条成果", achievements.size());
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            logger.error("查询成果记录失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
            Achievement achievement = achievementService.findById(id);
            if (achievement == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "成果不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }
            return ResponseEntity.ok(achievement);
        } catch (Exception e) {
            logger.error("查询成果记录失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Achievement update) {
        try {
            logger.info("更新成果记录 ID: {}", id);
            Achievement existing = achievementService.findById(id);
            if (existing == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "成果不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }
            if (update.getAchievement() != null) existing.setAchievement(update.getAchievement());
            if (update.getDescription() != null) existing.setDescription(update.getDescription());
            if (update.getCategory() != null) existing.setCategory(update.getCategory());
            if (update.getTime() != null) existing.setTime(update.getTime());
            Achievement saved = achievementService.save(existing);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("更新成果记录失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "更新失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        try {
            logger.info("删除成果记录 ID: {}", id);
            achievementService.deleteById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除成果记录失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "删除失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }
}
