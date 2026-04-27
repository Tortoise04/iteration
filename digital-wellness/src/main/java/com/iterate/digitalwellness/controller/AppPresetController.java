package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.entity.AppPreset;
import com.iterate.digitalwellness.service.AppPresetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app-presets")
public class AppPresetController {

    private static final Logger logger = LoggerFactory.getLogger(AppPresetController.class);

    @Autowired
    private AppPresetService appPresetService;

    @GetMapping
    public ResponseEntity<?> findAll() {
        try {
            logger.info("查询所有应用预设");
            List<AppPreset> presets = appPresetService.findActive();
            return ResponseEntity.ok(presets);
        } catch (Exception e) {
            logger.error("查询应用预设失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> findAllIncludingInactive() {
        try {
            logger.info("查询所有应用预设（包括未激活）");
            List<AppPreset> presets = appPresetService.findAll();
            return ResponseEntity.ok(presets);
        } catch (Exception e) {
            logger.error("查询应用预设失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
            logger.info("查询应用预设 ID: {}", id);
            AppPreset preset = appPresetService.findById(id);
            if (preset == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "预设不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            return ResponseEntity.ok(preset);
        } catch (Exception e) {
            logger.error("查询应用预设失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AppPreset appPreset) {
        try {
            logger.info("创建应用预设: {}", appPreset.getAppName());
            
            // 设置默认值
            if (appPreset.getIsActive() == null) {
                appPreset.setIsActive(true);
            }
            if (appPreset.getSortOrder() == null) {
                appPreset.setSortOrder(0);
            }
            appPreset.setCreatedAt(LocalDateTime.now());
            appPreset.setUpdatedAt(LocalDateTime.now());
            
            AppPreset saved = appPresetService.save(appPreset);
            logger.info("应用预设创建成功, ID: {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("创建应用预设失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "创建失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody AppPreset appPreset) {
        try {
            logger.info("更新应用预设 ID: {}", id);
            
            AppPreset existing = appPresetService.findById(id);
            if (existing == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "预设不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 更新字段
            if (appPreset.getAppName() != null) {
                existing.setAppName(appPreset.getAppName());
            }
            if (appPreset.getSortOrder() != null) {
                existing.setSortOrder(appPreset.getSortOrder());
            }
            if (appPreset.getIcon() != null) {
                existing.setIcon(appPreset.getIcon());
            }
            if (appPreset.getIsActive() != null) {
                existing.setIsActive(appPreset.getIsActive());
            }
            existing.setUpdatedAt(LocalDateTime.now());

            AppPreset saved = appPresetService.save(existing);
            logger.info("应用预设更新成功");
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("更新应用预设失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "更新失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            logger.info("删除应用预设 ID: {}", id);
            
            AppPreset existing = appPresetService.findById(id);
            if (existing == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "预设不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 软删除：设为未激活
            existing.setIsActive(false);
            existing.setUpdatedAt(LocalDateTime.now());
            appPresetService.save(existing);
            
            logger.info("应用预设删除成功");
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除应用预设失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "删除失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
